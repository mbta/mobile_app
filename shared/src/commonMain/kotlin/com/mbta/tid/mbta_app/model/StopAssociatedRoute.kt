package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.StopAndRoutePatternResponse

/** @property routePatterns A sorted list of [RoutePattern]s serving the stop */
data class PatternsByStop(val stop: Stop, val routePatterns: List<RoutePattern>)

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
fun StopAndRoutePatternResponse.byRouteAndStop(): List<StopAssociatedRoute> {
    val routePatternsUsed = mutableSetOf<String>()

    val patternsByRouteAndStop = mutableMapOf<Route, MutableMap<Stop, MutableList<RoutePattern>>>()

    stops.forEach { stop ->
        val newPatternIds =
            patternIdsByStop
                .getOrElse(stop.id) { emptyList() }
                .filter { !routePatternsUsed.contains(it) }
        routePatternsUsed.addAll(newPatternIds)

        val newPatternsByRoute =
            newPatternIds
                .mapNotNull { patternId -> routePatterns[patternId] }
                .sortedBy { it.sortOrder }
                .groupBy { it.route }

        newPatternsByRoute.forEach { (route, routePatterns) ->
            val stopKey = stop.parentStation ?: stop
            val routeStops = patternsByRouteAndStop.getOrPut(route) { mutableMapOf() }
            val patternsForStop = routeStops.getOrPut(stopKey) { mutableListOf() }
            patternsForStop += routePatterns
        }
    }

    return patternsByRouteAndStop.map { (route, patternsByStop) ->
        StopAssociatedRoute(
            route = route,
            patternsByStop =
                patternsByStop.map { (stop, patterns) ->
                    PatternsByStop(stop = stop, routePatterns = patterns)
                }
        )
    }
}
