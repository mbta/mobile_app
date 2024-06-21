package com.mbta.tid.mbta_app.model

import io.github.dellisd.spatialk.geojson.Position
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi
import io.github.dellisd.spatialk.turf.distance
import kotlinx.datetime.Instant

sealed class UpcomingTripKey() {
    data class ByHeadsign(val routeId: String, val headsign: String, val stopId: String) :
        UpcomingTripKey()

    data class ByDirection(val routeId: String, val direction: Int, val stopId: String) :
        UpcomingTripKey()
}

typealias UpcomingTripsMap = Map<UpcomingTripKey, List<UpcomingTrip>>

sealed class Patterns(
    val label: String,
) : Comparable<Patterns> {
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
        override val patterns: List<RoutePattern>,
        override val upcomingTrips: List<UpcomingTrip>? = null,
        override val alertsHere: List<Alert>? = null,
    ) : Patterns(headsign) {
        override val id = headsign

        constructor(
            staticData: NearbyStaticData.StaticPatterns.ByHeadsign,
            upcomingTripsMap: UpcomingTripsMap?,
            stopIds: Set<String>,
            alerts: Collection<Alert>?,
        ) : this(
            staticData.route,
            staticData.headsign,
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
            if (alerts != null) {
                stopIds.flatMap { stopId ->
                    alerts.filter { alert ->
                        alert.anyInformedEntity {
                            it.appliesTo(routeId = staticData.route.id, stopId = stopId) &&
                                it.activities.contains(Alert.InformedEntity.Activity.Board)
                        }
                    }
                }
            } else {
                null
            }
        )

        override fun compareTo(other: Patterns): Int =
            patterns.first().compareTo(other.patterns.first())

        fun format(now: Instant): Format {
            val alert = alertsHere?.firstOrNull()
            if (alert != null) return Format.NoService(alert)
            if (this.upcomingTrips == null) return Format.Loading
            val tripsToShow =
                upcomingTrips
                    .map { Format.Some.FormatWithId(it, now) }
                    .filterNot {
                        it.format is UpcomingTrip.Format.Hidden ||

                            // API best practices call for hiding scheduled times on subway
                            (this.route.type.isSubway() &&
                                it.format is UpcomingTrip.Format.Schedule)
                    }
                    .take(2)
            if (tripsToShow.isEmpty()) return Format.None
            return Format.Some(tripsToShow)
        }
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
    ) : Patterns(direction.destination) {
        override val id = direction.destination
        val representativeRoute = routes.min()

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
            if (alerts != null) {
                stopIds.flatMap { stopId ->
                    alerts.filter { alert ->
                        alert.anyInformedEntity {
                            staticData.routes.any { route ->
                                it.appliesTo(routeId = route.id, stopId = stopId)
                            } && it.activities.contains(Alert.InformedEntity.Activity.Board)
                        }
                    }
                }
            } else {
                null
            }
        )

        override fun compareTo(other: Patterns): Int =
            patterns.first().compareTo(other.patterns.first())

        fun format(now: Instant): Format {
            val alert = alertsHere?.firstOrNull()
            if (alert != null) return Format.NoService(alert)
            if (this.upcomingTrips == null) return Format.Loading
            //            println("${this.id} - ${upcomingTrips.size} upcoming,
            // ${upcomingTrips.first().trip.id}...")
            val tripsToShow =
                upcomingTrips
                    .map { Format.Some.FormatWithId(it, now) }
                    .filterNot {
                        it.format is UpcomingTrip.Format.Hidden ||
                            // API best practices call for hiding scheduled times on subway
                            (this.routes.min().type.isSubway() &&
                                it.format is UpcomingTrip.Format.Schedule)
                    }
                    .take(3)
            if (tripsToShow.isEmpty()) return Format.None
            return Format.Some(tripsToShow)
        }
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
            data class FormatWithId(val id: String, val format: UpcomingTrip.Format) {
                constructor(trip: UpcomingTrip, now: Instant) : this(trip.trip.id, trip.format(now))
            }
        }

        data class NoService(val alert: Alert) : Format()
    }
}

/**
 * @property patternsByHeadsign [RoutePattern]s serving the stop grouped by headsign. The headsigns
 *   are listed in ascending order based on [RoutePattern.sortOrder]
 */
data class PatternsByStop(
    val routes: List<Route>,
    val line: Line?,
    val stop: Stop,
    val patterns: List<Patterns>,
    val directions: List<Direction>
) {
    val representativeRoute = routes.min()
    val routeIdentifier = line?.id ?: representativeRoute.id
    val position = Position(longitude = stop.longitude, latitude = stop.latitude)

    constructor(
        staticData: NearbyStaticData.StopPatterns,
        upcomingTripsMap: UpcomingTripsMap?,
        cutoffTime: Instant,
        alerts: Collection<Alert>?,
    ) : this(
        when (staticData) {
            is NearbyStaticData.StopPatterns.ForRoute -> listOf(staticData.route)
            is NearbyStaticData.StopPatterns.ForLine -> staticData.routes
        },
        when (staticData) {
            is NearbyStaticData.StopPatterns.ForRoute -> null
            is NearbyStaticData.StopPatterns.ForLine -> staticData.line
        },
        staticData.stop,
        staticData.patterns
            .map {
                when (it) {
                    is NearbyStaticData.StaticPatterns.ByHeadsign ->
                        Patterns.ByHeadsign(it, upcomingTripsMap, staticData.allStopIds, alerts)
                    is NearbyStaticData.StaticPatterns.ByDirection ->
                        Patterns.ByDirection(it, upcomingTripsMap, staticData.allStopIds, alerts)
                }
            }
            .filter { (it.isTypical() || it.isUpcomingBefore(cutoffTime)) && !it.isArrivalOnly() }
            .sorted(),
        staticData.directions
    )

    constructor(
        route: Route,
        stop: Stop,
        patterns: List<Patterns>
    ) : this(listOf(route), null, stop, patterns, listOf(Direction(0, route), Direction(1, route)))

    @OptIn(ExperimentalTurfApi::class)
    fun distanceFrom(position: Position) = distance(position, this.position)

    fun allUpcomingTrips(): List<UpcomingTrip> =
        this.patterns.flatMap { it.upcomingTrips ?: emptyList() }.sorted()
}

sealed class StopsAssociated(val id: String) {
    fun distanceFrom(position: Position): Double =
        when (this) {
            is WithRoute -> this.distance(position)
            is WithLine -> this.distance(position)
        }

    fun isEmpty(): Boolean =
        when (this) {
            is WithRoute -> this.patternsByStop.isEmpty()
            is WithLine -> this.patternsByStop.isEmpty()
        }

    fun sortRoute(): Route =
        when (this) {
            is WithRoute -> this.route
            is WithLine -> this.routes.min()
        }

    /**
     * @property patternsByStop A list of route patterns grouped by the station or stop that they
     *   serve.
     */
    data class WithRoute(
        val route: Route,
        val patternsByStop: List<PatternsByStop>,
    ) : StopsAssociated("route-${route.id}") {
        @OptIn(ExperimentalTurfApi::class)
        fun distance(position: Position): Double =
            distance(position, patternsByStop.first().position)
    }

    data class WithLine(
        val line: Line,
        val routes: List<Route>,
        val patternsByStop: List<PatternsByStop>,
    ) : StopsAssociated("line-${line.id}") {
        @OptIn(ExperimentalTurfApi::class)
        fun distance(position: Position): Double =
            distance(position, patternsByStop.first().position)
    }
}
