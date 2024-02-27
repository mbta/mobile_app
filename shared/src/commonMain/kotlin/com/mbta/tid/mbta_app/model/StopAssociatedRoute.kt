package com.mbta.tid.mbta_app.model

import io.github.dellisd.spatialk.geojson.Position
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi
import io.github.dellisd.spatialk.turf.distance
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Instant

/**
 * @property patterns [RoutePattern] listed in ascending order based on [RoutePattern.sortOrder]
 * @property predictions Every [Prediction] for the [Stop] in the containing [PatternsByStop] for
 *   any of these [patterns]
 */
data class PatternsByHeadsign(
    val headsign: String,
    val patterns: List<RoutePattern>,
    val predictions: List<Prediction>? = null
) : Comparable<PatternsByHeadsign> {
    constructor(
        staticData: NearbyStaticData.HeadsignWithPatterns,
        predictionsByPatternAndStop: Map<Pair<String?, String?>, List<Prediction>>?,
        stopIds: Set<String>
    ) : this(
        staticData.headsign,
        staticData.patterns,
        if (predictionsByPatternAndStop != null) {
            staticData.patterns
                .flatMap { routePattern ->
                    stopIds
                        .mapNotNull { stopId ->
                            predictionsByPatternAndStop[routePattern.id to stopId]
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
            val predictionTime = it.predictionTime
            predictionTime != null && predictionTime < cutoffTime
        }
            ?: true

    override fun compareTo(other: PatternsByHeadsign): Int =
        patterns.first().sortOrder.compareTo(other.patterns.first().sortOrder)
}

/**
 * @property patternsByHeadsign [RoutePattern]s serving the stop grouped by headsign. The headsigns
 *   are listed in ascending order based on [RoutePattern.sortOrder]
 */
data class PatternsByStop(val stop: Stop, val patternsByHeadsign: List<PatternsByHeadsign>) {
    val position = Position(longitude = stop.longitude, latitude = stop.latitude)

    constructor(
        staticData: NearbyStaticData.StopWithPatterns,
        predictionsByPatternAndStop: Map<Pair<String?, String?>, List<Prediction>>?,
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
        predictionsByPatternAndStop: Map<Pair<String?, String?>, List<Prediction>>?,
        cutoffTime: Instant,
        sortByDistanceFrom: Position
    ) : this(
        staticData.route,
        staticData.patternsByStop
            .map { PatternsByStop(it, predictionsByPatternAndStop, cutoffTime) }
            .filterNot { it.patternsByHeadsign.isEmpty() }
            .sortedBy { it.distanceFrom(sortByDistanceFrom) }
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
    predictions: List<Prediction>?,
    filterAtTime: Instant
): List<StopAssociatedRoute> {
    // add predictions and apply filtering
    val predictionsByPatternAndStop = predictions?.groupBy { it.trip.routePatternId to it.stopId }
    val cutoffTime = filterAtTime.plus(90.minutes)

    return data
        .map {
            StopAssociatedRoute(it, predictionsByPatternAndStop, cutoffTime, sortByDistanceFrom)
        }
        .filterNot { it.patternsByStop.isEmpty() }
        .sortedBy { it.distanceFrom(sortByDistanceFrom) }
}
