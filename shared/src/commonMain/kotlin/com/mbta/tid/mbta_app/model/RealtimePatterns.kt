package com.mbta.tid.mbta_app.model

import kotlinx.datetime.Instant

typealias UpcomingTripsMap = Map<RealtimePatterns.UpcomingTripKey, List<UpcomingTrip>>

sealed class RealtimePatterns : Comparable<RealtimePatterns> {
    sealed class UpcomingTripKey {
        data class ByHeadsign(val routeId: String, val headsign: String, val stopId: String) :
            UpcomingTripKey()

        data class ByDirection(val routeId: String, val direction: Int, val stopId: String) :
            UpcomingTripKey()
    }

    abstract val patterns: List<RoutePattern>
    abstract val upcomingTrips: List<UpcomingTrip>?
    abstract val alertsHere: List<Alert>?
    abstract val id: String

    /**
     * @property patterns [RoutePattern] listed in ascending order based on [RoutePattern.sortOrder]
     * @property upcomingTrips Every [UpcomingTrip] for the [Stop] in the containing
     *   [PatternsByStop] for any of these [patterns]
     */
    data class ByHeadsign(
        val route: Route,
        val headsign: String,
        val line: Line?,
        override val patterns: List<RoutePattern>,
        override val upcomingTrips: List<UpcomingTrip>? = null,
        override val alertsHere: List<Alert>? = null,
    ) : RealtimePatterns() {
        override val id = headsign

        constructor(
            staticData: NearbyStaticData.StaticPatterns.ByHeadsign,
            upcomingTripsMap: UpcomingTripsMap?,
            stopIds: Set<String>,
            alerts: Collection<Alert>?,
        ) : this(
            staticData.route,
            staticData.headsign,
            staticData.line,
            staticData.patterns,
            if (upcomingTripsMap != null) {
                stopIds
                    .mapNotNull { stopId ->
                        upcomingTripsMap[
                            UpcomingTripKey.ByHeadsign(
                                staticData.route.id,
                                staticData.headsign,
                                stopId
                            )]
                    }
                    .flatten()
                    .sorted()
            } else {
                null
            },
            alerts?.let {
                applicableAlerts(
                    routes = listOf(staticData.route),
                    stopIds = stopIds,
                    alerts = alerts
                )
            }
        )
    }

    /**
     * @property patterns [RoutePattern] listed in ascending order based on [RoutePattern.sortOrder]
     * @property upcomingTrips Every [UpcomingTrip] for the [Stop] in the containing
     *   [PatternsByStop] for any of these [patterns]
     */
    data class ByDirection(
        val line: Line,
        val routes: List<Route>,
        val direction: Direction,
        override val patterns: List<RoutePattern>,
        override val upcomingTrips: List<UpcomingTrip>? = null,
        override val alertsHere: List<Alert>? = null,
    ) : RealtimePatterns() {
        override val id = "${line.id}:${direction.id}"
        val representativeRoute = routes.min()
        val routesByTrip =
            upcomingTrips
                ?.mapNotNull {
                    val route =
                        routes.firstOrNull { route -> route.id == it.trip.routeId }
                            ?: return@mapNotNull null
                    Pair(it.trip.id, route)
                }
                ?.toMap()
                ?: emptyMap()

        constructor(
            staticData: NearbyStaticData.StaticPatterns.ByDirection,
            upcomingTripsMap: UpcomingTripsMap?,
            stopIds: Set<String>,
            alerts: Collection<Alert>?,
        ) : this(
            staticData.line,
            staticData.routes,
            staticData.direction,
            staticData.patterns,
            if (upcomingTripsMap != null) {
                stopIds
                    .flatMap { stopId ->
                        staticData.routes.mapNotNull { route ->
                            upcomingTripsMap[
                                UpcomingTripKey.ByDirection(
                                    route.id,
                                    staticData.direction.id,
                                    stopId
                                )]
                        }
                    }
                    .flatten()
                    .sorted()
            } else {
                null
            },
            alerts?.let {
                applicableAlerts(routes = staticData.routes, stopIds = stopIds, alerts = alerts)
            }
        )
    }

    override fun compareTo(other: RealtimePatterns): Int =
        compareValuesBy(
            this,
            other,
            { it.directionId() },
            {
                when (it) {
                    is ByDirection -> -1
                    is ByHeadsign -> 1
                }
            },
            { it.patterns.first() }
        )

    fun alertsHereFor(stopIds: Set<String>, directionId: Int): List<Alert>? {
        return alertsHere?.let {
            when (this) {
                is ByHeadsign ->
                    applicableAlerts(
                        routes = listOf(route),
                        stopIds = stopIds,
                        directionId = directionId,
                        alerts = it
                    )
                is ByDirection ->
                    applicableAlerts(
                        routes = routes,
                        stopIds = stopIds,
                        directionId = directionId,
                        alerts = it
                    )
            }
        }
    }

    fun format(now: Instant, context: TripInstantDisplay.Context): Format {
        return this.format(
            now,
            when (this) {
                is ByHeadsign -> 2
                is ByDirection -> 3
            },
            context
        )
    }

    fun format(now: Instant, count: Int, context: TripInstantDisplay.Context): Format {
        val alert = alertsHere?.firstOrNull()
        if (alert != null) return Format.NoService(alert)
        if (this.upcomingTrips == null) return Format.Loading
        val allTrips = upcomingTrips ?: emptyList()
        val tripsToShow =
            allTrips
                .map { Format.Some.FormatWithId(it, now, context) }
                .filterNot {
                    it.format is TripInstantDisplay.Hidden ||
                        it.format is TripInstantDisplay.Skipped ||
                        // API best practices call for hiding scheduled times on subway
                        (when (this) {
                                is ByHeadsign -> this.route
                                is ByDirection -> this.routes.min()
                            }
                            .type
                            .isSubway() && it.format is TripInstantDisplay.Schedule)
                }
                .take(count)
        if (tripsToShow.isEmpty()) return Format.None
        return Format.Some(tripsToShow)
    }

    /**
     * Checks if any pattern under this headsign is [RoutePattern.Typicality.Typical].
     *
     * If any typicality is unknown, the route should be shown, and so this will return true.
     */
    fun isTypical() =
        patterns.any { it.typicality == null || it.typicality == RoutePattern.Typicality.Typical }

    /**
     * Checks if a trip exists before the given cutoff time.
     *
     * If [upcomingTrips] are unavailable (i.e. null), returns false, since non-typical patterns
     * should be hidden until data is available.
     */
    fun isUpcomingBefore(cutoffTime: Instant) =
        upcomingTrips?.any {
            val tripTime = it.time
            tripTime != null && tripTime < cutoffTime
        }
            ?: false

    /**
     * Checks if a trip exists after the given cutoff time.
     *
     * If [upcomingTrips] are unavailable (i.e. null), returns false, since non-typical patterns
     * should be hidden until data is available.
     */
    fun isUpcomingAfter(cutoffTime: Instant) =
        upcomingTrips?.any {
            val tripTime = it.time
            tripTime != null && tripTime > cutoffTime
        }
            ?: false

    /**
     * Checks if this headsign ends at this stop, i.e. all trips are arrival-only.
     *
     * Criteria:
     * - Trips are loaded
     * - At least one trip is scheduled as arrival-only
     * - No trips are scheduled or predicted with a departure
     */
    fun isArrivalOnly(): Boolean {
        // Intermediate variable set because kotlin can't smart cast properties with open getters
        val upcoming = upcomingTrips
        return upcoming != null &&
            upcoming
                .mapTo(mutableSetOf()) { it.isArrivalOnly() }
                .let { upcomingTripsArrivalOnly ->
                    upcomingTripsArrivalOnly.contains(true) &&
                        !upcomingTripsArrivalOnly.contains(false)
                }
    }

    fun directionId(): Int {
        val upcoming = upcomingTrips
        if (upcoming != null) {
            for (upcomingTrip in upcoming) {
                return upcomingTrip.trip.directionId
            }
        }
        for (pattern in patterns) {
            return pattern.directionId
        }
        // there shouldn't be a headsign with no trips and no patterns
        throw NoSuchElementException("Got directionId of empty PatternsByHeadsign")
    }

    sealed class Format {
        data object Loading : Format()

        data object None : Format()

        data class Some(val trips: List<FormatWithId>) : Format() {
            data class FormatWithId(val id: String, val format: TripInstantDisplay) {
                constructor(
                    trip: UpcomingTrip,
                    now: Instant,
                    context: TripInstantDisplay.Context
                ) : this(trip.trip.id, trip.format(now, context))
            }
        }

        data class NoService(val alert: Alert) : Format()
    }

    companion object {

        /**
         * Returns alerts that are applicable to the passed in routes and stops
         *
         * Criteria:
         * - Route ID matches an alert [Alert.InformedEntity]
         * - Stop ID matches an alert [Alert.InformedEntity]
         * - Alert's informed entity activities contains [Alert.InformedEntity.Activity.Board]
         */
        fun applicableAlerts(
            routes: List<Route>,
            stopIds: Set<String>,
            directionId: Int? = null,
            alerts: Collection<Alert>
        ): List<Alert> =
            stopIds.flatMap { stopId ->
                alerts.filter { alert ->
                    alert.anyInformedEntity {
                        routes.any { route ->
                            it.appliesTo(
                                directionId = directionId,
                                routeId = route.id,
                                stopId = stopId
                            )
                        } && it.activities.contains(Alert.InformedEntity.Activity.Board)
                    }
                }
            }
    }
}
