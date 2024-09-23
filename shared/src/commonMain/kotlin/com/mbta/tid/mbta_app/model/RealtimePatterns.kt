package com.mbta.tid.mbta_app.model

import kotlinx.datetime.Instant

typealias UpcomingTripsMap = Map<RealtimePatterns.UpcomingTripKey, List<UpcomingTrip>>

sealed class RealtimePatterns : Comparable<RealtimePatterns> {
    sealed class UpcomingTripKey {
        data class ByRoutePattern(
            val routeId: String,
            val routePatternId: String?,
            val stopId: String
        ) : UpcomingTripKey()

        data class ByDirection(val routeId: String, val direction: Int, val stopId: String) :
            UpcomingTripKey()
    }

    abstract val id: String

    abstract val patterns: List<RoutePattern>
    abstract val upcomingTrips: List<UpcomingTrip>?
    abstract val alertsHere: List<Alert>?
    abstract val hasSchedulesToday: Boolean

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
        override val hasSchedulesToday: Boolean = true,
    ) : RealtimePatterns() {
        override val id = headsign

        constructor(
            staticData: NearbyStaticData.StaticPatterns.ByHeadsign,
            upcomingTripsMap: UpcomingTripsMap?,
            alerts: Collection<Alert>?,
            hasSchedulesTodayByPattern: Map<String, Boolean>?,
        ) : this(
            staticData.route,
            staticData.headsign,
            staticData.line,
            staticData.patterns,
            if (upcomingTripsMap != null) {
                staticData.stopIds
                    .map { stopId ->
                        staticData.patterns
                            .mapNotNull { pattern ->
                                upcomingTripsMap[
                                    UpcomingTripKey.ByRoutePattern(
                                        staticData.route.id,
                                        pattern.id,
                                        stopId
                                    )]
                            }
                            .flatten()
                    }
                    .flatten()
                    .sorted()
            } else {
                null
            },
            alerts?.let {
                applicableAlerts(
                    routes = listOf(staticData.route),
                    stopIds = staticData.stopIds,
                    alerts = alerts
                )
            },
            hasSchedulesToday(hasSchedulesTodayByPattern, staticData.patterns),
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
        override val hasSchedulesToday: Boolean = true,
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
            alerts: Collection<Alert>?,
            hasSchedulesTodayByPattern: Map<String, Boolean>?,
        ) : this(
            staticData.line,
            staticData.routes,
            staticData.direction,
            staticData.patterns,
            if (upcomingTripsMap != null) {
                staticData.stopIds
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
                applicableAlerts(
                    routes = staticData.routes,
                    stopIds = staticData.stopIds,
                    alerts = alerts
                )
            },
            hasSchedulesToday(hasSchedulesTodayByPattern, staticData.patterns),
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

    fun format(now: Instant, routeType: RouteType, context: TripInstantDisplay.Context): Format {
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
    ): Format {
        val majorAlert = alertsHere?.firstOrNull { it.significance >= AlertSignificance.Major }
        if (majorAlert != null) return Format.NoService(majorAlert)
        val secondaryAlert =
            alertsHere
                ?.firstOrNull { it.significance >= AlertSignificance.Secondary }
                ?.let {
                    Format.SecondaryAlert(
                        it,
                        MapStopRoute.matching(
                            when (this) {
                                is ByHeadsign -> route
                                is ByDirection -> representativeRoute
                            }
                        )
                    )
                }
        if (this.upcomingTrips == null) return Format.Loading
        val allTrips = upcomingTrips ?: emptyList()
        val tripsToShow =
            allTrips
                .mapNotNull {
                    val isSubway =
                        when (this) {
                                is ByHeadsign -> this.route
                                is ByDirection -> this.routes.min()
                            }
                            .type
                            .isSubway()
                    formatUpcomingTrip(now, it, routeType, context, isSubway)
                }
                .take(count)
        if (tripsToShow.isEmpty()) {
            return if (hasSchedulesToday) Format.None(secondaryAlert)
            else Format.NoSchedulesToday(secondaryAlert)
        }
        return Format.Some(tripsToShow, secondaryAlert)
    }

    /**
     * Checks if any pattern under this headsign is [RoutePattern.Typicality.Typical].
     *
     * If any typicality is unknown, the route should be shown, and so this will return true.
     */
    fun isTypical() =
        patterns.any { it.typicality == null || it.typicality == RoutePattern.Typicality.Typical }

    /**
     * Checks if a trip exists in the near future, or the recent past if the vehicle has not yet
     * left this stop.
     *
     * If [upcomingTrips] are unavailable (i.e. null), returns false, since non-typical patterns
     * should be hidden until data is available.
     */
    fun isUpcomingWithin(currentTime: Instant, cutoffTime: Instant) =
        upcomingTrips?.any {
            val tripTime = it.time
            tripTime != null &&
                tripTime < cutoffTime &&
                (tripTime >= currentTime ||
                    (it.prediction != null && it.prediction.stopId == it.vehicle?.stopId))
        }
            ?: false

    /**
     * Checks if a trip exists.
     *
     * If [upcomingTrips] are unavailable (i.e. null), returns false, since non-typical patterns
     * should be hidden until data is available.
     */
    fun isUpcoming() =
        upcomingTrips?.any {
            val tripTime = it.time
            tripTime != null
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
        abstract val secondaryAlert: SecondaryAlert?

        data class SecondaryAlert(val iconName: String, val alertEffect: Alert.Effect) {
            constructor(
                alert: Alert,
                mapStopRoute: MapStopRoute?
            ) : this(
                "alert-${mapStopRoute?.let { "large-${it.name.lowercase()}"} ?: "borderless"}-${alert.alertState.name.lowercase()}",
                alert.effect
            )
        }

        data object Loading : Format() {
            override val secondaryAlert = null
        }

        data class None(override val secondaryAlert: SecondaryAlert?) : Format()

        data class NoSchedulesToday(override val secondaryAlert: SecondaryAlert?) : Format()

        data class Some(
            val trips: List<FormatWithId>,
            override val secondaryAlert: SecondaryAlert?
        ) : Format() {
            data class FormatWithId(
                val id: String,
                val routeType: RouteType,
                val format: TripInstantDisplay
            ) {
                constructor(
                    trip: UpcomingTrip,
                    routeType: RouteType,
                    now: Instant,
                    context: TripInstantDisplay.Context
                ) : this(trip.trip.id, routeType, trip.format(now, routeType, context))
            }
        }

        data class NoService(val alert: Alert) : Format() {
            override val secondaryAlert = null
        }
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
            stopIds
                .flatMap { stopId ->
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
                .distinct()

        fun formatUpcomingTrip(
            now: Instant,
            upcomingTrip: UpcomingTrip,
            routeType: RouteType,
            context: TripInstantDisplay.Context,
            isSubway: Boolean
        ): Format.Some.FormatWithId? {
            return Format.Some.FormatWithId(upcomingTrip, routeType, now, context).takeUnless {
                it.format is TripInstantDisplay.Hidden ||
                    it.format is TripInstantDisplay.Skipped ||
                    // API best practices call for hiding scheduled times on subway
                    (isSubway && it.format is TripInstantDisplay.Schedule)
            }
        }

        fun hasSchedulesToday(
            optionalHasSchedulesTodayByPattern: Map<String, Boolean>?,
            patterns: List<RoutePattern>
        ): Boolean {
            val hasSchedulesTodayByPattern = optionalHasSchedulesTodayByPattern ?: return true
            return patterns.any { pattern -> hasSchedulesTodayByPattern[pattern.id] == true }
        }
    }
}
