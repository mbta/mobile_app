package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Instant

// type aliases can't be nested :(

private typealias ByDirectionBuilder = Map<Int, RouteCardData.LeafBuilder>

private typealias ByStopIdBuilder = Map<String, RouteCardData.RouteStopDataBuilder>

private typealias ByLineOrRouteBuilder = Map<String, RouteCardData.Builder>

// route/ine id =>  stop id => direction id => upcoming trips
private typealias MutablePartialHierarchy<T> =
    MutableMap<String, MutableMap<String, MutableMap<Int, T>>>

private typealias PartialHierarchy<T> = Map<String, Map<String, Map<Int, T>>>

/**
 * Contain all data for presentation in a route card. A route card is a snapshot of service for a
 * route at a set of stops. It has the general structure: Route (or Line) => Stop(s) => Direction =>
 * Upcoming Trips / reason for absence of upcoming trips
 */
data class RouteCardData(private val lineOrRoute: LineOrRoute, val stopData: List<RouteStopData>) {
    enum class Context {
        NearbyTransit,
        StopDetailsFiltered,
        StopDetailsUnfiltered;

        fun isStopDetails(): Boolean {
            return this == StopDetailsFiltered || this == StopDetailsUnfiltered
        }
    }

    data class RouteStopData(val stop: Stop, val directions: List<Direction>, val data: List<Leaf>)

    data class Leaf(
        val directionId: Int,
        val routePatterns: List<RoutePattern>,
        val stopIds: Set<String>,
        val upcomingTrips: List<UpcomingTrip>,
        val alertsHere: List<Alert>,
        val allDataLoaded: Boolean
    )

    sealed interface LineOrRoute {
        data class Line(
            val line: com.mbta.tid.mbta_app.model.Line,
            val routes: Set<com.mbta.tid.mbta_app.model.Route>
        ) : LineOrRoute

        data class Route(val route: com.mbta.tid.mbta_app.model.Route) : LineOrRoute

        fun isSubway(): Boolean {
            return when (this) {
                is Line -> this.routes.any { it.type.isSubway() }
                is Route -> this.route.type.isSubway()
            }
        }

        fun id(): String {
            return when (this) {
                is Line -> this.line.id
                is Route -> this.route.id
            }
        }
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
            pinnedRoutes: Set<String>,
            context: Context
        ): List<RouteCardData>? {

            // if predictions or alerts are still loading, this is the loading state
            if (predictions == null || alerts == null) return null

            // if global data was still loading, there'd be no nearby data, and null handling is
            // annoying
            if (globalData == null) return null

            val hideNonTypicalPatternsBeyondNext: Duration? =
                when (context) {
                    Context.NearbyTransit -> 120.minutes
                    Context.StopDetailsFiltered -> 120.minutes
                    Context.StopDetailsUnfiltered -> null
                }

            val cutoffTime = hideNonTypicalPatternsBeyondNext?.let { filterAtTime + it }

            return ListBuilder()
                .addStaticStopsData(stopIds, globalData)
                .addUpcomingTrips(schedules, predictions, filterAtTime, globalData)
                .filterIrrelevantData(filterAtTime, cutoffTime, context, globalData)
                .addAlerts(alerts, includeMinorAlerts = context.isStopDetails(), filterAtTime)
                .build()
                .sort(sortByDistanceFrom, pinnedRoutes)
        }
    }

    class ListBuilder() {
        var data: ByLineOrRouteBuilder = mutableMapOf()
            private set

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

            val parentToAllStopIds = mutableMapOf<String, MutableSet<String>>()

            globalData.run {
                stopIds.forEach { stopId ->
                    val stop = stops[stopId] ?: return@forEach
                    val patternsByRouteOrLine =
                        patternsByRouteOrLine(stopId, globalData)
                            // filter out a route if we've already seen all of its patterns
                            .filterNot {
                                routePatternsUsed.containsAll(
                                    it.value.map { pattern -> pattern.id }.toSet()
                                )
                            }
                    routePatternsUsed.addAll(
                        patternsByRouteOrLine.flatMap { it.value }.map { it.id }
                    )

                    stop.parentStationId?.let { parentStationId ->
                        parentToAllStopIds
                            .getOrPut(parentStationId) { mutableSetOf(parentStationId) }
                            .add(stop.id)
                    }

                    val stopOrParent = stop.resolveParent(stops)

                    for ((routeOrLine, routePatterns) in patternsByRouteOrLine) {
                        val routeStops = patternsGrouped.getOrPut(routeOrLine) { mutableMapOf() }
                        val patternsForStop = routeStops.getOrPut(stopOrParent) { mutableListOf() }
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
                        // TODO: Directions list with actual values.
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
                                                        .mapValues {
                                                            LeafBuilder(
                                                                directionId = it.key,
                                                                routePatterns = it.value,
                                                                stopIds =
                                                                    parentToAllStopIds.getOrElse(
                                                                        byStop.key.id
                                                                    ) {
                                                                        setOf(byStop.key.id)
                                                                    }
                                                            )
                                                        }
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
            filterAtTime: Instant,
            globalData: GlobalResponse,
        ): ListBuilder {

            val upcomingTrips =
                UpcomingTrip.tripsFromData(
                    globalData.stops,
                    schedules?.schedules.orEmpty(),
                    predictions.predictions.values.toList(),
                    schedules?.trips.orEmpty() + predictions.trips,
                    predictions.vehicles,
                    filterAtTime
                )

            val partialHierarchy: MutablePartialHierarchy<MutableList<UpcomingTrip>> =
                mutableMapOf()

            for (upcomingTrip in upcomingTrips) {
                val parentStopId =
                    upcomingTrip.stopId?.let { parentStop(globalData, it)?.id } ?: continue
                val lineOrRouteId = lineOrRouteId(globalData, upcomingTrip.trip.routeId) ?: continue
                partialHierarchy
                    .getOrPut(lineOrRouteId, ::mutableMapOf)
                    .getOrPut(parentStopId, ::mutableMapOf)
                    .getOrPut(upcomingTrip.trip.directionId, ::mutableListOf)
                    .add(upcomingTrip)
            }

            this.updateLeaves<List<UpcomingTrip>>(partialHierarchy) { leafBuilder, upcomingTrips ->
                leafBuilder.upcomingTrips = upcomingTrips
                leafBuilder.allDataLoaded = schedules != null
            }
            return this
        }

        private fun parentStop(global: GlobalResponse, stopId: String): Stop? {
            val stop = global.stops[stopId] ?: return null
            return stop.resolveParent(global.stops)
        }

        private fun lineOrRouteId(global: GlobalResponse, routeId: String): String? {
            val route = global.routes[routeId] ?: return null
            val line = route.lineId.let { global.lines[it] }
            return if (line != null && line.isGrouped && !route.isShuttle) {
                line.id
            } else {
                routeId
            }
        }

        /**
         * update the [ByLineOrRouteBuilder] leaves by only adding data to the existing
         * LeafBuilders. with the provided [add] function. If the partialHierarchy contains any
         * routes / stops / directions that are not present in the builder, they *will not* be added
         * to the builder.
         */
        private fun <T> updateLeaves(
            partialHierarchy: PartialHierarchy<T>,
            add: (leafBuilder: LeafBuilder, newData: T) -> Unit
        ) {
            for (entry in partialHierarchy) {
                val (routeOrLineId, byStopId) = entry
                for (stopEntry in byStopId) {
                    val (stopId, byDirectionId) = stopEntry
                    for (directionEntry in byDirectionId) {
                        val (directionId, newLeafData) = directionEntry
                        this.data[routeOrLineId]
                            ?.stopData
                            ?.get(stopId)
                            ?.data
                            ?.get(directionId)
                            ?.let { add(it, newLeafData) }
                    }
                }
            }
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
            filterAtTime: Instant,
            cutoffTime: Instant?,
            context: Context,
            globalData: GlobalResponse
        ): ListBuilder {

            val showAllPatternsWhileLoading = context.isStopDetails()
            for (entry in this.data) {
                val (routeOrLineId, byStopId) = entry
                for (stopEntry in byStopId.stopData) {
                    val (stopId, byDirectionId) = stopEntry
                    val isSubway = byStopId.lineOrRoute.isSubway()
                    for (directionEntry in byDirectionId.data) {}

                    byDirectionId.data =
                        byDirectionId.data
                            .mapValues {
                                val (directionId, leafBuilder) = it
                                leafBuilder.filterCancellations(isSubway, context)
                            }
                            .filter {
                                val (directionId, leafBuilder) = it
                                leafBuilder.shouldShow(
                                    byDirectionId.stop,
                                    filterAtTime,
                                    cutoffTime,
                                    showAllPatternsWhileLoading,
                                    isSubway,
                                    globalData
                                )
                            }
                }
                byStopId.stopData = byStopId.stopData.filterNot { it.value.data.isEmpty() }
            }
            this.data = this.data.filterNot { it.value.stopData.isEmpty() }
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

    data class Builder(val lineOrRoute: LineOrRoute, var stopData: ByStopIdBuilder) {

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
        var data: ByDirectionBuilder
    ) {
        fun build(): RouteCardData.RouteStopData {
            return RouteCardData.RouteStopData(stop, directions, data.values.map { it.build() })
        }
    }

    data class LeafBuilder(
        val directionId: Int,
        var routePatterns: List<RoutePattern>? = null,
        var stopIds: Set<String>? = null,
        var upcomingTrips: List<UpcomingTrip>? = null,
        var alertsHere: List<Alert>? = null,
        var allDataLoaded: Boolean? = null
    ) {

        fun build(): RouteCardData.Leaf {
            // TODO: Once alerts functionality is added, checkNotNull on alerts too
            return RouteCardData.Leaf(
                directionId,
                checkNotNull(routePatterns),
                checkNotNull(stopIds),
                this.upcomingTrips ?: emptyList(),
                alertsHere ?: emptyList(),
                allDataLoaded ?: false
            )
        }

        /**
         * Filter the list of upcoming trips to remove cancelled trips based on the context and
         * whether or not the route is a subway route.
         */
        fun filterCancellations(isSubway: Boolean, context: Context): LeafBuilder {

            val filteredTrips =
                this.upcomingTrips?.filter { trip ->
                    if (
                        context == Context.NearbyTransit ||
                            context == Context.StopDetailsUnfiltered ||
                            isSubway
                    ) {
                        !trip.isCancelled
                    } else {
                        true
                    }
                }
            this.upcomingTrips = filteredTrips
            return this
        }

        fun shouldShow(
            stop: Stop,
            filterAtTime: Instant,
            cutoffTime: Instant?,
            showAllPatternsWhileLoading: Boolean,
            isSubway: Boolean,
            globalData: GlobalResponse
        ): Boolean {
            if (this.allDataLoaded == false && showAllPatternsWhileLoading) return true
            val isUpcoming =
                when (cutoffTime) {
                    null -> this.upcomingTrips?.isUpcoming() ?: false
                    else -> this.upcomingTrips?.isUpcomingWithin(filterAtTime, cutoffTime) ?: false
                }

            val shouldBeFilteredAsArrivalOnly =
                if (isSubway) {
                    // On subway, only filter out arrival only patterns at the typical last stop.
                    // This way, during a scheduled disruption we still show arrival-only
                    // headsign(s) at
                    // a temporary terminal to acknowledge the missing typical service.
                    this.isLastStopOnRoutePattern(stop, globalData) &&
                        (this.upcomingTrips?.isArrivalOnly() ?: false)
                } else {
                    this.upcomingTrips?.isArrivalOnly() ?: false
                }

            val isTypical = routePatterns?.isTypical() ?: false

            return (isTypical || isUpcoming) && !(shouldBeFilteredAsArrivalOnly)
        }

        private fun isLastStopOnRoutePattern(stop: Stop, globalData: GlobalResponse): Boolean {
            return this.routePatterns
                ?.filter {
                    it.typicality == com.mbta.tid.mbta_app.model.RoutePattern.Typicality.Typical
                }
                ?.mapNotNull { it.representativeTripId }
                ?.any { representativeTripId ->
                    val representativeTrip = globalData.trips[representativeTripId]
                    val lastStopIdInPattern =
                        representativeTrip?.stopIds?.last() ?: return@any false
                    lastStopIdInPattern == stop.id ||
                        stop.childStopIds.contains(lastStopIdInPattern)
                }
                ?: false
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
