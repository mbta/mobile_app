package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import io.github.dellisd.spatialk.geojson.Position
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi
import io.github.dellisd.spatialk.turf.distance
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Instant

data class HeadsignAndStop(val headsign: String, val stopId: String)

typealias UpcomingTripsByHeadsignAndStop = Map<HeadsignAndStop, List<UpcomingTrip>>

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
        upcomingTripsByHeadsignAndStop: UpcomingTripsByHeadsignAndStop?,
        stopIds: Set<String>
    ) : this(
        staticData.headsign,
        staticData.patterns,
        if (upcomingTripsByHeadsignAndStop != null) {
            stopIds
                .mapNotNull { stopId ->
                    upcomingTripsByHeadsignAndStop[HeadsignAndStop(staticData.headsign, stopId)]
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
        upcomingTripsByHeadsignAndStop: UpcomingTripsByHeadsignAndStop?,
        cutoffTime: Instant
    ) : this(
        staticData.stop,
        staticData.patternsByHeadsign
            .map {
                PatternsByHeadsign(
                    it,
                    upcomingTripsByHeadsignAndStop,
                    stopIds = staticData.allStopIds
                )
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
        upcomingTripsByHeadsignAndStop: UpcomingTripsByHeadsignAndStop?,
        cutoffTime: Instant,
        sortByDistanceFrom: Position
    ) : this(
        staticData.route,
        staticData.patternsByStop
            .map { PatternsByStop(it, upcomingTripsByHeadsignAndStop, cutoffTime) }
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
 * Attaches [predictions] to the route, stop, and headsign to which they apply. Removes non-typical
 * route patterns which are not predicted within 90 minutes of [filterAtTime]. Sorts routes by
 * nearest stop, stops by distance, and headsigns by route pattern sort order.
 */
fun NearbyStaticData.withRealtimeInfo(
    sortByDistanceFrom: Position,
    predictions: PredictionsStreamDataResponse?,
    filterAtTime: Instant
): List<StopAssociatedRoute> {
    // add predictions and apply filtering
    val predictionsByHeadsignAndStop =
        predictions?.let { streamData ->
            streamData.predictions.values.groupBy { prediction ->
                val trip = streamData.trips.getValue(prediction.tripId)
                HeadsignAndStop(trip.headsign, prediction.stopId)
            }
        }
    val upcomingTripsByHeadsignAndStop =
        if (predictionsByHeadsignAndStop != null) {
            predictionsByHeadsignAndStop.keys.associateWith { headsignAndStop ->
                val predictionsHere = predictionsByHeadsignAndStop[headsignAndStop]
                UpcomingTrip.tripsFromData(predictionsHere ?: emptyList(), predictions.vehicles)
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
}
