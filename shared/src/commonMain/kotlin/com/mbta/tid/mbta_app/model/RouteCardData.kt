package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration
import kotlinx.datetime.Instant

// type aliases can't be nested :(

typealias ByDirection = Map<Int, RouteCardData.Leaf>

private typealias ByDirectionBuilder = Map<Int, RouteCardData.LeafBuilder>

private typealias ByStopIdBuilder = Map<String, RouteCardData.RouteStopDataBuilder>

private typealias ByLineOrRouteBuilder = Map<String, RouteCardData.Builder>

/**
 * Contain all data for presentation in a route card. A route card is a snapshot of service for a
 * route at a set of stops. It has the general structure: Route (or Line) => Stop(s) => Direction =>
 * Upcoming Trips / reason for absence of upcoming trips
 */
data class RouteCardData(private val lineOrRoute: LineOrRoute, val stopData: List<RouteStopData>) {
    enum class Context {
        NearbyTransit,
        StopDetails
    }

    data class RouteStopData(
        val stop: Stop,
        val directions: List<Direction>,
        val data: ByDirection
    )

    data class Leaf(val upcomingTrips: List<UpcomingTrip>?, val alertsHere: List<Alert>?)

    sealed interface LineOrRoute {
        data class Line(
            val line: com.mbta.tid.mbta_app.model.Line,
            val routes: Set<com.mbta.tid.mbta_app.model.Route>
        ) : LineOrRoute

        data class Route(val route: com.mbta.tid.mbta_app.model.Route) : LineOrRoute
    }

    companion object {
        /**
         * Build a sorted list of route cards containing realtime data for the given stops.
         *
         * Routes are sorted in the following order
         * 1. pinned routes
         * 2. subway routes
         * 3. routes by distance
         * 4. route pattern sort order
         *
         * Any non-typical route patterns which are not happening either at all or between
         * [filterAtTime] and [filterAtTime] + [hideNonTypicalPatternsBeyondNext] are omitted.
         * Cancelled trips are also omitted when [context] = NearbyTransit.
         */
        fun routeCardsForStopList(
            stopIds: List<String>,
            globalData: GlobalResponse?,
            sortByDistanceFrom: Position?,
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse?,
            alerts: AlertsStreamDataResponse?,
            filterAtTime: Instant,
            hideNonTypicalPatternsBeyondNext: Duration?,
            pinnedRoutes: Set<String>,
            context: Context
        ): List<RouteCardData>? {

            // if predictions or alerts are still loading, this is the loading state
            if (predictions == null || alerts == null) return null

            // if global data was still loading, there'd be no nearby data, and null handling is
            // annoying
            if (globalData == null) return null

            val cutoffTime = hideNonTypicalPatternsBeyondNext?.let { filterAtTime + it }

            return ListBuilder()
                .addStaticStopsData(stopIds, globalData)
                .addUpcomingTrips(schedules, predictions, filterAtTime)
                .filterIrrelevantData(
                    cutoffTime,
                    showAllPatternsWhileLoading = context == Context.StopDetails,
                    filterCancellations = context == Context.NearbyTransit
                )
                .addAlerts(
                    alerts,
                    includeMinorAlerts = context == Context.StopDetails,
                    filterAtTime
                )
                .build()
                .sort(sortByDistanceFrom, pinnedRoutes)
        }
    }

    class ListBuilder() {
        var data: ByLineOrRouteBuilder = emptyMap()

        /**
         * Construct a map of the route/line-ids served by the given stops. Uses the order of the
         * stops in the given list to determine the stop ids that will be included for each route.
         *
         * A stop is only included at a route if it serves any route pattern that is not served by
         * an earlier stop in the list.
         */
        fun addStaticStopsData(stopIds: List<String>, globalData: GlobalResponse): ListBuilder {

            val routePatternsUsed = mutableSetOf<String>()

            val patternsGrouped =
                mutableMapOf<LineOrRoute, MutableMap<Stop, MutableList<RoutePattern>>>()

            val fullStopIds = mutableMapOf<String, MutableSet<String>>()

            globalData.run {
                stopIds.forEach { stopId ->
                    val stop = stops[stopId] ?: return@forEach
                    val patternsByRouteOrLine =
                        patternsByRouteOrLine(stopId, globalData)
                            // filter out a route if we've already seen all of its patterns
                            .filter {
                                routePatternsUsed.containsAll(
                                    it.value.map { pattern -> pattern.id }.toSet()
                                )
                            }
                    routePatternsUsed.addAll(
                        patternsByRouteOrLine.flatMap { it.value }.map { it.id }
                    )

                    val stopKey =
                        stop.parentStationId?.let { parentStationId ->
                            fullStopIds
                                .getOrPut(parentStationId) { mutableSetOf(parentStationId) }
                                .add(stop.id)
                            // Parents should be disjoint, but if somehow a parent has its own
                            // patterns,
                            // find it in the regular stops list
                            stops[parentStationId]
                        }
                            ?: stop

                    for ((routeOrLine, routePatterns) in patternsByRouteOrLine) {
                        val routeStops = patternsGrouped.getOrPut(routeOrLine) { mutableMapOf() }
                        val patternsForStop = routeStops.getOrPut(stopKey) { mutableListOf() }
                        patternsForStop += routePatterns
                    }
                }
            }

            val builderData =
                patternsGrouped
                    .map { byLineOrRoute ->
                        val key =
                            when (val cardType = byLineOrRoute.key) {
                                is LineOrRoute.Line -> cardType.line.id
                                is LineOrRoute.Route -> cardType.route.id
                            }
                        // TODO: Directions list
                        key to
                            RouteCardData.Builder(
                                byLineOrRoute.key,
                                byLineOrRoute.value
                                    .map { byStop ->
                                        byStop.key.id to
                                            RouteStopDataBuilder(
                                                byStop.key,
                                                directions = emptyList(),
                                                data =
                                                    byStop.value
                                                        .groupBy { pattern -> pattern.directionId }
                                                        .mapValues { LeafBuilder() }
                                            )
                                    }
                                    .toMap()
                            )
                    }
                    .toMap()
            data = builderData
            return this
        }

        private fun patternsByRouteOrLine(
            stopId: String,
            globalData: GlobalResponse
        ): Map<LineOrRoute, List<RoutePattern>> {
            return globalData.run {
                patternIdsByStop
                    .getOrElse(stopId) { emptyList() }
                    .mapNotNull { patternId ->
                        val pattern = routePatterns[patternId]
                        val route = pattern?.let { routes[it.routeId] }
                        Pair(route, pattern)
                    }
                    .groupBy { (route, _pattern) ->
                        if (route != null) {
                            val line = route.lineId?.let { lines[it] }
                            if (line != null && !route.isShuttle && line.isGrouped) {
                                // set routes empty for now, will populate once all routes
                                // of the line at this stop are known in the next step
                                LineOrRoute.Line(line, routes = emptySet())
                            } else LineOrRoute.Route(route)
                        } else null
                    }
                    .mapNotNull { entry ->
                        when (val key = entry.key) {
                            is LineOrRoute.Line ->
                                LineOrRoute.Line(
                                    key.line,
                                    routes = entry.value.mapNotNull { it.first }.toSet()
                                ) to entry.value.mapNotNull { it.second }
                            is LineOrRoute.Route -> key to entry.value.mapNotNull { it.second }
                            null -> null
                        }
                    }
                    .toMap()
            }
        }

        fun addUpcomingTrips(
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse,
            filterAtTime: Instant
        ): ListBuilder {
            // TODO build upcoming trips
            // transform into map route => stop => direction => upcoming trips
            // merge into data
            return this
        }

        fun addAlerts(
            alerts: AlertsStreamDataResponse?,
            includeMinorAlerts: Boolean,
            filterAtTime: Instant
        ): ListBuilder {
            // TODO
            // transform into map route => stop => direction => alerts
            // break out helper steps for different types of alerts as needed
            // merge into data
            return this
        }

        fun filterIrrelevantData(
            cutoffTime: Instant?,
            showAllPatternsWhileLoading: Boolean,
            filterCancellations: Boolean
        ): ListBuilder {
            // TODO
            return this
        }

        fun build(): List<RouteCardData> {
            return data.map { routeCardBuilder ->
                RouteCardData(
                    routeCardBuilder.value.lineOrRoute,
                    routeCardBuilder.value.stopData.values.map { it.build() }
                )
            }
        }
    }

    data class Builder(val lineOrRoute: LineOrRoute, val stopData: ByStopIdBuilder) {

        fun build(): RouteCardData {
            return RouteCardData(this.lineOrRoute, stopData.values.map { it.build() })
        }

        companion object {
            /**
             * Construct a map of the route/line-ids served by the given stops. Uses the order of
             * the stops in the given list to determine the stop ids that will be included for each
             * route.
             *
             * A stop is only included at a route if it serves any route pattern that is not served
             * by an earlier stop in the list.
             */
            fun fromListOfStops(
                global: GlobalResponse,
                stopIds: List<String>
            ): Map<String, Builder> {
                // TODO - basically what NearbyStaticData constructor does but into the new data
                // types
                return emptyMap()
            }
        }
    }

    data class RouteStopDataBuilder(
        val stop: Stop,
        val directions: List<Direction>,
        val data: ByDirectionBuilder
    ) {
        fun build(): RouteCardData.RouteStopData {
            return RouteCardData.RouteStopData(
                stop,
                directions,
                data.mapValues { it.value.build() }
            )
        }
    }

    data class LeafBuilder(
        var upcomingTrips: List<UpcomingTrip>? = null,
        var alertsHere: List<Alert>? = null
    ) {

        fun build(): RouteCardData.Leaf {
            // TODO: Once alerts functionality is added, checkNotNull on alerts too
            return RouteCardData.Leaf(checkNotNull(this.upcomingTrips), alertsHere ?: emptyList())
        }
    }
}

fun List<RouteCardData>.sort(
    distanceFrom: Position?,
    pinnedRoutes: Set<String>
): List<RouteCardData> {
    // TODO
    return this
}
