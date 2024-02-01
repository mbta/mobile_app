package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.NearbyResponse

data class NearbyPatternsByStop(val stop: Stop, val routePatterns: List<RoutePattern>)

data class NearbyRoute(
    val route: Route,
    val patternsByStop: List<NearbyPatternsByStop>,
)

fun NearbyResponse.byRouteAndStop(): List<NearbyRoute> {
    val routePatternsUsed = mutableSetOf<String>()

    val patternsByRouteAndStop = mutableMapOf<Route, MutableMap<Stop, List<RoutePattern>>>()

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
            val routeStops = patternsByRouteAndStop.getOrElse(route) { mutableMapOf() }
            routeStops[stop] = routePatterns
            patternsByRouteAndStop[route] = routeStops
        }
    }

    return patternsByRouteAndStop.map { (route, patternsByStop) ->
        NearbyRoute(
            route = route,
            patternsByStop =
                patternsByStop.map { (stop, patterns) ->
                    NearbyPatternsByStop(stop = stop, routePatterns = patterns)
                }
        )
    }
}
