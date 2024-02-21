package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.StopAndRoutePatternResponse

/**
 * @property patterns [RoutePattern] listed in ascending order based on [RoutePattern.sortOrder]
 * @property predictions Every [Prediction] for the [Stop] in the containing [PatternsByStop] for
 *   any of these [patterns]
 */
data class PatternsByHeadsign(
    val headsign: String,
    val patterns: List<RoutePattern>,
    val predictions: List<Prediction>? = null
)

/**
 * @property patternsByHeadsign [RoutePattern]s serving the stop grouped by headsign. The headsigns
 *   are listed in ascending order based on [RoutePattern.sortOrder]
 */
data class PatternsByStop(val stop: Stop, val patternsByHeadsign: List<PatternsByHeadsign>)

/**
 * @property patternsByStop A list of route patterns grouped by the station or stop that they serve.
 */
data class StopAssociatedRoute(
    val route: Route,
    val patternsByStop: List<PatternsByStop>,
)

/**
 * Aggregate stops and the patterns that serve them by route. Preserves the sort order of the stops
 * received by the server in [StopAndRoutePatternResponse.stops]
 */
@DefaultArgumentInterop.Enabled
fun StopAndRoutePatternResponse.byRouteAndStop(
    predictions: List<Prediction>? = null
): List<StopAssociatedRoute> {
    val hasPredictions = predictions != null
    val predictionsByPatternAndStop = predictions?.groupBy { it.trip.routePatternId to it.stopId }

    val routePatternsUsed = mutableSetOf<String>()

    val patternsByRouteAndStop =
        mutableMapOf<Route, MutableMap<Stop, MutableList<Pair<RoutePattern, List<Prediction>?>>>>()

    stops.forEach { stop ->
        val newPatternIds =
            patternIdsByStop
                .getOrElse(stop.id) { emptyList() }
                .filter { !routePatternsUsed.contains(it) }
        routePatternsUsed.addAll(newPatternIds)

        val newPatternsByRoute =
            newPatternIds
                .map { patternId ->
                    val routePattern = routePatterns.getValue(patternId)
                    routePattern to
                        if (hasPredictions) {
                            predictionsByPatternAndStop?.get(routePattern.id to stop.id)
                                ?: emptyList()
                        } else {
                            null
                        }
                }
                .sortedBy { (routePattern, _) -> routePattern.sortOrder }
                .groupBy { (routePattern, _) -> routePattern.routeId }

        newPatternsByRoute.forEach { (routeId, routePatterns) ->
            val stopKey = stop.parentStation ?: stop
            val routeStops =
                patternsByRouteAndStop.getOrPut(routes.getValue(routeId)) { mutableMapOf() }
            val patternsForStop = routeStops.getOrPut(stopKey) { mutableListOf() }
            patternsForStop += routePatterns
        }
    }

    return patternsByRouteAndStop.map { (route, patternsByStop) ->
        StopAssociatedRoute(
            route = route,
            patternsByStop =
                patternsByStop.map { (stop, patterns) ->
                    PatternsByStop(
                        stop = stop,
                        patternsByHeadsign =
                            patterns
                                .groupBy { (routePattern, _) ->
                                    routePattern.representativeTrip!!.headsign
                                }
                                .map { (headsign, routePatternsWithPredictions) ->
                                    val (routePatterns, eachPredictions) =
                                        routePatternsWithPredictions.unzip()
                                    val allPredictionsHere =
                                        if (hasPredictions) {
                                            eachPredictions.filterNotNull().flatten().sorted()
                                        } else {
                                            null
                                        }
                                    PatternsByHeadsign(headsign, routePatterns, allPredictionsHere)
                                }
                    )
                }
        )
    }
}
