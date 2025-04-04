package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.map.style.Color
import com.mbta.tid.mbta_app.model.RealtimePatterns.Companion.formatUpcomingTrip
import com.mbta.tid.mbta_app.model.UpcomingFormat.NoTripsFormat
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import io.github.dellisd.spatialk.geojson.Position
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

// These are used in LineOrRoute to disambiguate them from LineOrRoute.Route and LineOrRoute.Line

private typealias LineModel = Line

private typealias RouteModel = Route

// type aliases can't be nested :(

private typealias ByDirectionBuilder = Map<Int, RouteCardData.LeafBuilder>

private typealias ByStopIdBuilder = Map<String, RouteCardData.RouteStopDataBuilder>

private typealias ByLineOrRouteBuilder = Map<String, RouteCardData.Builder>

/**
 * Basic data that a row of a route card should have. For backwards compatibility with
 * [RealtimePatterns]
 */
interface ILeafData {
    val hasMajorAlerts: Boolean
    val upcomingTrips: List<UpcomingTrip>
    val hasSchedulesToday: Boolean
}

/**
 * Contain all data for presentation in a route card. A route card is a snapshot of service for a
 * route at a set of stops. It has the general structure: Route (or Line) => Stop(s) => Direction =>
 * Upcoming Trips / reason for absence of upcoming trips
 */
data class RouteCardData(
    val lineOrRoute: LineOrRoute,
    val stopData: List<RouteStopData>,
    val context: Context,
    val at: Instant,
) {
    enum class Context {
        NearbyTransit,
        StopDetailsFiltered,
        StopDetailsUnfiltered;

        fun isStopDetails(): Boolean {
            return this == StopDetailsFiltered || this == StopDetailsUnfiltered
        }
    }

    data class RouteStopData(
        val stop: Stop,
        val directions: List<Direction>,
        val data: List<Leaf>
    ) {
        // convenience constructors for when directions are not directly under test
        constructor(
            stop: Stop,
            route: Route,
            data: List<Leaf>,
            globalData: GlobalResponse
        ) : this(stop, LineOrRoute.Route(route), data, globalData)

        constructor(
            stop: Stop,
            line: Line,
            routes: Set<Route>,
            data: List<Leaf>,
            globalData: GlobalResponse
        ) : this(stop, LineOrRoute.Line(line, routes), data, globalData)

        constructor(
            stop: Stop,
            lineOrRoute: LineOrRoute,
            data: List<Leaf>,
            globalData: GlobalResponse
        ) : this(
            stop,
            lineOrRoute.directions(globalData, stop, data.map { it.routePatterns }.flatten()),
            data
        )

        val elevatorAlerts: List<Alert>
            get() =
                data
                    .flatMap { it.alertsHere }
                    .filter { alert -> alert.effect == Alert.Effect.ElevatorClosure }

        val hasElevatorAlerts: Boolean
            get() = elevatorAlerts.isNotEmpty()
    }

    data class Leaf(
        val directionId: Int,
        val routePatterns: List<RoutePattern>,
        val stopIds: Set<String>,
        override val upcomingTrips: List<UpcomingTrip>,
        val alertsHere: List<Alert>,
        val allDataLoaded: Boolean,
        override val hasSchedulesToday: Boolean,
        val alertsDownstream: List<Alert>
    ) : ILeafData {
        override val hasMajorAlerts: Boolean
            get() = run {
                this.alertsHere.any { alert -> alert.significance == AlertSignificance.Major }
            }

        fun format(
            now: Instant,
            representativeRoute: Route,
            globalData: GlobalResponse?,
            context: Context
        ): LeafFormat {
            val potentialHeadsigns = run {
                val potentialHeadsigns = mutableSetOf<String>()
                val cutoffTime = now + 120.minutes
                val tripsUpcoming = upcomingTrips.filter { it.isUpcomingWithin(now, cutoffTime) }
                val isBus = representativeRoute.type == RouteType.BUS
                val tripsToConsider = if (isBus) tripsUpcoming.take(2) else tripsUpcoming
                for (trip in tripsToConsider) {
                    if (trip.isUpcomingWithin(now, cutoffTime)) {
                        potentialHeadsigns.add(trip.headsign)
                    }
                }
                if (!isBus) {
                    for (routePattern in routePatterns) {
                        if (routePattern.isTypical()) {
                            val headsign =
                                globalData?.trips?.get(routePattern.representativeTripId)?.headsign
                                    ?: continue
                            potentialHeadsigns.add(headsign)
                        }
                    }
                }
                potentialHeadsigns
            }
            val isBranching = potentialHeadsigns.size > 1
            val needsRoutesInBranching = routePatterns.distinctBy { it.routeId }.size > 1
            val count =
                when {
                    context == Context.StopDetailsFiltered -> null
                    isBranching -> 3
                    else -> 2
                }

            val translatedContext =
                when (context) {
                    Context.NearbyTransit -> TripInstantDisplay.Context.NearbyTransit
                    Context.StopDetailsFiltered -> TripInstantDisplay.Context.StopDetailsFiltered
                    Context.StopDetailsUnfiltered ->
                        TripInstantDisplay.Context.StopDetailsUnfiltered
                }

            val mapStopRoute = MapStopRoute.matching(representativeRoute)
            val routeType = representativeRoute.type
            val majorAlert = alertsHere.firstOrNull { it.significance >= AlertSignificance.Major }
            if (majorAlert != null)
                return LeafFormat.Single(
                    potentialHeadsigns.singleOrNull(),
                    UpcomingFormat.Disruption(majorAlert, mapStopRoute)
                )
            val secondaryAlertToDisplay =
                alertsHere.firstOrNull { it.significance >= AlertSignificance.Secondary }
                    ?: alertsDownstream.firstOrNull()

            val secondaryAlert =
                secondaryAlertToDisplay?.let {
                    UpcomingFormat.SecondaryAlert(StopAlertState.Issue, mapStopRoute)
                }

            val tripsToShow =
                upcomingTrips
                    .mapNotNull {
                        val format =
                            formatUpcomingTrip(now, it, routeType, translatedContext)
                                ?: return@mapNotNull null
                        Pair(it, format)
                    }
                    .run { if (count != null) take(count) else this }

            return when {
                tripsToShow.isNotEmpty() ->
                    if (isBranching) {
                        LeafFormat.Branched(
                            tripsToShow.map { (trip, format) ->
                                val route =
                                    if (needsRoutesInBranching)
                                        globalData?.getRoute(trip.trip.routeId)
                                    else null
                                LeafFormat.Branched.Branch(
                                    route,
                                    trip.headsign,
                                    UpcomingFormat.Some(format, secondaryAlert)
                                )
                            }
                        )
                    } else {
                        LeafFormat.Single(
                            potentialHeadsigns.singleOrNull(),
                            UpcomingFormat.Some(tripsToShow.map { it.second }, secondaryAlert)
                        )
                    }
                !allDataLoaded ->
                    LeafFormat.Single(potentialHeadsigns.singleOrNull(), UpcomingFormat.Loading)
                else ->
                    LeafFormat.Single(
                        potentialHeadsigns.singleOrNull(),
                        UpcomingFormat.NoTrips(
                            NoTripsFormat.fromUpcomingTrips(upcomingTrips, hasSchedulesToday, now),
                            secondaryAlert
                        )
                    )
            }
        }
    }

    sealed interface LineOrRoute {

        data class Line(val line: LineModel, val routes: Set<RouteModel>) : LineOrRoute

        data class Route(val route: RouteModel) : LineOrRoute

        val id: String
            get() =
                when (this) {
                    is Line -> this.line.id
                    is Route -> this.route.id
                }

        val name: String
            get() =
                when (this) {
                    is Line -> this.line.longName
                    is Route -> this.route.label
                }

        val type: RouteType
            get() =
                when (this) {
                    is Line -> this.sortRoute.type
                    is Route -> this.route.type
                }

        val backgroundColor: Color
            get() =
                when (this) {
                    is Line -> this.line.color
                    is Route -> this.route.color
                }

        val textColor: Color
            get() =
                when (this) {
                    is Line -> this.line.textColor
                    is Route -> this.route.textColor
                }

        val isSubway: Boolean
            get() =
                when (this) {
                    is Line -> this.routes.any { it.type.isSubway() }
                    is Route -> this.route.type.isSubway()
                }

        /** The route whose sortOrder to use when sorting a RouteCardData. */
        val sortRoute: RouteModel
            get() =
                when (this) {
                    is Route -> this.route
                    is Line -> this.routes.min()
                }

        fun directions(
            globalData: GlobalResponse,
            stop: Stop,
            patterns: List<RoutePattern>
        ): List<Direction> =
            when (this) {
                is Line -> Direction.getDirectionsForLine(globalData, stop, patterns)
                is Route -> Direction.getDirections(globalData, stop, this.route, patterns)
            }
    }

    @OptIn(ExperimentalTurfApi::class)
    /** The distance from the given position to the first stop in this route card. */
    fun distanceFrom(position: Position): Double {
        return io.github.dellisd.spatialk.turf.distance(
            position,
            this.stopData.first().stop.position
        )
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
        suspend fun routeCardsForStopList(
            stopIds: List<String>,
            globalData: GlobalResponse?,
            sortByDistanceFrom: Position?,
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse?,
            alerts: AlertsStreamDataResponse?,
            now: Instant,
            pinnedRoutes: Set<String>,
            context: Context
        ): List<RouteCardData>? =
            withContext(Dispatchers.Default) {

                // if predictions or alerts are still loading, this is the loading state
                if (predictions == null || alerts == null) return@withContext null

                // if global data was still loading, there'd be no nearby data, and null handling is
                // annoying
                if (globalData == null) return@withContext null

                val hideNonTypicalPatternsBeyondNext: Duration? =
                    when (context) {
                        Context.NearbyTransit -> 120.minutes
                        Context.StopDetailsUnfiltered -> 120.minutes
                        Context.StopDetailsFiltered -> null
                    }

                val cutoffTime = hideNonTypicalPatternsBeyondNext?.let { now + it }
                val allDataLoaded = schedules != null

                ListBuilder(allDataLoaded, context, now)
                    .addStaticStopsData(stopIds, globalData)
                    .addUpcomingTrips(schedules, predictions, now, globalData)
                    .filterIrrelevantData(now, cutoffTime, context, globalData)
                    .addAlerts(
                        alerts,
                        includeMinorAlerts = context.isStopDetails(),
                        now,
                        globalData
                    )
                    .build()
                    .sort(sortByDistanceFrom, pinnedRoutes)
            }
    }

    data class HierarchyPath(val routeOrLineId: String, val stopId: String, val directionId: Int)

    class ListBuilder(val allDataLoaded: Boolean, val context: Context, val now: Instant) {
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

            // A map of a stop to itself and any children.
            // for standalone stops, an entry will be <standaloneStop, [standaloneStopId]>.
            // for stations, an entry will be <station, [stationId, child1Id, child2Id, etc.]>
            val parentToAllStops: Map<Stop, Set<String>> =
                stopIds
                    .mapNotNull { stopId ->
                        val stop = globalData.stops[stopId]
                        if (stop != null) {
                            Pair(stop.resolveParent(globalData), stop)
                        } else {
                            null
                        }
                    }
                    .groupBy({ it.first }, { it.second.id })
                    .mapValues { it.value.toSet().plus(it.key.id) }

            val patternsGrouped = patternsGroupedByLineOrRouteAndStop(globalData, parentToAllStops)

            val builderData =
                patternsGrouped
                    .map { (lineOrRoute, patternsByStop) ->
                        val key = lineOrRoute.id
                        key to
                            Builder(
                                lineOrRoute,
                                patternsByStop
                                    .map { (stop, patterns) ->
                                        val directions =
                                            lineOrRoute.directions(globalData, stop, patterns)
                                        stop.id to
                                            RouteStopDataBuilder(
                                                stop,
                                                directions = directions,
                                                data =
                                                    patterns
                                                        .groupBy { pattern -> pattern.directionId }
                                                        .mapValues { (directionId, patterns) ->
                                                            LeafBuilder(
                                                                directionId = directionId,
                                                                routePatterns = patterns,
                                                                stopIds =
                                                                    NearbyStaticData
                                                                        .filterStopsByPatterns(
                                                                            patterns,
                                                                            globalData,
                                                                            parentToAllStops
                                                                                .getOrElse(stop) {
                                                                                    setOf(stop.id)
                                                                                }
                                                                        ),
                                                                allDataLoaded = allDataLoaded
                                                            )
                                                        }
                                            )
                                    }
                                    .toMap(),
                                context,
                                now
                            )
                    }
                    .toMap()
            data = builderData
            return this
        }

        private fun patternsByRouteOrLine(
            stopIds: Set<String>,
            globalData: GlobalResponse
        ): Map<LineOrRoute, List<RoutePattern>> {

            val allPatternsAtStopWithRoute: List<Pair<Route, RoutePattern>> =
                stopIds.flatMap { stopId ->
                    val patternsIds = globalData.patternIdsByStop.getOrElse(stopId) { emptyList() }
                    patternsIds.mapNotNull { patternId ->
                        val pattern = globalData.routePatterns[patternId]
                        val route = pattern?.let { globalData.routes[it.routeId] }
                        if (route != null && pattern != null) {
                            Pair(route, pattern)
                        } else {
                            null
                        }
                    }
                }

            val patternsByRouteOrLine =
                allPatternsAtStopWithRoute.groupBy(
                    { (route, _pattern) ->
                        val line = route.lineId?.let { globalData.lines[it] }
                        if (line != null && !route.isShuttle && line.isGrouped) {
                            LineOrRoute.Line(
                                line,
                                routes =
                                    globalData.routesByLineId
                                        .getOrElse(line.id) { emptyList() }
                                        .toSet()
                            )
                        } else LineOrRoute.Route(route)
                    },
                    { it.second }
                )

            return patternsByRouteOrLine
        }

        // build the map of LineOrRoute => Stop => Patterns.
        // A stop is only included for a LineOrRoute if it has any patterns that haven't been seen
        // at an earlier stop for that LineOrRoute.
        private fun patternsGroupedByLineOrRouteAndStop(
            globalData: GlobalResponse,
            parentToAllStops: Map<Stop, Set<String>>
        ): Map<LineOrRoute, Map<Stop, List<RoutePattern>>> {
            val routePatternsUsed = mutableSetOf<String>()

            val patternsGrouped =
                mutableMapOf<LineOrRoute, MutableMap<Stop, MutableList<RoutePattern>>>()

            globalData.run {
                parentToAllStops.forEach { (parentStop, allStopsForParent) ->
                    val patternsByRouteOrLine =
                        patternsByRouteOrLine(allStopsForParent, globalData)
                            // filter out a route if we've already seen all of its patterns
                            .filterNot { (_key, routePatterns) ->
                                routePatternsUsed.containsAll(routePatterns.map { it.id }.toSet())
                            }

                    for ((routeOrLine, routePatterns) in patternsByRouteOrLine) {
                        routePatternsUsed.addAll(routePatterns.map { it.id })
                        val routeStops = patternsGrouped.getOrPut(routeOrLine) { mutableMapOf() }
                        val patternsForStop = routeStops.getOrPut(parentStop) { mutableListOf() }
                        patternsForStop += routePatterns
                    }
                }
            }

            return patternsGrouped
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

            val upcomingTripsBySlot = mutableMapOf<HierarchyPath, MutableList<UpcomingTrip>>()

            for (upcomingTrip in upcomingTrips) {
                val parentStopId =
                    upcomingTrip.stopId?.let { parentStop(globalData, it)?.id } ?: continue
                val lineOrRouteId = lineOrRouteId(globalData, upcomingTrip.trip.routeId) ?: continue
                upcomingTripsBySlot
                    .getOrPut(
                        HierarchyPath(lineOrRouteId, parentStopId, upcomingTrip.trip.directionId),
                        ::mutableListOf
                    )
                    .add(upcomingTrip)
            }

            val hasSchedulesTodayByPattern = NearbyStaticData.getSchedulesTodayByPattern(schedules)

            forEachLeaf { path, leafBuilder ->
                val upcomingTripsHere = upcomingTripsBySlot[path]
                leafBuilder.upcomingTrips = upcomingTripsHere
                leafBuilder.allDataLoaded = schedules != null
                leafBuilder.hasSchedulesToday =
                    if (hasSchedulesTodayByPattern != null)
                        leafBuilder.routePatterns?.any { hasSchedulesTodayByPattern[it.id] == true }
                    else null
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

        private fun forEachLeaf(process: (path: HierarchyPath, leafBuilder: LeafBuilder) -> Unit) {
            for ((routeOrLineId, byStopId) in data) {
                for ((stopId, byDirectionId) in byStopId.stopData) {
                    for ((directionId, leaf) in byDirectionId.data) {
                        process(HierarchyPath(routeOrLineId, stopId, directionId), leaf)
                    }
                }
            }
        }

        fun addAlerts(
            alerts: AlertsStreamDataResponse?,
            includeMinorAlerts: Boolean,
            filterAtTime: Instant,
            globalData: GlobalResponse
        ): ListBuilder {
            val activeRelevantAlerts =
                filterRelevantAlerts(alerts, includeMinorAlerts, filterAtTime)
            forEachLeaf(
                process = { path, leafBuilder ->
                    val routes =
                        if (path.routeOrLineId.startsWith("line-")) {
                            globalData.routesByLineId[path.routeOrLineId].orEmpty().map { it.id }
                        } else {
                            listOf(path.routeOrLineId)
                        }
                    val applicableAlerts =
                        Alert.applicableAlerts(
                            activeRelevantAlerts,
                            path.directionId,
                            routes,
                            leafBuilder.stopIds,
                            null
                        )
                    val downstreamAlerts =
                        PatternsByStop.alertsDownstream(
                            activeRelevantAlerts,
                            leafBuilder.routePatterns.orEmpty(),
                            leafBuilder.stopIds.orEmpty(),
                            globalData.trips
                        )
                    val elevatorAlerts =
                        Alert.elevatorAlerts(activeRelevantAlerts, leafBuilder.stopIds.orEmpty())
                    leafBuilder.alertsHere = applicableAlerts + elevatorAlerts
                    leafBuilder.alertsDownstream = downstreamAlerts
                }
            )
            return this
        }

        private fun filterRelevantAlerts(
            alerts: AlertsStreamDataResponse?,
            includeMinorAlerts: Boolean,
            filterAtTime: Instant
        ): List<Alert> =
            alerts?.alerts?.values?.filter {
                it.isActive(filterAtTime) &&
                    it.significance >=
                        if (includeMinorAlerts) AlertSignificance.Minor
                        else AlertSignificance.Accessibility
            }
                ?: emptyList()

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
                    val isSubway = byStopId.lineOrRoute.isSubway
                    for (directionEntry in byDirectionId.data) {}

                    byDirectionId.data =
                        byDirectionId.data
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
                            .mapValues {
                                val (directionId, leafBuilder) = it
                                leafBuilder
                                    .filterCancellations(isSubway, context)
                                    .filterArrivalOnly()
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
                    routeCardBuilder.value.stopData.values.map { it.build() },
                    context,
                    now
                )
            }
        }
    }

    data class Builder(
        val lineOrRoute: LineOrRoute,
        var stopData: ByStopIdBuilder,
        val context: Context,
        val now: Instant
    ) {

        fun build(): RouteCardData {
            return RouteCardData(this.lineOrRoute, stopData.values.map { it.build() }, context, now)
        }
    }

    data class RouteStopDataBuilder(
        val stop: Stop,
        val directions: List<Direction>,
        var data: ByDirectionBuilder
    ) {
        // convenience constructors for when directions are not directly under test
        constructor(
            stop: Stop,
            route: Route,
            data: ByDirectionBuilder,
            globalData: GlobalResponse
        ) : this(stop, LineOrRoute.Route(route), data, globalData)

        constructor(
            stop: Stop,
            line: Line,
            routes: Set<Route>,
            data: ByDirectionBuilder,
            globalData: GlobalResponse
        ) : this(stop, LineOrRoute.Line(line, routes), data, globalData)

        constructor(
            stop: Stop,
            lineOrRoute: LineOrRoute,
            data: ByDirectionBuilder,
            globalData: GlobalResponse
        ) : this(
            stop,
            lineOrRoute.directions(
                globalData,
                stop,
                data.values.mapNotNull { it.routePatterns }.flatten()
            ),
            data
        )

        fun build(): RouteStopData {
            return RouteStopData(
                stop,
                directions,
                data.values.map { it.build() }.sortedBy { it.directionId }
            )
        }
    }

    data class LeafBuilder(
        val directionId: Int,
        var routePatterns: List<RoutePattern>? = null,
        var stopIds: Set<String>? = null,
        var upcomingTrips: List<UpcomingTrip>? = null,
        var alertsHere: List<Alert>? = null,
        var allDataLoaded: Boolean? = null,
        var hasSchedulesToday: Boolean? = null,
        var alertsDownstream: List<Alert>? = null
    ) {

        fun build(): RouteCardData.Leaf {
            return RouteCardData.Leaf(
                directionId,
                checkNotNull(routePatterns),
                checkNotNull(stopIds),
                this.upcomingTrips ?: emptyList(),
                checkNotNull(alertsHere),
                allDataLoaded ?: false,
                hasSchedulesToday ?: false,
                checkNotNull(alertsDownstream)
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

        /** Filter the list of upcoming trips to remove any that are arrival-only. */
        fun filterArrivalOnly(): LeafBuilder {

            val filteredTrips =
                this.upcomingTrips?.filterNot { trip -> trip.isArrivalOnly() ?: false }
            this.upcomingTrips = filteredTrips
            return this
        }

        /**
         * Whether this leaf should be a shown. A leaf should be shown if any of the following are
         * true:
         * - Any pattern it serves is typical
         * - it has upcoming service that is not arrival-only
         */
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
                    null -> this.upcomingTrips?.any { it.isUpcoming() }
                    else ->
                        this.upcomingTrips?.any { it.isUpcomingWithin(filterAtTime, cutoffTime) }
                }
                    ?: false

            val shouldBeFilteredAsArrivalOnly =
                if (isSubway) {
                    // On subway, only filter out arrival only patterns at the typical last stop.
                    // This way, during a scheduled disruption we still show arrival-only
                    // headsign(s) at
                    // a temporary terminal to acknowledge the missing typical service.
                    this.isTypicalLastStopOnRoutePattern(stop, globalData) &&
                        (this.upcomingTrips?.isArrivalOnly() ?: false)
                } else {
                    this.upcomingTrips?.isArrivalOnly() ?: false
                }

            val isTypical = routePatterns?.any { it.isTypical() } ?: false

            return (isTypical || isUpcoming) && !(shouldBeFilteredAsArrivalOnly)
        }

        private fun isTypicalLastStopOnRoutePattern(
            stop: Stop,
            globalData: GlobalResponse
        ): Boolean {
            return this.routePatterns
                ?.filter {
                    it.typicality == com.mbta.tid.mbta_app.model.RoutePattern.Typicality.Typical
                }
                ?.map { it.representativeTripId }
                ?.all { representativeTripId ->
                    val representativeTrip = globalData.trips[representativeTripId]
                    val lastStopIdInPattern =
                        representativeTrip?.stopIds?.last() ?: return@all false
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
    return this.sortedWith(PatternSorting.compareRouteCards(pinnedRoutes, distanceFrom))
}
