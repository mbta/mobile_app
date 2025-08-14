package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.map.style.Color
import com.mbta.tid.mbta_app.model.UpcomingFormat.NoTripsFormat
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import io.github.dellisd.spatialk.geojson.Position
import kotlin.jvm.JvmName
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// These are used in LineOrRoute to disambiguate them from LineOrRoute.Route and LineOrRoute.Line

private typealias LineModel = Line

private typealias RouteModel = Route

// type aliases can't be nested :(

private typealias ByDirectionBuilder = Map<Int, RouteCardData.LeafBuilder>

private typealias ByStopIdBuilder = Map<String, RouteCardData.RouteStopDataBuilder>

private typealias ByLineOrRouteBuilder = Map<String, RouteCardData.Builder>

/**
 * Contain all data for presentation in a route card. A route card is a snapshot of service for a
 * route at a set of stops. It has the general structure: Route (or Line) => Stop(s) => Direction =>
 * Upcoming Trips / reason for absence of upcoming trips
 */
public data class RouteCardData(
    val lineOrRoute: LineOrRoute,
    val stopData: List<RouteStopData>,
    val at: EasternTimeInstant,
) {
    public val id: String = lineOrRoute.id

    public enum class Context {
        NearbyTransit,
        StopDetailsFiltered,
        StopDetailsUnfiltered,
        Favorites;

        internal fun isStopDetails(): Boolean {
            return this == StopDetailsFiltered || this == StopDetailsUnfiltered
        }

        internal fun toTripInstantDisplayContext(): TripInstantDisplay.Context {
            return when (this) {
                NearbyTransit,
                Favorites -> TripInstantDisplay.Context.NearbyTransit
                StopDetailsFiltered -> TripInstantDisplay.Context.StopDetailsFiltered
                StopDetailsUnfiltered -> TripInstantDisplay.Context.StopDetailsUnfiltered
            }
        }
    }

    public data class RouteStopData(
        val lineOrRoute: LineOrRoute,
        val stop: Stop,
        val directions: List<Direction>,
        val data: List<Leaf>,
    ) {
        // convenience constructors for when directions are not directly under test
        public constructor(
            route: Route,
            stop: Stop,
            data: List<Leaf>,
            globalData: GlobalResponse,
        ) : this(LineOrRoute.Route(route), stop, data, globalData)

        internal constructor(
            line: Line,
            routes: Set<Route>,
            stop: Stop,
            data: List<Leaf>,
            globalData: GlobalResponse,
        ) : this(LineOrRoute.Line(line, routes), stop, data, globalData)

        public constructor(
            lineOrRoute: LineOrRoute,
            stop: Stop,
            data: List<Leaf>,
            globalData: GlobalResponse,
        ) : this(
            lineOrRoute,
            stop,
            lineOrRoute.directions(globalData, stop, data.map { it.routePatterns }.flatten()),
            data,
        )

        internal val id = stop.id

        /** The directions for the lineOrRoute that are actually served by this stop */
        val availableDirections: Set<Int> = data.map { it.directionId }.toSet()

        val elevatorAlerts: List<Alert>
            get() =
                data
                    .flatMap { it.alertsHere() }
                    .filter { alert -> alert.effect == Alert.Effect.ElevatorClosure }
                    .distinct()

        val hasElevatorAlerts: Boolean
            get() = elevatorAlerts.isNotEmpty()
    }

    public data class Leaf
    internal constructor(
        val lineOrRoute: LineOrRoute,
        val stop: Stop,
        val directionId: Int,
        val routePatterns: List<RoutePattern>,
        internal val stopIds: Set<String>,
        val upcomingTrips: List<UpcomingTrip>,
        private val alertsHere: List<Alert>,
        internal val allDataLoaded: Boolean,
        internal val hasSchedulesTodayByPattern: Map<String, Boolean>,
        private val alertsDownstream: List<Alert>,
        internal val context: Context,
    ) {

        /** Convenience constructor for testing to avoid having to set hasSchedulesTodayByPattern */
        public constructor(
            lineOrRoute: LineOrRoute,
            stop: Stop,
            directionId: Int,
            routePatterns: List<RoutePattern>,
            stopIds: Set<String>,
            upcomingTrips: List<UpcomingTrip>,
            alertsHere: List<Alert>,
            allDataLoaded: Boolean,
            hasSchedulesToday: Boolean,
            alertsDownstream: List<Alert>,
            context: Context,
        ) : this(
            lineOrRoute,
            stop,
            directionId,
            routePatterns,
            stopIds,
            upcomingTrips,
            alertsHere,
            allDataLoaded,
            // adding this fakeId will make hasSchedulesToday JustWork for non-branched routes
            // when routePatterns are not specified in the test
            if (routePatterns.isEmpty()) mapOf("fakeId" to hasSchedulesToday)
            else routePatterns.associate { it.id to hasSchedulesToday },
            alertsDownstream,
            context,
        )

        internal val id: Int = directionId

        internal val hasSchedulesToday: Boolean = hasSchedulesTodayByPattern.any { it.value }

        internal val hasMajorAlerts: Boolean
            get() = run {
                this.alertsHere.any { alert -> alert.significance == AlertSignificance.Major }
            }

        private val majorAlert =
            alertsHere.firstOrNull { it.significance >= AlertSignificance.Major }

        private val secondaryAlertToDisplay =
            alertsHere.firstOrNull {
                it.significance < AlertSignificance.Major &&
                    it.significance >= AlertSignificance.Secondary
            } ?: alertsDownstream.firstOrNull()

        public fun alertsHere(tripId: String? = null): List<Alert> =
            alertsHere.filter { alert ->
                (tripId == null || alert.anyInformedEntitySatisfies { checkTrip(tripId) })
            }

        public fun alertsDownstream(tripId: String? = null): List<Alert> =
            alertsDownstream.filter { alert ->
                (tripId == null || alert.anyInformedEntitySatisfies { checkTrip(tripId) })
            }

        private data class PotentialService(
            val routeId: String,
            val headsign: String,
            val routePatternIds: Set<String>,
        )

        /**
         * Get all routes and headsigns that might be shown for this leaf. For bus, the only
         * headsigns that could be shown would be of the next two upcoming trips. For all other
         * modes, headsigns for all of the upcoming trips **and** any other typical headsigns that
         * are not reflected in the upcoming trips (may have already ended for the day, be
         * disrupted, etc. but should still be considered) could be shown
         */
        private fun potentialService(
            now: EasternTimeInstant,
            representativeRoute: Route,
            globalData: GlobalResponse?,
            context: Context,
        ): Set<PotentialService> {
            val potentialService: MutableMap<Pair<String, String>, MutableSet<String>> =
                mutableMapOf()
            val cutoffTime = now + 120.minutes
            val tripsUpcoming = upcomingTrips.filter { it.isUpcomingWithin(now, cutoffTime) }
            val isBus = representativeRoute.type == RouteType.BUS
            val tripsToConsider =
                if (isBus && context != Context.StopDetailsFiltered)
                    tripsUpcoming.take(TYPICAL_LEAF_ROWS)
                else tripsUpcoming

            for (trip in tripsToConsider) {
                if (trip.isUpcomingWithin(now, cutoffTime)) {
                    val existingPatterns =
                        potentialService.getOrPut(Pair(trip.trip.routeId, trip.headsign)) {
                            mutableSetOf()
                        }
                    if (trip.trip.routePatternId != null) {
                        existingPatterns.add(trip.trip.routePatternId)
                    }
                }
            }
            if (!isBus) {
                for (routePattern in routePatterns) {
                    if (
                        routePattern.isTypical() &&
                            // If this pattern is already represented under a different headsign,
                            // then we don't need to represent it separately.
                            !potentialService.values
                                .flatMapTo(mutableSetOf(), { it })
                                .contains(routePattern.id)
                    ) {
                        val headsign =
                            globalData?.trips?.get(routePattern.representativeTripId)?.headsign
                                ?: continue
                        potentialService
                            .getOrPut(Pair(routePattern.routeId, headsign)) { mutableSetOf() }
                            .add(routePattern.id)
                    }
                }
            }
            return potentialService
                .map { (key, patternIds) -> PotentialService(key.first, key.second, patternIds) }
                .toSet()
        }

        /**
         * Convenience struct to group together all the data under a single headsign that is
         * necessary to determine what should be displayed for that headsign on a branched route
         *
         * @param stopIds the child stop ids of this Leaf that are served by the [routePatterns]
         * @param routePatterns all patterns that have the matching headsign
         * @param hasSchedulesToday whether there are schedules today for the headsign. Used to
         *   determine the appropriate [UpcomingFormat.NoTripsFormat] when needed
         * @param allUpcomingTrips all the upcoming trips under the headsign. Used to determine the
         *   appropriate [UpcomingFormat.NoTripsFormat] when needed
         * @param majorAlert the major alert affecting this headsign, if it exists
         */
        private data class ByHeadsignData(
            val stopIds: Set<String>,
            val routePatterns: List<RoutePattern>,
            val hasSchedulesToday: Boolean,
            val allUpcomingTrips: List<UpcomingTrip>,
            val majorAlert: Alert?,
        )

        /**
         * Group the data from this leaf by headsign using the given list of potential headsigns and
         * the pre-determined list of tripsWithFormat that should be shown for this leaf.
         */
        private fun dataByHeadsign(
            potentialService: Set<PotentialService>,
            globalData: GlobalResponse?,
        ): Map<String, ByHeadsignData> {
            return potentialService
                .map { (_, headsign, patternIds) ->
                    val routePatterns =
                        routePatterns.filter { pattern -> patternIds.contains(pattern.id) }

                    val routePatternIds = routePatterns.map { it.id }.toSet()

                    val stopIds =
                        globalData
                            ?.let { filterStopsByPatterns(routePatterns, it, this.stopIds) }
                            .orEmpty()
                    val majorAlert =
                        Alert.applicableAlerts(
                            alertsHere.filter { it.significance >= AlertSignificance.Major },
                            directionId,
                            routePatterns.map { it.routeId },
                            stopIds,
                            null,
                        )

                    headsign to
                        ByHeadsignData(
                            stopIds,
                            routePatterns,
                            hasSchedulesTodayByPattern
                                .filterKeys { it in routePatternIds }
                                .any { it.value },
                            upcomingTrips.filter { it.headsign == headsign },
                            majorAlert.firstOrNull(),
                        )
                }
                .toMap()
        }

        /**
         * For a Leaf that is already determined to represent branched service, produce the
         * appropriate [LeafFormat]. If there is a major alert affecting all branches, this will
         * return a [LeafFormat.Single]. Otherwise, this will return a [LeafFormat.Branched].
         *
         * The [LeafFormat.Branched] can include up to 4 branches. If there is a disruption on 3 of
         * the branches, there will be 3 disruption branches and one branch to display an upcoming
         * trip or [NoTripsFormat.PredictionsUnavailable].
         *
         * In all other cases, there will be up to 3 branches, prioritizing showing disruption
         * branches, then upcoming trips, then [NoTripsFormat.PredictionsUnavailable]
         */
        private fun formatForBranchedService(
            potentialService: Set<PotentialService>,
            tripsWithFormat: List<Pair<UpcomingTrip, UpcomingFormat.Some.FormattedTrip>>,
            mapStopRoute: MapStopRoute?,
            secondaryAlert: UpcomingFormat.SecondaryAlert?,
            globalData: GlobalResponse?,
            now: EasternTimeInstant,
        ): LeafFormat {

            // If we are dealing with a line, then we should show the route alongside the
            // UpcomingTripFormat
            val shouldIncludeRoute = this.lineOrRoute is LineOrRoute.Line

            val dataByHeadsign = dataByHeadsign(potentialService, globalData)
            val (nonDisruptedHeadsigns, disruptedHeadsigns) =
                dataByHeadsign.entries.partition { it.value.majorAlert == null }

            if (disruptedHeadsigns.isEmpty()) {
                return LeafFormat.Branched(
                    tripsWithFormat.map { (trip, format) ->
                        val route =
                            if (shouldIncludeRoute) globalData?.getRoute(trip.trip.routeId)
                            else null
                        LeafFormat.Branched.BranchRow(
                            route,
                            trip.headsign,
                            UpcomingFormat.Some(format, null),
                        )
                    },
                    secondaryAlert,
                )
            }

            if (
                nonDisruptedHeadsigns.isEmpty() &&
                    disruptedHeadsigns
                        .map { it.value.majorAlert }
                        .all { it == disruptedHeadsigns.first().value.majorAlert }
            ) {
                return LeafFormat.Single(
                    route = null,
                    null,
                    UpcomingFormat.Disruption(
                        disruptedHeadsigns.first().value.majorAlert!!,
                        mapStopRoute,
                    ),
                )
            }

            val disruptedHeadsignBranches =
                disruptedHeadsigns
                    .sortedBy { it.value.routePatterns.minOf { pattern -> pattern.sortOrder } }
                    .take(BRANCHING_LEAF_ROWS)
                    .map { (headsign, groupedData) ->
                        val route =
                            if (shouldIncludeRoute)
                                globalData?.getRoute(
                                    groupedData.routePatterns.firstOrNull()?.routeId
                                )
                            else null
                        LeafFormat.Branched.BranchRow(
                            route,
                            headsign,
                            UpcomingFormat.Disruption(groupedData.majorAlert!!, mapStopRoute),
                        )
                    }

            var remainingRowsToShow = max(1, BRANCHING_LEAF_ROWS - disruptedHeadsignBranches.size)

            val upcomingTripBranches =
                tripsWithFormat.take(remainingRowsToShow).map { (upcomingTrip, formatted) ->
                    val route =
                        if (shouldIncludeRoute) globalData?.getRoute(upcomingTrip.trip.routeId)
                        else null
                    LeafFormat.Branched.BranchRow(
                        route,
                        upcomingTrip.trip.headsign,
                        UpcomingFormat.Some(formatted, null),
                    )
                }

            remainingRowsToShow = max(0, remainingRowsToShow - upcomingTripBranches.size)

            val predictionsUnavailableBranches =
                if (remainingRowsToShow > 0) {
                    nonDisruptedHeadsigns
                        .sortedBy { it.value.routePatterns.minOf { pattern -> pattern.sortOrder } }
                        .mapNotNull { (headsign, groupedData) ->
                            val route =
                                if (shouldIncludeRoute)
                                    globalData?.getRoute(
                                        groupedData.routePatterns.firstOrNull()?.routeId
                                    )
                                else null
                            val noTripsFormat =
                                NoTripsFormat.fromUpcomingTrips(
                                    groupedData.allUpcomingTrips,
                                    groupedData.hasSchedulesToday,
                                    now,
                                )

                            if (noTripsFormat == NoTripsFormat.PredictionsUnavailable) {
                                LeafFormat.Branched.BranchRow(
                                    route,
                                    headsign,
                                    UpcomingFormat.NoTrips(noTripsFormat, null),
                                )
                            } else {
                                null
                            }
                        }
                        .take(remainingRowsToShow)
                } else {
                    emptyList()
                }

            return LeafFormat.Branched(
                upcomingTripBranches + predictionsUnavailableBranches + disruptedHeadsignBranches,
                secondaryAlert,
            )
        }

        /**
         * For a Leaf that is already determined should be formatted as a single headsign, produce
         * the appropriate [LeafFormat.Single].
         */
        private fun formatForSingleHeadsignService(
            route: Route?,
            headsign: String?,
            formattedTrips: List<UpcomingFormat.Some.FormattedTrip>,
            mapStopRoute: MapStopRoute?,
            secondaryAlert: UpcomingFormat.SecondaryAlert?,
        ): LeafFormat.Single {
            val format =
                if (majorAlert != null) {
                    UpcomingFormat.Disruption(majorAlert, mapStopRoute)
                } else {
                    UpcomingFormat.Some(formattedTrips, secondaryAlert)
                }
            return LeafFormat.Single(route, headsign, format)
        }

        public fun format(now: EasternTimeInstant, globalData: GlobalResponse?): LeafFormat {
            val representativeRoute = this.lineOrRoute.sortRoute
            val potentialService = potentialService(now, representativeRoute, globalData, context)

            // If we are dealing with a line, then we should show the route alongside the
            // UpcomingTripFormat
            val shouldIncludeRoute = this.lineOrRoute is LineOrRoute.Line

            val isBranching = potentialService.size > 1

            val routeType = representativeRoute.type
            val translatedContext = context.toTripInstantDisplayContext()
            val countTripsToDisplay =
                when {
                    context == Context.StopDetailsFiltered -> null
                    isBranching && routeType != RouteType.BUS -> BRANCHING_LEAF_ROWS
                    else -> TYPICAL_LEAF_ROWS
                }

            val tripsToShow =
                upcomingTrips.withFormat(now, routeType, translatedContext, countTripsToDisplay)

            val mapStopRoute = MapStopRoute.matching(representativeRoute)

            val secondaryAlert =
                secondaryAlertToDisplay?.let {
                    UpcomingFormat.SecondaryAlert(StopAlertState.Issue, mapStopRoute)
                }

            if (majorAlert == null && tripsToShow.isEmpty()) {
                // base case is the same for branched & non-branched routes:
                // if there is no alert & no trips to show, use the single format
                val service = if (isBranching) null else potentialService.firstOrNull()
                val route = globalData?.getRoute(service?.routeId)?.takeIf { shouldIncludeRoute }
                return when {
                    !allDataLoaded ->
                        LeafFormat.Single(route, service?.headsign, UpcomingFormat.Loading)
                    else ->
                        LeafFormat.Single(
                            route,
                            service?.headsign,
                            UpcomingFormat.NoTrips(
                                NoTripsFormat.fromUpcomingTrips(
                                    upcomingTrips,
                                    hasSchedulesToday,
                                    now,
                                ),
                                secondaryAlert,
                            ),
                        )
                }
            }

            return if (isBranching) {
                formatForBranchedService(
                    potentialService,
                    tripsToShow,
                    mapStopRoute,
                    secondaryAlert,
                    globalData,
                    now,
                )
            } else {
                formatForSingleHeadsignService(
                    globalData?.getRoute(potentialService.singleOrNull()?.routeId)?.takeIf {
                        shouldIncludeRoute
                    },
                    potentialService.singleOrNull()?.headsign,
                    tripsToShow.map { it.second },
                    mapStopRoute,
                    secondaryAlert,
                )
            }
        }
    }

    public sealed class LineOrRoute {

        public data class Line(val line: LineModel, val routes: Set<RouteModel>) : LineOrRoute()

        public data class Route(val route: RouteModel) : LineOrRoute()

        public val id: String
            get() =
                when (this) {
                    is Line -> this.line.id
                    is Route -> this.route.id
                }

        public val name: String
            get() =
                when (this) {
                    is Line -> this.line.longName
                    is Route -> this.route.label
                }

        public val type: RouteType
            get() =
                when (this) {
                    is Line -> this.sortRoute.type
                    is Route -> this.route.type
                }

        public val backgroundColor: Color
            get() =
                when (this) {
                    is Line -> this.line.color
                    is Route -> this.route.color
                }

        public val textColor: Color
            get() =
                when (this) {
                    is Line -> this.line.textColor
                    is Route -> this.route.textColor
                }

        internal val isSubway: Boolean
            get() =
                when (this) {
                    is Line -> this.routes.any { it.type.isSubway() }
                    is Route -> this.route.type.isSubway()
                }

        /** The route whose sortOrder to use when sorting a RouteCardData. */
        public val sortRoute: RouteModel
            get() =
                when (this) {
                    is Route -> this.route
                    is Line -> this.routes.min()
                }

        public val allRoutes: Set<RouteModel>
            get() =
                when (this) {
                    is Route -> setOf(this.route)
                    is Line -> this.routes
                }

        public fun directions(
            globalData: GlobalResponse,
            stop: Stop,
            patterns: List<RoutePattern>,
        ): List<Direction> =
            when (this) {
                is Line -> Direction.getDirectionsForLine(globalData, stop, patterns)
                is Route -> Direction.getDirections(globalData, stop, this.route, patterns)
            }

        public fun containsRoute(routeId: String?): Boolean =
            when (this) {
                is Line -> this.routes.any { it.id == routeId }
                is Route -> this.id == routeId
            }
    }

    /** The distance from the given position to the first stop in this route card. */
    internal fun distanceFrom(position: Position): Double =
        this.stopData.first().stop.distanceFrom(position)

    override fun toString(): String = "[RouteCardData]"

    public companion object {
        // For regular non-branching service, we always show up to 2 departure rows for each leaf
        internal const val TYPICAL_LEAF_ROWS = 2
        // For branching non-bus service we show up to 3 departure or disruption rows for each leaf
        internal const val BRANCHING_LEAF_ROWS = 3

        /**
         * Build a sorted list of route cards containing realtime data for the given stops.
         *
         * Routes are sorted in the following order
         * 1. subway routes
         * 2. routes by distance
         * 3. route pattern sort order
         *
         * Any non-typical route patterns which are not happening either at all or between
         * [filterAtTime] and [filterAtTime] + [hideNonTypicalPatternsBeyondNext] are omitted.
         * Cancelled trips are also omitted when [context] = NearbyTransit.
         */
        @DefaultArgumentInterop.Enabled
        public suspend fun routeCardsForStopList(
            stopIds: List<String>,
            globalData: GlobalResponse?,
            sortByDistanceFrom: Position?,
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse?,
            alerts: AlertsStreamDataResponse?,
            now: EasternTimeInstant,
            context: Context,
            favorites: Set<RouteStopDirection>? = null,
            coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
        ): List<RouteCardData>? =
            withContext(coroutineDispatcher) {

                // if predictions or alerts are still loading, this is the loading state
                if (predictions == null || alerts == null) return@withContext null

                // if global data was still loading, there'd be no nearby data, and null handling is
                // annoying
                if (globalData == null) return@withContext null

                val hideNonTypicalPatternsBeyondNext: Duration? =
                    when (context) {
                        Context.NearbyTransit -> 120.minutes
                        Context.StopDetailsUnfiltered -> 120.minutes
                        Context.StopDetailsFiltered,
                        Context.Favorites -> null
                    }

                val cutoffTime = hideNonTypicalPatternsBeyondNext?.let { now + it }
                val allDataLoaded = schedules != null

                ListBuilder(allDataLoaded, context, now)
                    .addStaticStopsData(stopIds, globalData, context, favorites)
                    .addUpcomingTrips(schedules, predictions, now, globalData)
                    .filterIrrelevantData(now, cutoffTime, context, globalData)
                    .addAlerts(
                        alerts,
                        includeMinorAlerts = context.isStopDetails(),
                        now,
                        globalData,
                    )
                    .build(sortByDistanceFrom)
                    .sort(sortByDistanceFrom, context)
            }

        /**
         * Build a static sorted list of route cards for the given stops.
         *
         * Routes are sorted in the following order
         * 1. subway routes
         * 2. routes by distance
         * 3. route pattern sort order
         */
        @DefaultArgumentInterop.Enabled
        internal suspend fun routeCardsForStaticStopList(
            stopIds: List<String>,
            globalData: GlobalResponse?,
            context: Context,
            now: EasternTimeInstant = EasternTimeInstant.now(),
            sortByDistanceFrom: Position? = null,
            favorites: Set<RouteStopDirection>? = null,
            coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
        ): List<RouteCardData>? =
            withContext(coroutineDispatcher) {
                // if global data was still loading, there'd be no nearby data, and null handling is
                // annoying
                if (globalData == null) return@withContext null

                ListBuilder(true, context, now)
                    .addStaticStopsData(stopIds, globalData, context, favorites)
                    // We don't need alerts here, this is just to satisfy the null check
                    .addAlerts(
                        alerts = AlertsStreamDataResponse(emptyMap()),
                        includeMinorAlerts = false,
                        filterAtTime = now,
                        globalData = globalData,
                    )
                    .build(sortByDistanceFrom)
                    .sort(sortByDistanceFrom, context)
            }

        internal fun filterStopsByPatterns(
            routePatterns: List<RoutePattern>,
            global: GlobalResponse,
            localStops: Set<String>,
        ): Set<String> {
            val patternsStops =
                routePatterns.flatMapTo(mutableSetOf()) {
                    global.trips[it.representativeTripId]?.stopIds.orEmpty()
                }
            val relevantStops = patternsStops.intersect(localStops)
            return relevantStops.ifEmpty { localStops }
        }
    }

    internal data class HierarchyPath(
        val routeOrLineId: String,
        val stopId: String,
        val directionId: Int,
    )

    internal class ListBuilder(
        val allDataLoaded: Boolean,
        val context: Context,
        val now: EasternTimeInstant,
    ) {
        var data: ByLineOrRouteBuilder = mutableMapOf()
            private set

        /**
         * Construct a map of the route/line-ids served by the given stops. Uses the order of the
         * stops in the given list to determine the stop ids that will be included for each route.
         *
         * A stop is only included at a route if it serves any route pattern that is not served by
         * an earlier stop in the list.
         */
        fun addStaticStopsData(
            stopIds: List<String>,
            globalData: GlobalResponse,
            context: Context,
            favorites: Set<RouteStopDirection>?,
        ): ListBuilder {

            val parentToAllStops = Stop.resolvedParentToAllStops(stopIds, globalData)

            val patternsGrouped =
                RoutePattern.patternsGroupedByLineOrRouteAndStop(
                    parentToAllStops,
                    globalData,
                    context,
                    favorites,
                )

            val builderData =
                patternsGrouped
                    .map { (lineOrRoute, patternsByStop) ->
                        val key = lineOrRoute.id
                        key to
                            Builder(
                                lineOrRoute,
                                patternsByStop
                                    .map { (stop, patternsForStop) ->
                                        val directions =
                                            lineOrRoute.directions(
                                                globalData,
                                                stop,
                                                patternsForStop.allPatterns.filter {
                                                    it.isTypical()
                                                },
                                            )
                                        stop.id to
                                            RouteStopDataBuilder(
                                                lineOrRoute,
                                                stop,
                                                directions = directions,
                                                data =
                                                    patternsForStop.allPatterns
                                                        .groupBy { pattern -> pattern.directionId }
                                                        .mapValues { (directionId, patterns) ->
                                                            LeafBuilder(
                                                                lineOrRoute = lineOrRoute,
                                                                stop = stop,
                                                                directionId = directionId,
                                                                routePatterns = patterns,
                                                                patternsNotSeenAtEarlierStops =
                                                                    patterns
                                                                        .map { it.id }
                                                                        .toSet()
                                                                        .intersect(
                                                                            patternsForStop
                                                                                .patternsNotSeenAtEarlierStops
                                                                        ),
                                                                stopIds =
                                                                    filterStopsByPatterns(
                                                                        patterns,
                                                                        globalData,
                                                                        parentToAllStops.getOrElse(
                                                                            stop
                                                                        ) {
                                                                            setOf(stop.id)
                                                                        },
                                                                    ),
                                                                allDataLoaded = allDataLoaded,
                                                                context = context,
                                                            )
                                                        },
                                            )
                                    }
                                    .toMap(),
                                now,
                            )
                    }
                    .toMap()
            data = builderData
            return this
        }

        fun addUpcomingTrips(
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse,
            filterAtTime: EasternTimeInstant,
            globalData: GlobalResponse,
        ): ListBuilder {

            val upcomingTrips =
                UpcomingTrip.tripsFromData(
                    globalData.stops,
                    schedules?.schedules.orEmpty(),
                    predictions.predictions.values.toList(),
                    schedules?.trips.orEmpty() + predictions.trips,
                    predictions.vehicles,
                    filterAtTime,
                )

            val upcomingTripsBySlot = mutableMapOf<HierarchyPath, MutableList<UpcomingTrip>>()

            for (upcomingTrip in upcomingTrips) {
                val parentStopId =
                    upcomingTrip.stopId?.let { parentStop(globalData, it)?.id } ?: continue
                val lineOrRouteId = lineOrRouteId(globalData, upcomingTrip.trip.routeId) ?: continue
                upcomingTripsBySlot
                    .getOrPut(
                        HierarchyPath(lineOrRouteId, parentStopId, upcomingTrip.trip.directionId),
                        ::mutableListOf,
                    )
                    .add(upcomingTrip)
            }

            val hasSchedulesTodayByPattern = schedules?.getSchedulesTodayByPattern()

            forEachLeaf { path, leafBuilder ->
                val upcomingTripsHere = upcomingTripsBySlot[path]
                val patternIds = (leafBuilder.routePatterns ?: emptyList()).map { it.id }.toSet()
                leafBuilder.upcomingTrips = upcomingTripsHere
                leafBuilder.allDataLoaded = schedules != null
                leafBuilder.hasSchedulesTodayByPattern =
                    hasSchedulesTodayByPattern?.let {
                        patternIds.associateWith { patternId -> it.getOrElse(patternId) { false } }
                    }
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
            filterAtTime: EasternTimeInstant,
            globalData: GlobalResponse,
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
                    val isCRCore = globalData.getStop(path.stopId)?.isCRCore ?: false
                    val applicableAlerts =
                        Alert.applicableAlerts(
                                activeRelevantAlerts,
                                path.directionId,
                                routes,
                                leafBuilder.stopIds,
                                null,
                            )
                            .discardTrackChangesAtCRCore(isCRCore)
                    val downstreamAlerts =
                        Alert.alertsDownstreamForPatterns(
                            activeRelevantAlerts,
                            leafBuilder.routePatterns.orEmpty(),
                            leafBuilder.stopIds.orEmpty(),
                            globalData.trips,
                        )
                    val elevatorAlerts =
                        Alert.elevatorAlerts(activeRelevantAlerts, leafBuilder.stopIds.orEmpty())
                    leafBuilder.alertsHere = (applicableAlerts + elevatorAlerts).distinct()
                    leafBuilder.alertsDownstream = downstreamAlerts
                }
            )
            return this
        }

        private fun filterRelevantAlerts(
            alerts: AlertsStreamDataResponse?,
            includeMinorAlerts: Boolean,
            filterAtTime: EasternTimeInstant,
        ): List<Alert> =
            alerts?.alerts?.values?.filter {
                it.isActive(filterAtTime) &&
                    it.significance >=
                        if (includeMinorAlerts) AlertSignificance.Minor
                        else AlertSignificance.Accessibility
            } ?: emptyList()

        fun filterIrrelevantData(
            filterAtTime: EasternTimeInstant,
            cutoffTime: EasternTimeInstant?,
            context: Context,
            globalData: GlobalResponse,
        ): ListBuilder {

            val showAllPatternsWhileLoading = context.isStopDetails()
            for (entry in this.data) {
                val (routeOrLineId, byStopId) = entry
                for (stopEntry in byStopId.stopData) {
                    val (stopId, byDirectionId) = stopEntry
                    val lineOrRoute = byStopId.lineOrRoute

                    byDirectionId.data =
                        byDirectionId.data
                            .filter {
                                val (directionId, leafBuilder) = it
                                leafBuilder.shouldShow(
                                    byDirectionId.stop,
                                    filterAtTime,
                                    cutoffTime,
                                    showAllPatternsWhileLoading,
                                    lineOrRoute,
                                    globalData,
                                )
                            }
                            .mapValues {
                                val (directionId, leafBuilder) = it
                                leafBuilder
                                    .filterCancellations(lineOrRoute.isSubway, context)
                                    .filterArrivalOnly()
                            }
                }
                byStopId.stopData = byStopId.stopData.filterNot { it.value.data.isEmpty() }
            }
            this.data = this.data.filterNot { it.value.stopData.isEmpty() }
            return this
        }

        fun build(sortByDistanceFrom: Position?): List<RouteCardData> {
            return data.map { routeCardBuilder ->
                RouteCardData(
                    routeCardBuilder.value.lineOrRoute,
                    routeCardBuilder.value.stopData.values
                        .map { it.build() }
                        .sort(sortByDistanceFrom),
                    now,
                )
            }
        }
    }

    internal data class Builder(
        val lineOrRoute: LineOrRoute,
        var stopData: ByStopIdBuilder,
        val now: EasternTimeInstant,
    ) {

        fun build(sortByDistanceFrom: Position?): RouteCardData {
            return RouteCardData(
                this.lineOrRoute,
                stopData.values.map { it.build() }.sort(sortByDistanceFrom),
                now,
            )
        }
    }

    internal data class RouteStopDataBuilder(
        val lineOrRoute: LineOrRoute,
        val stop: Stop,
        val directions: List<Direction>,
        var data: ByDirectionBuilder,
    ) {
        // convenience constructors for when directions are not directly under test
        constructor(
            route: Route,
            stop: Stop,
            data: ByDirectionBuilder,
            globalData: GlobalResponse,
        ) : this(stop, LineOrRoute.Route(route), data, globalData)

        constructor(
            line: Line,
            routes: Set<Route>,
            stop: Stop,
            data: ByDirectionBuilder,
            globalData: GlobalResponse,
        ) : this(stop, LineOrRoute.Line(line, routes), data, globalData)

        constructor(
            stop: Stop,
            lineOrRoute: LineOrRoute,
            data: ByDirectionBuilder,
            globalData: GlobalResponse,
        ) : this(
            lineOrRoute,
            stop,
            lineOrRoute.directions(
                globalData,
                stop,
                data.values.mapNotNull { it.routePatterns }.flatten(),
            ),
            data,
        )

        fun build(): RouteStopData {
            return RouteStopData(
                lineOrRoute,
                stop,
                directions,
                data.values.map { it.build() }.sort(),
            )
        }
    }

    internal data class LeafBuilder(
        val lineOrRoute: LineOrRoute,
        val stop: Stop,
        val directionId: Int,
        var routePatterns: List<RoutePattern>? = null,
        var patternsNotSeenAtEarlierStops: Set<String>? = routePatterns?.map { it.id }?.toSet(),
        var stopIds: Set<String>? = null,
        var upcomingTrips: List<UpcomingTrip>? = null,
        var alertsHere: List<Alert>? = null,
        var allDataLoaded: Boolean? = null,
        var hasSchedulesTodayByPattern: Map<String, Boolean>? = null,
        var alertsDownstream: List<Alert>? = null,
        val context: Context,
    ) {

        fun build(): Leaf {
            return Leaf(
                lineOrRoute,
                stop,
                directionId,
                checkNotNull(routePatterns),
                checkNotNull(stopIds),
                this.upcomingTrips ?: emptyList(),
                checkNotNull(alertsHere),
                allDataLoaded ?: false,
                hasSchedulesTodayByPattern
                    ?: checkNotNull(routePatterns).associate { it.id to false },
                checkNotNull(alertsDownstream),
                context,
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
         * - Any pattern that it serves that isn't served by an earlier stop is typical
         * - Any pattern that it serves that isn't served by an earlier stop has an upcoming trip
         *   within the cutoff parameters (if all of the upcoming trips are for patterns served by
         *   an earlier stop, then this leaf should not be shown)
         * - it has upcoming service that is not arrival-only
         */
        fun shouldShow(
            stop: Stop,
            filterAtTime: EasternTimeInstant,
            cutoffTime: EasternTimeInstant?,
            showAllPatternsWhileLoading: Boolean,
            lineOrRoute: LineOrRoute,
            globalData: GlobalResponse,
        ): Boolean {
            if (this.allDataLoaded == false && showAllPatternsWhileLoading) return true
            val isBus = lineOrRoute.type == RouteType.BUS
            val isSubway = lineOrRoute.isSubway

            // Only take the next 2 (if bus) or 3 upcoming trips into account, since more than that
            // can never be shown in nearby transit.
            val upcomingTripsInCutoff =
                when (cutoffTime) {
                    null -> this.upcomingTrips?.filter { it.isUpcoming() }
                    else ->
                        this.upcomingTrips?.filter { it.isUpcomingWithin(filterAtTime, cutoffTime) }
                }?.take(if (isBus) TYPICAL_LEAF_ROWS else BRANCHING_LEAF_ROWS)

            val hasUnseenUpcomingTrip =
                upcomingTripsInCutoff?.any { upcomingTrip ->
                    upcomingTrip.trip.routePatternId?.let {
                        // If there isn't a route pattern for the trip (rare GL cases), assume it
                        // hasn't been seen elsewhere and that we should show this leaf.
                        this.patternsNotSeenAtEarlierStops?.contains(it)
                    } ?: true
                } ?: false

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

            val hasUnseenTypicalPattern =
                routePatterns?.any {
                    (patternsNotSeenAtEarlierStops?.contains(it.id) ?: false) && it.isTypical()
                } ?: false

            return (hasUnseenTypicalPattern || hasUnseenUpcomingTrip) &&
                !(shouldBeFilteredAsArrivalOnly)
        }

        private fun isTypicalLastStopOnRoutePattern(
            stop: Stop,
            globalData: GlobalResponse,
        ): Boolean {
            return this.routePatterns
                ?.filter { it.typicality == RoutePattern.Typicality.Typical }
                ?.map { it.representativeTripId }
                ?.all { representativeTripId ->
                    val representativeTrip = globalData.trips[representativeTripId]
                    val lastStopIdInPattern =
                        representativeTrip?.stopIds?.last() ?: return@all false
                    lastStopIdInPattern == stop.id ||
                        stop.childStopIds.contains(lastStopIdInPattern)
                } ?: false
        }
    }
}

@JvmName("optionalHasContext")
public fun List<RouteCardData>?.hasContext(context: RouteCardData.Context): Boolean =
    this?.hasContext(context) == true

internal fun List<RouteCardData>.hasContext(context: RouteCardData.Context): Boolean =
    this.any {
        it.stopData.any { stopData -> stopData.data.any { leaf -> leaf.context == context } }
    }

internal fun List<RouteCardData>.sort(
    distanceFrom: Position?,
    context: RouteCardData.Context,
): List<RouteCardData> = this.sortedWith(PatternSorting.compareRouteCards(distanceFrom, context))

internal fun List<RouteCardData.RouteStopData>.sort(
    distanceFrom: Position?
): List<RouteCardData.RouteStopData> =
    this.sortedWith(PatternSorting.compareStopsOnRoute(distanceFrom))

internal fun List<RouteCardData.Leaf>.sort(): List<RouteCardData.Leaf> =
    this.sortedWith(PatternSorting.compareLeavesAtStop())
