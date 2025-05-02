package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import kotlinx.datetime.Instant

typealias UpcomingTripsMap = Map<RealtimePatterns.UpcomingTripKey, List<UpcomingTrip>>

internal fun List<UpcomingTrip>.filterCancellations(isSubway: Boolean): List<UpcomingTrip> {
    /**
     * Do not display cancelled/skipped trips in contexts where there are only two departures shown
     * since it's more important that riders know about trips they can still take. Never show
     * cancelled trips for subways.
     */
    return if (this.size <= 2) {
        this.filter { trip -> !trip.isCancelled }
    } else {
        this.filter { trip ->
            if (isSubway) {
                !trip.isCancelled
            } else {
                true
            }
        }
    }
}

internal fun UpcomingTripsMap.filterCancellations(isSubway: Boolean): UpcomingTripsMap =
    this.entries.associate { it.key to it.value.filterCancellations(isSubway) }

sealed class RealtimePatterns : ILeafData {
    sealed class UpcomingTripKey {
        data class ByRoutePattern(
            val routeId: String,
            val routePatternId: String?,
            val parentStopId: String
        ) : UpcomingTripKey()

        data class ByDirection(val routeId: String, val direction: Int, val parentStopId: String) :
            UpcomingTripKey()
    }

    abstract val id: String

    // contains null if an added trip with no pattern is included in the upcoming trips
    abstract val patterns: List<RoutePattern?>
    abstract override val upcomingTrips: List<UpcomingTrip>
    abstract val alertsHere: List<Alert>?
    abstract val alertsDownstream: List<Alert>?
    abstract override val hasSchedulesToday: Boolean
    abstract val allDataLoaded: Boolean

    override val hasMajorAlerts
        get() = run {
            this.alertsHere?.any { alert -> alert.significance == AlertSignificance.Major } == true
        }

    /**
     * @property patterns [RoutePattern] listed in ascending order based on [RoutePattern.sortOrder]
     * @property upcomingTrips Every [UpcomingTrip] for the [Stop] in the containing
     *   [PatternsByStop] for any of these [patterns]
     */
    data class ByHeadsign
    @DefaultArgumentInterop.Enabled
    constructor(
        val route: Route,
        val headsign: String,
        val line: Line?,
        override val patterns: List<RoutePattern?>,
        override val upcomingTrips: List<UpcomingTrip>,
        override val alertsHere: List<Alert> = emptyList(),
        override val alertsDownstream: List<Alert> = emptyList(),
        override val hasSchedulesToday: Boolean = true,
        override val allDataLoaded: Boolean = true,
    ) : RealtimePatterns() {
        override val id = headsign

        constructor(
            staticData: NearbyStaticData.StaticPatterns.ByHeadsign,
            upcomingTripsMap: UpcomingTripsMap,
            parentStopId: String,
            alertsHere: List<Alert>,
            alertsDownstream: List<Alert>,
            hasSchedulesTodayByPattern: Map<String, Boolean>?,
            allDataLoaded: Boolean,
        ) : this(
            staticData.route,
            staticData.headsign,
            staticData.line,
            staticData.patterns,
            staticData.patterns
                .mapNotNull { pattern ->
                    upcomingTripsMap[
                        UpcomingTripKey.ByRoutePattern(
                            staticData.route.id,
                            pattern.id,
                            parentStopId
                        )]
                }
                .flatten()
                .sorted(),
            alertsHere,
            alertsDownstream,
            hasSchedulesToday(hasSchedulesTodayByPattern, staticData.patterns),
            allDataLoaded,
        )
    }

    /**
     * @property patterns [RoutePattern] listed in ascending order based on [RoutePattern.sortOrder]
     * @property upcomingTrips Every [UpcomingTrip] for the [Stop] in the containing
     *   [PatternsByStop] for any of these [patterns]
     */
    data class ByDirection
    @DefaultArgumentInterop.Enabled
    constructor(
        val line: Line,
        val routes: List<Route>,
        val direction: Direction,
        override val patterns: List<RoutePattern?>,
        override val upcomingTrips: List<UpcomingTrip>,
        override val alertsHere: List<Alert> = emptyList(),
        override val alertsDownstream: List<Alert>? = emptyList(),
        override val hasSchedulesToday: Boolean = true,
        override val allDataLoaded: Boolean = true,
    ) : RealtimePatterns() {
        override val id = "${line.id}:${direction.id}"
        val representativeRoute = routes.min()
        val routesByTrip =
            upcomingTrips
                .mapNotNull {
                    val route =
                        routes.firstOrNull { route -> route.id == it.trip.routeId }
                            ?: return@mapNotNull null
                    Pair(it.trip.id, route)
                }
                .toMap()

        constructor(
            staticData: NearbyStaticData.StaticPatterns.ByDirection,
            upcomingTripsMap: UpcomingTripsMap,
            parentStopId: String,
            alertsHere: List<Alert>,
            alertsDownstream: List<Alert>,
            hasSchedulesTodayByPattern: Map<String, Boolean>?,
            allDataLoaded: Boolean,
        ) : this(
            staticData.line,
            staticData.routes,
            staticData.direction,
            staticData.patterns,
            staticData.routes
                .mapNotNull { route ->
                    upcomingTripsMap[
                        UpcomingTripKey.ByDirection(
                            route.id,
                            staticData.direction.id,
                            parentStopId
                        )]
                }
                .flatten()
                .sorted(),
            alertsHere,
            alertsDownstream,
            hasSchedulesToday(hasSchedulesTodayByPattern, staticData.patterns),
            allDataLoaded,
        )
    }

    fun alertsHereFor(stopIds: Set<String>, directionId: Int, tripId: String?): List<Alert>? {
        val routeIds =
            when (this) {
                is ByHeadsign -> listOf(this.route.id)
                is ByDirection -> this.routes.map { it.id }
            }
        return if (alertsHere != null) {
            Alert.applicableAlerts(alertsHere as List, directionId, routeIds, stopIds, tripId)
        } else {
            null
        }
    }

    fun format(
        now: Instant,
        routeType: RouteType,
        context: TripInstantDisplay.Context
    ): UpcomingFormat {
        return this.format(
            now,
            routeType,
            when (this) {
                is ByHeadsign -> 2
                is ByDirection -> 3
            },
            context
        )
    }

    fun format(
        now: Instant,
        routeType: RouteType,
        count: Int,
        context: TripInstantDisplay.Context
    ): UpcomingFormat {
        val mapStopRoute =
            MapStopRoute.matching(
                when (this) {
                    is ByHeadsign -> route
                    is ByDirection -> representativeRoute
                }
            )
        val majorAlert = alertsHere?.firstOrNull { it.significance >= AlertSignificance.Major }
        if (majorAlert != null) return UpcomingFormat.Disruption(majorAlert, mapStopRoute)
        val secondaryAlertToDisplay =
            alertsHere?.firstOrNull { it.significance >= AlertSignificance.Secondary }
                ?: alertsDownstream?.firstOrNull()

        val secondaryAlert =
            secondaryAlertToDisplay?.let {
                UpcomingFormat.SecondaryAlert(StopAlertState.Issue, mapStopRoute)
            }

        val tripsToShow =
            upcomingTrips
                .mapNotNull {
                    val isSubway =
                        when (this) {
                                is ByHeadsign -> this.route
                                is ByDirection -> this.routes.min()
                            }
                            .type
                            .isSubway()
                    it.format(now, routeType, context, isSubway)
                }
                .take(count)
        return when {
            tripsToShow.isNotEmpty() -> UpcomingFormat.Some(tripsToShow, secondaryAlert)
            !allDataLoaded -> UpcomingFormat.Loading
            else ->
                UpcomingFormat.NoTrips(
                    UpcomingFormat.NoTripsFormat.fromUpcomingTrips(
                        upcomingTrips,
                        hasSchedulesToday,
                        now
                    ),
                    secondaryAlert
                )
        }
    }

    /**
     * Checks if any pattern under this headsign is [RoutePattern.Typicality.Typical].
     *
     * If any typicality is unknown, the route should be shown, and so this will return true.
     */
    fun isTypical() = patterns.any { it?.isTypical() ?: true }

    /**
     * Checks if a trip exists in the near future, or the recent past if the vehicle has not yet
     * left this stop.
     *
     * If [upcomingTrips] are unavailable (i.e. null), returns false, since non-typical patterns
     * should be hidden until data is available.
     */
    fun isUpcomingWithin(currentTime: Instant, cutoffTime: Instant) =
        upcomingTrips.any { it.isUpcomingWithin(currentTime, cutoffTime) }

    /** Checks if a trip exists. */
    fun isUpcoming() = upcomingTrips.any { it.isUpcoming() }

    /**
     * Checks if this headsign ends at this stop, i.e. all trips are arrival-only.
     *
     * Criteria:
     * - At least one trip is scheduled as arrival-only
     * - No trips are scheduled or predicted with a departure
     */
    fun isArrivalOnly(): Boolean {
        return upcomingTrips.isArrivalOnly()
    }

    fun directionId(): Int {
        val upcoming = upcomingTrips
        for (upcomingTrip in upcoming) {
            return upcomingTrip.trip.directionId
        }
        for (pattern in patterns) {
            if (pattern != null) return pattern.directionId
        }
        // there shouldn't be a headsign with no trips and no patterns
        throw NoSuchElementException("Got directionId of empty PatternsByHeadsign")
    }

    companion object {
        fun hasSchedulesToday(
            optionalHasSchedulesTodayByPattern: Map<String, Boolean>?,
            patterns: Collection<RoutePattern?>
        ): Boolean {
            val hasSchedulesTodayByPattern = optionalHasSchedulesTodayByPattern ?: return true
            return patterns.any { pattern -> hasSchedulesTodayByPattern[pattern?.id] == true }
        }
    }
}
