package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import io.github.dellisd.spatialk.geojson.Position
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi
import io.github.dellisd.spatialk.turf.distance
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Instant

data class UpcomingTripKey(val routeId: String, val headsign: String, val stopId: String)

typealias UpcomingTripsMap = Map<UpcomingTripKey, List<UpcomingTrip>>

/**
 * @property patterns [RoutePattern] listed in ascending order based on [RoutePattern.sortOrder]
 * @property upcomingTrips Every [UpcomingTrip] for the [Stop] in the containing [PatternsByStop]
 *   for any of these [patterns]
 */
data class PatternsByHeadsign(
    val route: Route,
    val headsign: String,
    val patterns: List<RoutePattern>,
    val upcomingTrips: List<UpcomingTrip>? = null,
    val alertsHere: List<Alert>? = null,
) : Comparable<PatternsByHeadsign> {
    constructor(
        staticData: NearbyStaticData.HeadsignWithPatterns,
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
                        UpcomingTripKey(staticData.route.id, staticData.headsign, stopId)]
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
    fun isArrivalOnly() =
        upcomingTrips != null &&
            upcomingTrips
                .mapTo(mutableSetOf()) { it.isArrivalOnly() }
                .let { upcomingTripsArrivalOnly ->
                    upcomingTripsArrivalOnly.contains(true) &&
                        !upcomingTripsArrivalOnly.contains(false)
                }

    override fun compareTo(other: PatternsByHeadsign): Int =
        patterns.first().compareTo(other.patterns.first())

    sealed class Format {
        data object Loading : Format()

        data object None : Format()

        data class Some(val trips: List<FormatWithId>) : Format() {
            data class FormatWithId(val id: String, val format: UpcomingTrip.Format) {
                constructor(trip: UpcomingTrip, now: Instant) : this(trip.id, trip.format(now))
            }
        }

        data class NoService(val alert: Alert) : Format()
    }

    fun format(now: Instant): Format {
        if (this.upcomingTrips == null) return Format.Loading
        val tripsToShow =
            upcomingTrips
                .map { Format.Some.FormatWithId(it, now) }
                .filterNot {
                    it.format is UpcomingTrip.Format.Hidden ||
                        // API best practices call for hiding scheduled times on subway
                        (this.route.type.isSubway() && it.format is UpcomingTrip.Format.Schedule)
                }
                .take(2)
        if (tripsToShow.isEmpty()) {
            this.alertsHere?.firstOrNull()?.let {
                return Format.NoService(it)
            }
            return Format.None
        }
        return Format.Some(tripsToShow)
    }
}

/**
 * @property patternsByHeadsign [RoutePattern]s serving the stop grouped by headsign. The headsigns
 *   are listed in ascending order based on [RoutePattern.sortOrder]
 */
data class PatternsByStop(
    val route: Route,
    val stop: Stop,
    val patternsByHeadsign: List<PatternsByHeadsign>
) {
    val position = Position(longitude = stop.longitude, latitude = stop.latitude)

    constructor(
        staticData: NearbyStaticData.StopWithPatterns,
        upcomingTripsMap: UpcomingTripsMap?,
        cutoffTime: Instant,
        alerts: Collection<Alert>?,
    ) : this(
        staticData.route,
        staticData.stop,
        staticData.patternsByHeadsign
            .map { PatternsByHeadsign(it, upcomingTripsMap, staticData.allStopIds, alerts) }
            .filter { (it.isTypical() || it.isUpcomingBefore(cutoffTime)) && !it.isArrivalOnly() }
            .sorted()
    )

    @OptIn(ExperimentalTurfApi::class)
    fun distanceFrom(position: Position) = distance(position, this.position)
}

/**
 * @property patternsByStop A list of route patterns grouped by the station or stop that they serve.
 */
data class StopAssociatedRoute(
    val route: Route,
    val patternsByStop: List<PatternsByStop>,
) {
    constructor(
        staticData: NearbyStaticData.RouteWithStops,
        upcomingTripsMap: UpcomingTripsMap?,
        cutoffTime: Instant,
        sortByDistanceFrom: Position,
        alerts: Collection<Alert>?,
    ) : this(
        staticData.route,
        staticData.patternsByStop
            .map { PatternsByStop(it, upcomingTripsMap, cutoffTime, alerts) }
            .filterNot { it.patternsByHeadsign.isEmpty() }
            .sortedWith(
                compareBy(
                    { it.distanceFrom(sortByDistanceFrom) },
                    { it.patternsByHeadsign.first() }
                )
            )
    )

    @OptIn(ExperimentalTurfApi::class)
    fun distanceFrom(position: Position) = distance(position, patternsByStop.first().position)
}

/**
 * Attaches [schedules] and [predictions] to the route, stop, and headsign to which they apply.
 * Removes non-typical route patterns which are not predicted within 90 minutes of [filterAtTime].
 * Sorts routes by subway first then nearest stop, stops by distance, and headsigns by route pattern
 * sort order.
 */
fun NearbyStaticData.withRealtimeInfo(
    sortByDistanceFrom: Position,
    schedules: ScheduleResponse?,
    predictions: PredictionsStreamDataResponse?,
    alerts: AlertsStreamDataResponse?,
    filterAtTime: Instant
): List<StopAssociatedRoute> {
    // add predictions and apply filtering
    val schedulesMap =
        schedules?.let { scheduleData ->
            scheduleData.schedules.groupBy { schedule ->
                val trip = scheduleData.trips.getValue(schedule.tripId)
                UpcomingTripKey(schedule.routeId, trip.headsign, schedule.stopId)
            }
        }
    val predictionsMap =
        predictions?.let { streamData ->
            streamData.predictions.values.groupBy { prediction ->
                val trip = streamData.trips.getValue(prediction.tripId)
                UpcomingTripKey(prediction.routeId, trip.headsign, prediction.stopId)
            }
        }
    val upcomingTripsByHeadsignAndStop =
        if (schedulesMap != null || predictionsMap != null) {
            ((schedulesMap?.keys ?: emptySet()) + (predictionsMap?.keys ?: emptySet()))
                .associateWith { upcomingTripKey ->
                    val schedulesHere = schedulesMap?.get(upcomingTripKey)
                    val predictionsHere = predictionsMap?.get(upcomingTripKey)
                    UpcomingTrip.tripsFromData(
                        schedulesHere ?: emptyList(),
                        predictionsHere ?: emptyList(),
                        predictions?.vehicles ?: emptyMap()
                    )
                }
        } else {
            null
        }
    val cutoffTime = filterAtTime.plus(90.minutes)

    val activeRelevantAlerts =
        alerts?.alerts?.values?.filter {
            it.isActive(filterAtTime) && Alert.serviceDisruptionEffects.contains(it.effect)
        }

    return data
        .map {
            StopAssociatedRoute(
                it,
                upcomingTripsByHeadsignAndStop,
                cutoffTime,
                sortByDistanceFrom,
                activeRelevantAlerts
            )
        }
        .filterNot { it.patternsByStop.isEmpty() }
        .sortedWith(compareBy({ it.distanceFrom(sortByDistanceFrom) }, { it.route }))
        .sortedWith(compareBy(Route.subwayFirstComparator) { it.route })
}
