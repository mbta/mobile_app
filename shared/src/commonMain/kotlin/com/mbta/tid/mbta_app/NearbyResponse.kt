package com.mbta.tid.mbta_app

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NearbyResponse(
    val stops: List<Stop>,
    @SerialName("route_patterns") val routePatterns: Map<String, RoutePattern>,
    @SerialName("pattern_ids_by_stop") val patternIdsByStop: Map<String, List<String>>,
) {
    fun byRouteAndStop(): List<NearbyRoute> {
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
                nearbyPatterns =
                    patternsByStop.map { (stop, patterns) ->
                        NearbyPatternsByStop(stop = stop, routePatterns = patterns)
                    }
            )
        }
    }
}
