package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.util.deepenTripleKey
import io.github.dellisd.spatialk.geojson.Position
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi
import io.github.dellisd.spatialk.turf.distance
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Instant

typealias UpcomingTripsByHeadsign = Map<String, List<UpcomingTrip>>

typealias UpcomingTripsByStop = Map<String, UpcomingTripsByHeadsign>

typealias UpcomingTripsByRoute = Map<String, UpcomingTripsByStop>

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
        upcomingTrips: List<UpcomingTrip>?,
    ) : this(staticData.headsign, staticData.patterns, upcomingTrips?.sorted())

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
     * If [upcomingTrips] are unavailable (i.e. null), returns true, since nothing should be hidden
     * until data is available.
     */
    fun isUpcomingBefore(cutoffTime: Instant) =
        upcomingTrips?.any {
            val tripTime = it.time
            tripTime != null && tripTime < cutoffTime
        }
            ?: true

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
        upcomingTripsAtStop: UpcomingTripsByHeadsign?,
        cutoffTime: Instant
    ) : this(
        staticData.stop,
        staticData.patternsByHeadsign
            .map {
                PatternsByHeadsign(it, upcomingTripsAtStop?.getOrElse(it.headsign) { emptyList() })
            }
            .filter { it.isTypical() || it.isUpcomingBefore(cutoffTime) }
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
        upcomingTripsOnRoute: UpcomingTripsByStop?,
        cutoffTime: Instant,
        sortByDistanceFrom: Position
    ) : this(
        staticData.route,
        staticData.patternsByStop
            .map { stopWithPatterns ->
                PatternsByStop(
                    stopWithPatterns,
                    if (upcomingTripsOnRoute != null) {
                        stopWithPatterns.allStopIds
                            .mapNotNull { upcomingTripsOnRoute[it] }
                            .reduce { a, b ->
                                (a.keys + b.keys).associateWith { headsign ->
                                    (a[headsign] ?: emptyList()) + (b[headsign] ?: emptyList())
                                }
                            }
                    } else {
                        null
                    },
                    cutoffTime
                )
            }
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
 * Sorts routes by nearest stop, stops by distance, and headsigns by route pattern sort order.
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
                Triple(schedule.routeId, schedule.stopId, trip.headsign)
            }
        }
    val predictionsMap =
        predictions?.let { streamData ->
            streamData.predictions.values.groupBy { prediction ->
                val trip = streamData.trips.getValue(prediction.tripId)
                Triple(prediction.routeId, prediction.stopId, trip.headsign)
            }
        }
    val upcomingTripsMap: UpcomingTripsByRoute? =
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
                .deepenTripleKey()
        } else {
            null
        }
    val cutoffTime = filterAtTime.plus(90.minutes)

    return data
        .map {
            StopAssociatedRoute(
                it,
                upcomingTripsMap?.get(it.route.id),
                cutoffTime,
                sortByDistanceFrom
            )
        }
        .filterNot { it.patternsByStop.isEmpty() }
        .sortedWith(compareBy({ it.distanceFrom(sortByDistanceFrom) }, { it.route }))
}
