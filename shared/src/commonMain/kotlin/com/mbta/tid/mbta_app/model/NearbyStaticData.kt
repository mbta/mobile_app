package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.NearbyStaticData.StaticPatterns
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.utils.resolveParentId
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

/**
 * Aggregates stops and the patterns that serve them by route. The list of routes is ordered with
 * subway routes first, then sorted by distance. Ties are broken by the sort order of the first
 * route pattern.
 */
data class NearbyStaticData(val data: List<TransitWithStops>) {

    sealed class StaticPatterns {
        abstract val patterns: List<RoutePattern>
        abstract val stopIds: Set<String>

        abstract fun copy(
            patterns: List<RoutePattern> = this.patterns,
            stopIds: Set<String> = this.stopIds
        ): StaticPatterns

        data class ByHeadsign(
            val route: Route,
            val headsign: String,
            val line: Line?,
            override val patterns: List<RoutePattern>,
            override val stopIds: Set<String>,
            val direction: Direction? = null,
        ) : StaticPatterns() {
            override fun copy(patterns: List<RoutePattern>, stopIds: Set<String>) =
                copy(route = route, patterns = patterns, stopIds = stopIds)
        }

        data class ByDirection(
            val line: Line,
            val routes: List<Route>,
            val direction: Direction,
            override val patterns: List<RoutePattern>,
            override val stopIds: Set<String>
        ) : StaticPatterns() {
            override fun copy(patterns: List<RoutePattern>, stopIds: Set<String>) =
                copy(line = line, patterns = patterns, stopIds = stopIds)

            val routeIds = routes.map { it.id }
        }
    }

    sealed class StopPatterns {
        abstract val stop: Stop
        abstract val patterns: List<StaticPatterns>
        abstract val directions: List<Direction>

        abstract fun copy(
            stop: Stop = this.stop,
            patterns: List<StaticPatterns> = this.patterns,
            directions: List<Direction> = this.directions
        ): StopPatterns

        data class ForRoute(
            val route: Route,
            override val stop: Stop,
            override val patterns: List<StaticPatterns>,
            override val directions: List<Direction>
        ) : StopPatterns() {
            constructor(
                route: Route,
                stop: Stop,
                patterns: List<StaticPatterns>,
            ) : this(route, stop, patterns, listOf(Direction(0, route), Direction(1, route)))

            override fun copy(
                stop: Stop,
                patterns: List<StaticPatterns>,
                directions: List<Direction>
            ) = copy(route = route, stop = stop, patterns = patterns, directions = directions)
        }

        data class ForLine(
            val line: Line,
            val routes: List<Route>,
            override val stop: Stop,
            override val patterns: List<StaticPatterns>,
            override val directions: List<Direction>
        ) : StopPatterns() {
            constructor(
                line: Line,
                routes: List<Route>,
                stop: Stop,
                patterns: List<StaticPatterns>,
            ) : this(
                line,
                routes,
                stop,
                patterns,
                listOf(groupedDirection(patterns, routes, 0), groupedDirection(patterns, routes, 1))
            )

            override fun copy(
                stop: Stop,
                patterns: List<StaticPatterns>,
                directions: List<Direction>
            ) = this.copy(line = line, stop = stop, patterns = patterns, directions = directions)

            companion object {
                fun groupedDirection(
                    patterns: List<StaticPatterns>,
                    routes: List<Route>,
                    directionId: Int
                ): Direction {
                    val typicalHere =
                        patterns.filter {
                            it.patterns.any { pattern ->
                                pattern.typicality == RoutePattern.Typicality.Typical &&
                                    pattern.directionId == directionId
                            }
                        }
                    return if (typicalHere.isEmpty()) {
                        Direction(directionId, routes.first())
                    } else if (typicalHere.size > 1) {
                        Direction(
                            routes.first().directionNames[directionId] ?: "",
                            null,
                            directionId
                        )
                    } else {
                        when (val it = typicalHere.first()) {
                            is StaticPatterns.ByDirection -> it.direction
                            // When buildStopPatternsForLine creates a StaticPatterns.ByHeadsign,
                            // it should always set it.direction to a non-null value, this fallback
                            // is only here to make the compiler happy and do something sensible if
                            // there are possible edge cases where one is set to null.
                            is StaticPatterns.ByHeadsign -> it.direction
                                    ?: Direction(
                                        it.route.directionNames[directionId] ?: "",
                                        it.headsign,
                                        directionId
                                    )
                        }
                    }
                }
            }
        }
    }

    sealed class TransitWithStops {
        abstract val patternsByStop: List<StopPatterns>

        abstract fun allRoutes(): List<Route>

        abstract fun copy(
            patternsByStop: List<StopPatterns> = this.patternsByStop
        ): TransitWithStops

        fun sortRoute(): Route =
            when (this) {
                is ByRoute -> this.route
                is ByLine -> this.routes.min()
            }

        data class ByLine(
            val line: Line,
            val routes: List<Route>,
            override val patternsByStop: List<StopPatterns>
        ) : TransitWithStops() {
            override fun allRoutes() = routes

            override fun copy(patternsByStop: List<StopPatterns>) =
                this.copy(line = line, routes = routes, patternsByStop = patternsByStop)
        }

        data class ByRoute(val route: Route, override val patternsByStop: List<StopPatterns>) :
            TransitWithStops() {
            override fun allRoutes() = listOf(route)

            override fun copy(patternsByStop: List<StopPatterns>) =
                this.copy(route = route, patternsByStop = patternsByStop)
        }
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
                val stop = stops[stopId] ?: return@forEach
                val newPatternIds =
                    patternIdsByStop
                        .getOrElse(stop.id) { emptyList() }
                        .filter { !routePatternsUsed.contains(it) }
                routePatternsUsed.addAll(newPatternIds)

                val newPatternsByRoute =
                    newPatternIds
                        .mapNotNull { patternId -> routePatterns[patternId] }
                        .groupBy { it.routeId }

                val stopKey =
                    stop.parentStationId?.let { parentStationId ->
                        fullStopIds
                            .getOrPut(parentStationId) { mutableSetOf(parentStationId) }
                            .add(stop.id)
                        // Parents should be disjoint, but if somehow a parent has its own patterns,
                        // find it in the regular stops list
                        stops[parentStationId]
                    }
                        ?: stop

                for ((routeId, routePatterns) in newPatternsByRoute) {
                    val route = routes[routeId] ?: continue
                    val routeStops = patternsByRouteAndStop.getOrPut(route) { mutableMapOf() }
                    val patternsForStop = routeStops.getOrPut(stopKey) { mutableListOf() }
                    patternsForStop += routePatterns
                }
            }

            val byLine =
                patternsByRouteAndStop.entries.groupBy { (route, _) ->
                    route.lineId?.takeUnless { route.isShuttle }
                }

            val touchedLines = mutableSetOf<Line>()

            patternsByRouteAndStop
                .mapNotNull { (route, patternsByStop) ->
                    val line = global.lines[route.lineId]
                    val isGrouped = line?.isGrouped == true && !route.isShuttle
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
                .sortedWith(PatternSorting.compareTransitWithStops())
        }
    )

    companion object {
        fun buildStopPatternsForRoute(
            stop: Stop,
            patterns: List<RoutePattern>,
            route: Route,
            allStopIds: Set<String>,
            global: GlobalResponse
        ): StopPatterns.ForRoute {
            val patternsByHeadsign =
                patterns
                    .groupBy { global.trips[it.representativeTripId]?.headsign }
                    .filterKeys { it != null }

            return StopPatterns.ForRoute(
                route = route,
                stop = stop,
                patterns =
                    patternsByHeadsign
                        .map { (headsign, routePatterns) ->
                            StaticPatterns.ByHeadsign(
                                route,
                                checkNotNull(headsign),
                                null,
                                routePatterns.sorted(),
                                RouteCardData.filterStopsByPatterns(
                                    routePatterns,
                                    global,
                                    allStopIds
                                )
                            )
                        }
                        .sortedWith(PatternSorting.compareStaticPatterns()),
                directions = Direction.getDirections(global, stop, route, patterns)
            )
        }

        private fun buildStopPatternsForLine(
            stop: Stop,
            patterns: Map<Route, List<RoutePattern>>,
            line: Line,
            allStopIds: Set<String>,
            global: GlobalResponse,
        ): StopPatterns.ForLine {
            val routes = patterns.keys.sorted()

            val directionsByPattern =
                patterns
                    .flatMap { (route, patterns) ->
                        patterns.map { pattern ->
                            Pair(
                                pattern.id,
                                Direction.getDirectionForPattern(global, stop, route, pattern)
                            )
                        }
                    }
                    .toMap()
            val patternsByDirection =
                patterns.values.flatten().groupBy { directionsByPattern[it.id] }

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
                            directionPatterns
                                .groupBy { global.trips[it.representativeTripId]?.headsign }
                                .filterKeys { it != null }
                        return@flatMap patternsByHeadsign.map { (headsign, patterns) ->
                            StaticPatterns.ByHeadsign(
                                route,
                                checkNotNull(headsign),
                                line,
                                patterns.sorted(),
                                RouteCardData.filterStopsByPatterns(patterns, global, allStopIds),
                                direction
                            )
                        }
                    }
                    listOf(
                        StaticPatterns.ByDirection(
                            line = line,
                            routes = directionRoutes,
                            direction = direction,
                            patterns = directionPatterns.sorted(),
                            RouteCardData.filterStopsByPatterns(
                                directionPatterns,
                                global,
                                allStopIds
                            )
                        )
                    )
                }

            return StopPatterns.ForLine(
                line = line,
                routes = routes,
                stop = stop,
                patterns =
                    // Remove all directions terminating mid-line at Gov Ctr, this destination
                    // should never be displayed as a grouped direction. This will only be generated
                    // for typical C and B trains when the provided stop is Gov Ctr. At any earlier
                    // stops, they will be overridden to "Gov Ctr & North". At Gov Ctr, they will
                    // have no downstream stops so Direction.getSpecialCaseDestination will find no
                    // override and will return their default destination, "Government Center",
                    // which doesn't make sense as a direction from Gov Ctr. And after Gov Ctr,
                    // B and C trains can only be running there if they're using an atypical route
                    // pattern, which should be overridden or default to a label that matches the
                    // other E or D trains.
                    linePatterns.filter {
                        when (it) {
                            is StaticPatterns.ByDirection ->
                                it.direction.destination != "Government Center"
                            else -> true
                        }
                    },
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
 * Removes non-typical route patterns which are not happening either at all or between
 * [filterAtTime] and [filterAtTime] + [hideNonTypicalPatternsBeyondNext]. Sorts routes by subway
 * first then nearest stop, stops by distance, and headsigns by route pattern sort order.
 *
 * Runs static data and predictions through [TemporaryTerminalFilter].
 */
fun NearbyStaticData.withRealtimeInfoWithoutTripHeadsigns(
    globalData: GlobalResponse?,
    sortByDistanceFrom: Position?,
    schedules: ScheduleResponse?,
    predictions: PredictionsStreamDataResponse?,
    alerts: AlertsStreamDataResponse?,
    filterAtTime: Instant,
    showAllPatternsWhileLoading: Boolean,
    hideNonTypicalPatternsBeyondNext: Duration?,
    filterCancellations: Boolean,
    includeMinorAlerts: Boolean,
    pinnedRoutes: Set<String>
): List<StopsAssociated>? {
    // if predictions or alerts are still loading, this is the loading state
    if (predictions == null || alerts == null) return null

    val activeRelevantAlerts =
        alerts.alerts.values.filter {
            it.isActive(filterAtTime) &&
                it.significance >=
                    if (includeMinorAlerts) AlertSignificance.Minor
                    else AlertSignificance.Accessibility
        }

    val allDataLoaded = schedules != null

    val rewrittenThis =
        if (globalData == null || schedules == null) this
        else
            TemporaryTerminalFilter(this, predictions, globalData, activeRelevantAlerts, schedules)
                .filtered()

    val globalStops = globalData?.stops.orEmpty()

    // add predictions and apply filtering
    val upcomingTripsByRoutePatternAndStop =
        UpcomingTrip.tripsMappedBy(
                globalData?.stops.orEmpty(),
                schedules,
                predictions,
                scheduleKey = { schedule, scheduleData ->
                    val trip = scheduleData.trips[schedule.tripId]
                    RealtimePatterns.UpcomingTripKey.ByRoutePattern(
                        schedule.routeId,
                        trip?.routePatternId,
                        globalStops.resolveParentId(schedule.stopId)
                    )
                },
                predictionKey = { prediction, streamData ->
                    val trip = streamData.trips[prediction.tripId]
                    RealtimePatterns.UpcomingTripKey.ByRoutePattern(
                        prediction.routeId,
                        trip?.routePatternId,
                        globalStops.resolveParentId(prediction.stopId)
                    )
                },
                filterAtTime
            )
            .orEmpty()

    val upcomingTripsByDirectionAndStop =
        UpcomingTrip.tripsMappedBy(
                globalData?.stops.orEmpty(),
                schedules,
                predictions,
                scheduleKey = { schedule, scheduleData ->
                    val trip = scheduleData.trips[schedule.tripId] ?: return@tripsMappedBy null
                    RealtimePatterns.UpcomingTripKey.ByDirection(
                        schedule.routeId,
                        trip.directionId,
                        globalStops.resolveParentId(schedule.stopId)
                    )
                },
                predictionKey = { prediction, streamData ->
                    val trip = streamData.trips[prediction.tripId] ?: return@tripsMappedBy null
                    RealtimePatterns.UpcomingTripKey.ByDirection(
                        prediction.routeId,
                        trip.directionId,
                        globalStops.resolveParentId(prediction.stopId)
                    )
                },
                filterAtTime
            )
            .orEmpty()

    val cutoffTime = hideNonTypicalPatternsBeyondNext?.let { filterAtTime + it }
    val hasSchedulesTodayByPattern = RouteCardData.getSchedulesTodayByPattern(schedules)

    val upcomingTripsMap: UpcomingTripsMap =
        upcomingTripsByRoutePatternAndStop + upcomingTripsByDirectionAndStop

    fun UpcomingTripsMap.maybeFilterCancellations(isSubway: Boolean) =
        if (filterCancellations) this.filterCancellations(isSubway) else this

    fun RealtimePatterns.isLastStopOnRoutePattern(stop: Stop): Boolean {
        return this.patterns
            .filter { it?.typicality == RoutePattern.Typicality.Typical }
            .mapNotNull { it?.representativeTripId }
            .any { representativeTripId ->
                val representativeTrip = globalData?.trips?.get(representativeTripId)
                val lastStopIdInPattern = representativeTrip?.stopIds?.last() ?: return@any false
                lastStopIdInPattern == stop.id || stop.childStopIds.contains(lastStopIdInPattern)
            }
    }

    fun RealtimePatterns.shouldShow(stop: Stop): Boolean {
        if (!allDataLoaded && showAllPatternsWhileLoading) return true
        val isUpcoming =
            when (cutoffTime) {
                null -> this.isUpcoming()
                else -> this.isUpcomingWithin(filterAtTime, cutoffTime)
            }

        val isSubway =
            this.patterns.all { globalData?.routes?.get(it?.routeId)?.type?.isSubway() ?: false }

        val shouldBeFilteredAsArrivalOnly =
            if (isSubway) {
                // On subway, only filter out arrival only patterns at the typical last stop.
                // This way, during a scheduled disruption we still show arrival-only headsign(s) at
                // a temporary terminal to acknowledge the missing typical service.
                this.isLastStopOnRoutePattern(stop) && isArrivalOnly()
            } else {
                if (this.patterns.any { it?.routeId == "Boat-F1" }) {}
                isArrivalOnly()
            }

        return (isTypical() || isUpcoming) && !(shouldBeFilteredAsArrivalOnly)
    }

    fun List<PatternsByStop>.filterEmptyAndSort(): List<PatternsByStop> {
        return this.filterNot { it.patterns.isEmpty() }
            .sortedWith(PatternSorting.comparePatternsByStop(pinnedRoutes, sortByDistanceFrom))
    }

    return rewrittenThis.data
        .asSequence()
        .map { transit ->
            when (transit) {
                is NearbyStaticData.TransitWithStops.ByRoute ->
                    StopsAssociated.WithRoute(
                        transit.route,
                        transit.patternsByStop
                            .map { stopPatterns ->
                                PatternsByStop(
                                    stopPatterns,
                                    upcomingTripsMap.maybeFilterCancellations(
                                        transit.route.type.isSubway()
                                    ),
                                    { it.shouldShow(stopPatterns.stop) },
                                    activeRelevantAlerts.discardTrackChangesAtCRCore(
                                        stopPatterns.stop.isCRCore
                                    ),
                                    globalData?.trips ?: mapOf(),
                                    hasSchedulesTodayByPattern,
                                    allDataLoaded
                                )
                            }
                            .filterEmptyAndSort()
                    )
                is NearbyStaticData.TransitWithStops.ByLine ->
                    StopsAssociated.WithLine(
                        transit.line,
                        transit.routes,
                        transit.patternsByStop
                            .map { stopPatterns ->
                                PatternsByStop(
                                    stopPatterns,
                                    upcomingTripsMap.maybeFilterCancellations(
                                        transit.routes.min().type.isSubway()
                                    ),
                                    { it.shouldShow(stopPatterns.stop) },
                                    activeRelevantAlerts.discardTrackChangesAtCRCore(
                                        stopPatterns.stop.isCRCore
                                    ),
                                    globalData?.trips ?: mapOf(),
                                    hasSchedulesTodayByPattern,
                                    allDataLoaded
                                )
                            }
                            .filterEmptyAndSort()
                    )
            }
        }
        .filterNot { it.isEmpty() }
        .toList()
        .sortedWith(PatternSorting.compareStopsAssociated(pinnedRoutes, sortByDistanceFrom))
}

/**
 * Groups [schedules] and [predictions] into [NearbyHierarchy]. Removes non-typical route patterns
 * which are not happening either at all or between [filterAtTime] and
 * [filterAtTime] + [hideNonTypicalPatternsBeyondNext]. Sorts routes by subway first then nearest
 * stop, stops by distance, and headsigns by route pattern sort order.
 */
fun NearbyStaticData.withRealtimeInfoViaTripHeadsigns(
    globalData: GlobalResponse?,
    sortByDistanceFrom: Position?,
    schedules: ScheduleResponse?,
    predictions: PredictionsStreamDataResponse?,
    alerts: AlertsStreamDataResponse?,
    filterAtTime: Instant,
    showAllPatternsWhileLoading: Boolean,
    hideNonTypicalPatternsBeyondNext: Duration?,
    filterCancellations: Boolean,
    includeMinorAlerts: Boolean,
    pinnedRoutes: Set<String>
): List<StopsAssociated>? {
    // if predictions or alerts are still loading, this is the loading state
    if (predictions == null || alerts == null) return null

    // if global data was still loading, there'd be no nearby data, and null handling is annoying
    if (globalData == null) return null

    val activeRelevantAlerts =
        alerts.alerts.values.filter {
            it.isActive(filterAtTime) &&
                it.significance >=
                    if (includeMinorAlerts) AlertSignificance.Minor
                    else AlertSignificance.Accessibility
        }

    val allDataLoaded = schedules != null

    // add predictions and apply filtering
    val cutoffTime = hideNonTypicalPatternsBeyondNext?.let { filterAtTime + it }
    val hasSchedulesTodayByPattern = RouteCardData.getSchedulesTodayByPattern(schedules)

    fun Map<NearbyHierarchy.DirectionOrHeadsign, NearbyHierarchy.NearbyLeaf>
        .maybeFilterCancellations(isSubway: Boolean) =
        if (filterCancellations) this.mapValues { it.value.filterCancellations(isSubway) } else this

    fun RealtimePatterns.shouldShow(): Boolean {
        if (!allDataLoaded && showAllPatternsWhileLoading) return true
        val isUpcoming =
            when (cutoffTime) {
                null -> this.isUpcoming()
                else -> this.isUpcomingWithin(filterAtTime, cutoffTime)
            }
        return (isTypical() || isUpcoming) && !isArrivalOnly()
    }

    fun List<PatternsByStop>.filterEmptyAndSort(): List<PatternsByStop> {
        return this.filterNot { it.patterns.isEmpty() }
            .sortedWith(PatternSorting.comparePatternsByStop(pinnedRoutes, sortByDistanceFrom))
    }

    val nearbyHierarchy =
        NearbyHierarchy.fromStaticData(this)
            .zip(NearbyHierarchy.fromRealtime(globalData, schedules, predictions, filterAtTime))
            .withLabels(globalData)

    return nearbyHierarchy
        .mapEntries { (lineOrRoute, routeData) ->
            when (lineOrRoute) {
                is NearbyHierarchy.LineOrRoute.Route ->
                    StopsAssociated.WithRoute(
                        lineOrRoute.route,
                        routeData
                            .pickOnlyNearest(sortByDistanceFrom)
                            .map { (stop, routeAtStop) ->
                                val (directions, data) = routeAtStop
                                PatternsByStop(
                                    lineOrRoute,
                                    stop,
                                    directions,
                                    data.maybeFilterCancellations(
                                        lineOrRoute.route.type.isSubway()
                                    ),
                                    { it.shouldShow() },
                                    activeRelevantAlerts.discardTrackChangesAtCRCore(stop.isCRCore),
                                    globalData.trips,
                                    hasSchedulesTodayByPattern,
                                    allDataLoaded
                                )
                            }
                            .filterEmptyAndSort()
                    )
                is NearbyHierarchy.LineOrRoute.Line ->
                    StopsAssociated.WithLine(
                        lineOrRoute.line,
                        lineOrRoute.routes.sorted(),
                        routeData
                            .pickOnlyNearest(sortByDistanceFrom)
                            .map { (stop, routeAtStop) ->
                                val (directions, data) = routeAtStop
                                PatternsByStop(
                                    lineOrRoute,
                                    stop,
                                    directions,
                                    data.maybeFilterCancellations(
                                        lineOrRoute.routes.first().type.isSubway()
                                    ),
                                    { it.shouldShow() },
                                    activeRelevantAlerts.discardTrackChangesAtCRCore(stop.isCRCore),
                                    globalData.trips,
                                    hasSchedulesTodayByPattern,
                                    allDataLoaded
                                )
                            }
                            .filterEmptyAndSort()
                    )
            }
        }
        .filterNot { it.isEmpty() }
        .toList()
        .sortedWith(PatternSorting.compareStopsAssociated(pinnedRoutes, sortByDistanceFrom))
}

suspend fun NearbyStaticData.withRealtimeInfo(
    globalData: GlobalResponse?,
    sortByDistanceFrom: Position,
    schedules: ScheduleResponse?,
    predictions: PredictionsStreamDataResponse?,
    alerts: AlertsStreamDataResponse?,
    filterAtTime: Instant,
    pinnedRoutes: Set<String>,
): List<StopsAssociated>? {
    return withContext(Dispatchers.Default) {
        this@withRealtimeInfo.withRealtimeInfoWithoutTripHeadsigns(
            globalData,
            sortByDistanceFrom,
            schedules,
            predictions,
            alerts,
            filterAtTime,
            showAllPatternsWhileLoading = false,
            hideNonTypicalPatternsBeyondNext = 120.minutes,
            filterCancellations = true,
            includeMinorAlerts = false,
            pinnedRoutes
        )
    }
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

    class PatternsBuilder(val line: Line?, val routes: List<Route>, val allStopIds: Set<String>) {
        val data = mutableListOf<StaticPatterns>()
        val directions = mutableListOf<Direction>()

        @DefaultArgumentInterop.Enabled
        fun headsign(
            route: Route,
            headsign: String,
            patterns: List<RoutePattern>,
            stopIds: Set<String> = allStopIds,
            direction: Direction? = null
        ) {
            data.add(StaticPatterns.ByHeadsign(route, headsign, line, patterns, stopIds, direction))
        }

        @DefaultArgumentInterop.Enabled
        fun headsign(
            headsign: String,
            patterns: List<RoutePattern>,
            stopIds: Set<String> = allStopIds
        ) {
            headsign(routes.min(), headsign, patterns, stopIds)
        }

        @DefaultArgumentInterop.Enabled
        fun direction(
            direction: Direction,
            routes: List<Route>,
            patterns: List<RoutePattern>,
            stopIds: Set<String> = allStopIds
        ) {
            if (line == null) {
                throw RuntimeException("Can't build direction patterns without a line")
            }
            data.add(StaticPatterns.ByDirection(line, routes, direction, patterns, stopIds))
            directions.add(direction)
        }
    }

    class StopPatternsForRouteBuilder(val route: Route) {
        val data = mutableListOf<NearbyStaticData.StopPatterns>()

        @DefaultArgumentInterop.Enabled
        fun stop(
            stop: Stop,
            childStopIds: List<String> = stop.childStopIds,
            directions: List<Direction>? = null,
            block: PatternsBuilder.() -> Unit
        ) {
            val allStopIds = setOf(stop.id).plus(childStopIds)
            val builder = PatternsBuilder(null, listOf(route), allStopIds)
            builder.block()
            data.add(
                if (directions != null) {
                    NearbyStaticData.StopPatterns.ForRoute(route, stop, builder.data, directions)
                } else {
                    NearbyStaticData.StopPatterns.ForRoute(route, stop, builder.data)
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
            val allStopIds = setOf(stop.id).plus(childStopIds)
            val builder = PatternsBuilder(line, routes, allStopIds)
            builder.block()
            data.add(
                NearbyStaticData.StopPatterns.ForLine(
                    line,
                    routes,
                    stop,
                    builder.data,
                    directions ?: builder.directions
                )
            )
        }
    }
}
