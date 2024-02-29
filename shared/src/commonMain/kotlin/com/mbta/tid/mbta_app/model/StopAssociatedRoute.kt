package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import io.github.dellisd.spatialk.geojson.Position
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi
import io.github.dellisd.spatialk.turf.distance
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Instant

data class PredictionWithVehicle
@DefaultArgumentInterop.Enabled
constructor(val prediction: Prediction, val vehicle: Vehicle? = null) :
    Comparable<PredictionWithVehicle> {
    override fun compareTo(other: PredictionWithVehicle) = prediction.compareTo(other.prediction)

    fun format(now: Instant) = prediction.format(now, vehicle)
}

data class PatternAndStop(val patternId: String, val stopId: String)

typealias PredictionsByPatternAndStop = Map<PatternAndStop, List<PredictionWithVehicle>>

/**
 * @property patterns [RoutePattern] listed in ascending order based on [RoutePattern.sortOrder]
 * @property predictions Every [Prediction] for the [Stop] in the containing [PatternsByStop] for
 *   any of these [patterns]
 */
data class PatternsByHeadsign(
    val headsign: String,
    val patterns: List<RoutePattern>,
    val predictions: List<PredictionWithVehicle>? = null
) : Comparable<PatternsByHeadsign> {
    constructor(
        staticData: NearbyStaticData.HeadsignWithPatterns,
        predictionsByPatternAndStop: PredictionsByPatternAndStop?,
        stopIds: Set<String>
    ) : this(
        staticData.headsign,
        staticData.patterns,
        if (predictionsByPatternAndStop != null) {
            staticData.patterns
                .flatMap { routePattern ->
                    stopIds
                        .mapNotNull { stopId ->
                            predictionsByPatternAndStop[PatternAndStop(routePattern.id, stopId)]
                        }
                        .flatten()
                }
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
     * Checks if a prediction exists before the given cutoff time.
     *
     * If [predictions] are unavailable (i.e. null), returns true, since nothing should be hidden
     * until data is available.
     */
    fun isPredictedBefore(cutoffTime: Instant) =
        predictions?.any {
            val predictionTime = it.prediction.predictionTime
            predictionTime != null && predictionTime < cutoffTime
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
        predictionsByPatternAndStop: PredictionsByPatternAndStop?,
        cutoffTime: Instant
    ) : this(
        staticData.stop,
        staticData.patternsByHeadsign
            .map {
                PatternsByHeadsign(it, predictionsByPatternAndStop, stopIds = staticData.allStopIds)
            }
            .filter { it.isTypical() || it.isPredictedBefore(cutoffTime) }
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
        predictionsByPatternAndStop: PredictionsByPatternAndStop?,
        cutoffTime: Instant,
        sortByDistanceFrom: Position
    ) : this(
        staticData.route,
        staticData.patternsByStop
            .map { PatternsByStop(it, predictionsByPatternAndStop, cutoffTime) }
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
    val predictionsByPatternAndStop =
        predictions?.let { streamData ->
            streamData.predictions.values.groupBy(
                { prediction ->
                    val trip = streamData.trips.getValue(prediction.tripId)
                    PatternAndStop(trip.routePatternId, prediction.stopId)
                },
                { prediction ->
                    PredictionWithVehicle(prediction, streamData.vehicles[prediction.vehicleId])
                }
            )
        }
    val cutoffTime = filterAtTime.plus(90.minutes)

    return data
        .map {
            StopAssociatedRoute(it, predictionsByPatternAndStop, cutoffTime, sortByDistanceFrom)
        }
        .filterNot { it.patternsByStop.isEmpty() }
        .sortedWith(compareBy({ it.distanceFrom(sortByDistanceFrom) }, { it.route }))
}
