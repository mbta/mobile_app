package com.mbta.tid.mbta_app.model

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
    val headsign: String,
    val patterns: List<RoutePattern>,
    val upcomingTrips: List<UpcomingTrip>? = null,
) : Comparable<PatternsByHeadsign> {
    constructor(
        staticData: NearbyStaticData.HeadsignWithPatterns,
        routeId: String,
        upcomingTripsMap: UpcomingTripsMap?,
        stopIds: Set<String>
    ) : this(
        staticData.headsign,
        staticData.patterns,
        if (upcomingTripsMap != null) {
            stopIds
                .mapNotNull { stopId ->
                    upcomingTripsMap[UpcomingTripKey(routeId, staticData.headsign, stopId)]
                }
                .flatten()
                .sorted()
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
}

/**
 * @property patternsByHeadsign [RoutePattern]s serving the stop grouped by headsign. The headsigns
 *   are listed in ascending order based on [RoutePattern.sortOrder]
 */
data class PatternsByStop(val stop: Stop, val patternsByHeadsign: List<PatternsByHeadsign>) {
    val position = Position(longitude = stop.longitude, latitude = stop.latitude)

    constructor(
        staticData: NearbyStaticData.StopWithPatterns,
        routeId: String,
        upcomingTripsMap: UpcomingTripsMap?,
        cutoffTime: Instant
    ) : this(
        staticData.stop,
        staticData.patternsByHeadsign
            .map {
                PatternsByHeadsign(it, routeId, upcomingTripsMap, stopIds = staticData.allStopIds)
            }
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
        sortByDistanceFrom: Position
    ) : this(
        staticData.route,
        staticData.patternsByStop
            .map { PatternsByStop(it, staticData.route.id, upcomingTripsMap, cutoffTime) }
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

    return data
        .map {
            StopAssociatedRoute(it, upcomingTripsByHeadsignAndStop, cutoffTime, sortByDistanceFrom)
        }
        .filterNot { it.patternsByStop.isEmpty() }
        .sortedWith(compareBy({ it.distanceFrom(sortByDistanceFrom) }, { it.route }))
        .sortedWith(compareBy(Route.subwayFirstComparator) { it.route })
}
