package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Instant

/**
 * Aggregates stops and the patterns that serve them by route. The list of routes is ordered with
 * subway routes first, then sorted by distance. Ties are broken by the sort order of the first
 * route pattern.
 */
data class NearbyStaticData(val data: List<TransitWithStops>) {

    sealed class StaticPatterns : Comparable<StaticPatterns> {
        abstract val patterns: List<RoutePattern>

        data class ByHeadsign(
            val route: Route,
            val headsign: String,
            val line: Line?,
            override val patterns: List<RoutePattern>
        ) : StaticPatterns()

        data class ByDirection(
            val line: Line,
            val routes: List<Route>,
            val direction: Direction,
            override val patterns: List<RoutePattern>
        ) : StaticPatterns()

        override fun compareTo(other: StaticPatterns): Int =
            compareValuesBy(
                this,
                other,
                { it.patterns.first().directionId },
                {
                    when (it) {
                        is ByDirection -> -1
                        is ByHeadsign -> 1
                    }
                },
                { it.patterns.first() }
            )
    }

    sealed class StopPatterns {
        abstract val stop: Stop
        abstract val allStopIds: Set<String>
        abstract val patterns: List<StaticPatterns>
        abstract val directions: List<Direction>

        data class ForRoute(
            val route: Route,
            override val stop: Stop,
            /** Includes both parent and child stop IDs if present */
            override val allStopIds: Set<String>,
            override val patterns: List<StaticPatterns>,
            override val directions: List<Direction>
        ) : StopPatterns() {
            constructor(
                route: Route,
                stop: Stop,
                allStopIds: Set<String>,
                patterns: List<StaticPatterns>,
            ) : this(
                route,
                stop,
                allStopIds,
                patterns,
                listOf(Direction(0, route), Direction(1, route))
            )
        }

        data class ForLine(
            val line: Line,
            val routes: List<Route>,
            override val stop: Stop,
            /** Includes both parent and child stop IDs if present */
            override val allStopIds: Set<String>,
            override val patterns: List<StaticPatterns>,
            override val directions: List<Direction>
        ) : StopPatterns() {
            constructor(
                line: Line,
                routes: List<Route>,
                stop: Stop,
                patterns: List<StaticPatterns>,
                allStopIds: Set<String>,
            ) : this(
                line,
                routes,
                stop,
                allStopIds,
                patterns,
                listOf(Direction(0, routes.first()), Direction(1, routes.first()))
            )
        }
    }

    sealed class TransitWithStops {
        fun sortRoute(): Route =
            when (this) {
                is ByRoute -> this.route
                is ByLine -> this.routes.min()
            }

        data class ByLine(
            val line: Line,
            val routes: List<Route>,
            val patternsByStop: List<StopPatterns>
        ) : TransitWithStops()

        data class ByRoute(val route: Route, val patternsByStop: List<StopPatterns>) :
            TransitWithStops()
    }

    fun stopIds(): Set<String> =
        data.flatMapTo(mutableSetOf()) { transit ->
            when (transit) {
                is TransitWithStops.ByRoute -> {
                    transit.patternsByStop.map { it.stop.id }
                }
                is TransitWithStops.ByLine -> {
                    transit.patternsByStop.map { it.stop.id }
                }
            }
        }

    constructor(
        global: GlobalResponse,
        nearby: NearbyResponse
    ) : this(
        global.run {
            val routePatternsUsed = mutableSetOf<String>()

            val patternsByRouteAndStop =
                mutableMapOf<Route, MutableMap<Stop, MutableList<RoutePattern>>>()

            val fullStopIds = mutableMapOf<String, MutableSet<String>>()

            nearby.stopIds.forEach { stopId ->
                val stop = stops.getValue(stopId)
                val newPatternIds =
                    patternIdsByStop
                        .getOrElse(stop.id) { emptyList() }
                        .filter { !routePatternsUsed.contains(it) }
                routePatternsUsed.addAll(newPatternIds)

                val newPatternsByRoute =
                    newPatternIds
                        .map { patternId -> routePatterns.getValue(patternId) }
                        .groupBy { it.routeId }

                val stopKey =
                    stop.parentStationId?.let { parentStationId ->
                        fullStopIds
                            .getOrPut(parentStationId) { mutableSetOf(parentStationId) }
                            .add(stop.id)
                        // Parents should be disjoint, but if somehow a parent has its own patterns,
                        // find it in the regular stops list
                        stops.getValue(parentStationId)
                    }
                        ?: stop

                newPatternsByRoute.forEach { (routeId, routePatterns) ->
                    val routeStops =
                        patternsByRouteAndStop.getOrPut(routes.getValue(routeId)) { mutableMapOf() }
                    val patternsForStop = routeStops.getOrPut(stopKey) { mutableListOf() }
                    patternsForStop += routePatterns
                }
            }

            val byLine = patternsByRouteAndStop.entries.groupBy { it.key.lineId }

            val touchedLines = mutableSetOf<Line>()

            patternsByRouteAndStop
                .mapNotNull { (route, patternsByStop) ->
                    val line = global.lines[route.lineId]
                    val isGrouped = groupedLines.contains(route.lineId) && line != null
                    val lineRoutes = byLine[line?.id]?.map { (route, _) -> route } ?: emptyList()

                    if (isGrouped && touchedLines.contains(line)) {
                        null
                    } else if (line != null && isGrouped) {
                        touchedLines.add(line)
                        val sortedRoutes = lineRoutes.sorted()
                        val stopPatternsByRoute =
                            byLine[line.id]?.associate { entry -> Pair(entry.key, entry.value) }
                                ?: emptyMap()
                        buildTransitForLine(
                            line,
                            sortedRoutes,
                            stopPatternsByRoute,
                            fullStopIds,
                            global,
                        )
                    } else {
                        TransitWithStops.ByRoute(
                            route = route,
                            patternsByStop =
                                patternsByStop.map { (stop, patterns) ->
                                    buildStopPatternsForRoute(
                                        stop,
                                        patterns,
                                        route,
                                        fullStopIds.getOrElse(stop.id) { setOf(stop.id) },
                                        global
                                    )
                                }
                        )
                    }
                }
                .sortedWith(compareBy(Route.subwayFirstComparator) { it.sortRoute() })
        }
    )

    companion object {
        val groupedLines = listOf("line-Green")

        fun getSchedulesTodayByPattern(schedules: ScheduleResponse?): Map<String, Boolean>? =
            schedules?.let { scheduleResponse ->
                val scheduledTrips = scheduleResponse.trips
                return@let scheduleResponse.schedules.fold(mutableMapOf<String, Boolean>()) {
                    hasSchedules,
                    schedule ->
                    val trip = scheduledTrips[schedule.tripId]
                    val patternId = trip?.routePatternId ?: return@fold hasSchedules
                    hasSchedules[patternId] = true
                    return@fold hasSchedules
                }
            }

        fun buildStopPatternsForRoute(
            stop: Stop,
            patterns: List<RoutePattern>,
            route: Route,
            allStopIds: Set<String>,
            global: GlobalResponse
        ): StopPatterns.ForRoute {
            val patternsByHeadsign =
                patterns.groupBy { global.trips.getValue(it.representativeTripId).headsign }

            return StopPatterns.ForRoute(
                route = route,
                stop = stop,
                allStopIds = allStopIds,
                patterns =
                    patternsByHeadsign
                        .map { (headsign, routePatterns) ->
                            StaticPatterns.ByHeadsign(route, headsign, null, routePatterns.sorted())
                        }
                        .sorted(),
                directions = Direction.getDirections(global, stop, route, patterns)
            )
        }

        fun buildStopPatternsForLine(
            stop: Stop,
            patterns: Map<Route, List<RoutePattern>>,
            line: Line,
            allStopIds: Set<String>,
            global: GlobalResponse,
        ): StopPatterns.ForLine {
            val routes = patterns.keys.sorted()
            val routeDirections =
                patterns
                    .map { (route, patterns) ->
                        Pair(route.id, Direction.getDirections(global, stop, route, patterns))
                    }
                    .toMap()
            val patternsByDirection =
                patterns.values.flatten().groupBy {
                    routeDirections[it.routeId]?.get(it.directionId)
                }
            val linePatterns =
                patternsByDirection.flatMap { (direction, directionPatterns) ->
                    if (direction == null) {
                        return@flatMap emptyList()
                    }
                    val directionRoutes =
                        directionPatterns.mapNotNull { global.routes[it.routeId] }.toSet().sorted()
                    if (directionRoutes.filter { !it.id.startsWith("Shuttle-") }.size == 1) {
                        val route = directionRoutes.first()
                        val patternsByHeadsign =
                            directionPatterns.groupBy {
                                global.trips.getValue(it.representativeTripId).headsign
                            }
                        return@flatMap patternsByHeadsign.map { (headsign, patterns) ->
                            StaticPatterns.ByHeadsign(route, headsign, line, patterns.sorted())
                        }
                    }
                    listOf(
                        StaticPatterns.ByDirection(
                            line = line,
                            routes = directionRoutes,
                            direction = direction,
                            patterns = directionPatterns.sorted(),
                        )
                    )
                }

            return StopPatterns.ForLine(
                line = line,
                routes = routes,
                stop = stop,
                allStopIds = allStopIds,
                patterns = linePatterns,
            )
        }

        fun buildTransitForLine(
            line: Line,
            routes: List<Route>,
            stopPatternsByRoute: Map<Route, Map<Stop, List<RoutePattern>>>,
            fullStopIds: Map<String, Set<String>>,
            global: GlobalResponse,
        ): TransitWithStops.ByLine {
            // We have a map of maps, but need to flip the stops to be the top level keys
            val patternsByRouteAndStop =
                stopPatternsByRoute.entries.flatMap { routeEntry ->
                    routeEntry.value.entries.map { stopEntry ->
                        Triple(routeEntry.key, stopEntry.key, stopEntry.value)
                    }
                }
            val groupedPatterns = patternsByRouteAndStop.groupBy { it.second }

            return TransitWithStops.ByLine(
                line = line,
                routes = routes,
                patternsByStop =
                    groupedPatterns.map { (stop, patternsByRoute) ->
                        val allStopIds = fullStopIds.getOrElse(stop.id) { setOf(stop.id) }
                        buildStopPatternsForLine(
                            stop,
                            patternsByRoute.associate { Pair(it.first, it.third) },
                            line,
                            allStopIds,
                            global,
                        )
                    },
            )
        }

        fun build(block: NearbyStaticDataBuilder.() -> Unit): NearbyStaticData {
            val builder = NearbyStaticDataBuilder()
            builder.block()
            return NearbyStaticData(builder.data)
        }
    }
}

/**
 * Attaches [schedules] and [predictions] to the route, stop, and routePattern to which they apply.
 * Removes non-typical route patterns which are not predicted within 90 minutes of [filterAtTime].
 * Sorts routes by subway first then nearest stop, stops by distance, and headsigns by route pattern
 * sort order.
 */
fun NearbyStaticData.withRealtimeInfo(
    sortByDistanceFrom: Position,
    schedules: ScheduleResponse?,
    predictions: PredictionsStreamDataResponse?,
    alerts: AlertsStreamDataResponse?,
    filterAtTime: Instant,
    pinnedRoutes: Set<String>
): List<StopsAssociated> {
    // add predictions and apply filtering
    val upcomingTripsByRoutePatternAndStop =
        UpcomingTrip.tripsMappedBy(
                schedules,
                predictions,
                scheduleKey = { schedule, scheduleData ->
                    val trip = scheduleData.trips.getValue(schedule.tripId)
                    RealtimePatterns.UpcomingTripKey.ByRoutePattern(
                        schedule.routeId,
                        trip.routePatternId,
                        schedule.stopId
                    )
                },
                predictionKey = { prediction, streamData ->
                    val trip = streamData.trips.getValue(prediction.tripId)
                    RealtimePatterns.UpcomingTripKey.ByRoutePattern(
                        prediction.routeId,
                        trip.routePatternId,
                        prediction.stopId
                    )
                },
                filterAtTime
            )
            .orEmpty()

    val upcomingTripsByDirectionAndStop =
        UpcomingTrip.tripsMappedBy(
                schedules,
                predictions,
                scheduleKey = { schedule, scheduleData ->
                    val trip = scheduleData.trips.getValue(schedule.tripId)
                    RealtimePatterns.UpcomingTripKey.ByDirection(
                        schedule.routeId,
                        trip.directionId,
                        schedule.stopId
                    )
                },
                predictionKey = { prediction, streamData ->
                    val trip = streamData.trips.getValue(prediction.tripId)
                    RealtimePatterns.UpcomingTripKey.ByDirection(
                        prediction.routeId,
                        trip.directionId,
                        prediction.stopId
                    )
                },
                filterAtTime
            )
            .orEmpty()

    val cutoffTime = filterAtTime.plus(90.minutes)

    val activeRelevantAlerts =
        alerts?.alerts?.values?.filter {
            it.isActive(filterAtTime) && it.significance >= AlertSignificance.Secondary
        }
    val hasSchedulesTodayByPattern = NearbyStaticData.getSchedulesTodayByPattern(schedules)

    return data
        .asSequence()
        .map { transit ->
            when (transit) {
                is NearbyStaticData.TransitWithStops.ByRoute ->
                    StopsAssociated.WithRoute(
                        transit.route,
                        transit.patternsByStop
                            .map {
                                PatternsByStop(
                                    it,
                                    upcomingTripsByRoutePatternAndStop +
                                        upcomingTripsByDirectionAndStop,
                                    cutoffTime,
                                    activeRelevantAlerts,
                                    hasSchedulesTodayByPattern
                                )
                            }
                            .filterNot { it.patterns.isEmpty() }
                            .sortedWith(
                                compareBy(
                                    { it.distanceFrom(sortByDistanceFrom) },
                                    { it.patterns.first() }
                                )
                            )
                    )
                is NearbyStaticData.TransitWithStops.ByLine ->
                    StopsAssociated.WithLine(
                        transit.line,
                        transit.routes,
                        transit.patternsByStop
                            .map {
                                PatternsByStop(
                                    it,
                                    upcomingTripsByRoutePatternAndStop +
                                        upcomingTripsByDirectionAndStop,
                                    cutoffTime,
                                    activeRelevantAlerts,
                                    hasSchedulesTodayByPattern
                                )
                            }
                            .filterNot { it.patterns.isEmpty() }
                            .sortedWith(
                                compareBy(
                                    { it.distanceFrom(sortByDistanceFrom) },
                                    { it.patterns.first() }
                                )
                            )
                    )
            }
        }
        .filterNot { it.isEmpty() }
        .toList()
        .sortedWith(
            compareBy(
                {
                    it.patternsByStop.all { byStop ->
                        byStop.patterns.all { patterns -> !patterns.hasSchedulesToday }
                    }
                },
                { it.distanceFrom(sortByDistanceFrom) },
                { it.sortRoute() },
            )
        )
        .sortedWith(compareBy(Route.relevanceComparator(pinnedRoutes)) { it.sortRoute() })
}

class NearbyStaticDataBuilder {
    val data = mutableListOf<NearbyStaticData.TransitWithStops>()

    fun route(route: Route, block: StopPatternsForRouteBuilder.() -> Unit) {
        val builder = StopPatternsForRouteBuilder(route)
        builder.block()
        data.add(NearbyStaticData.TransitWithStops.ByRoute(route, builder.data))
    }

    fun line(line: Line, routes: List<Route>, block: StopPatternsForLineBuilder.() -> Unit) {
        val builder = StopPatternsForLineBuilder(line)
        builder.block()
        data.add(NearbyStaticData.TransitWithStops.ByLine(line, routes, builder.data))
    }

    class PatternsBuilder(val line: Line?, val routes: List<Route>) {
        val data = mutableListOf<NearbyStaticData.StaticPatterns>()
        val directions = mutableListOf<Direction>()

        fun headsign(route: Route, headsign: String, patterns: List<RoutePattern>) {
            data.add(NearbyStaticData.StaticPatterns.ByHeadsign(route, headsign, line, patterns))
        }

        fun headsign(headsign: String, patterns: List<RoutePattern>) {
            headsign(routes.min(), headsign, patterns)
        }

        fun direction(direction: Direction, routes: List<Route>, patterns: List<RoutePattern>) {
            if (line == null) {
                throw RuntimeException("Can't build direction patterns without a line")
            }
            data.add(NearbyStaticData.StaticPatterns.ByDirection(line, routes, direction, patterns))
            directions.add(direction)
        }
    }

    class StopPatternsForRouteBuilder(val route: Route) {
        val data = mutableListOf<NearbyStaticData.StopPatterns>()

        @DefaultArgumentInterop.Enabled
        fun stop(
            stop: Stop,
            childStopIds: List<String> = emptyList(),
            directions: List<Direction>? = null,
            block: PatternsBuilder.() -> Unit
        ) {
            val builder = PatternsBuilder(null, listOf(route))
            builder.block()
            data.add(
                if (directions != null) {
                    NearbyStaticData.StopPatterns.ForRoute(
                        route,
                        stop,
                        setOf(stop.id).plus(childStopIds),
                        builder.data,
                        directions
                    )
                } else {
                    NearbyStaticData.StopPatterns.ForRoute(
                        route,
                        stop,
                        setOf(stop.id).plus(childStopIds),
                        builder.data
                    )
                }
            )
        }
    }

    class StopPatternsForLineBuilder(val line: Line) {
        val data = mutableListOf<NearbyStaticData.StopPatterns>()

        @DefaultArgumentInterop.Enabled
        fun stop(
            stop: Stop,
            routes: List<Route>,
            childStopIds: List<String> = emptyList(),
            directions: List<Direction>? = null,
            block: PatternsBuilder.() -> Unit
        ) {
            val builder = PatternsBuilder(line, routes)
            builder.block()
            data.add(
                NearbyStaticData.StopPatterns.ForLine(
                    line,
                    routes,
                    stop,
                    setOf(stop.id).plus(childStopIds),
                    builder.data,
                    directions ?: builder.directions
                )
            )
        }
    }
}
