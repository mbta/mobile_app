package com.mbta.tid.mbta_app

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NearbyResponse(
    val stops: List<Stop>,
    @SerialName("route_patterns") val routePatterns: Map<String, RoutePattern>,
    @SerialName("pattern_ids_by_stop") val patternIdsByStop: Map<String, List<String>>,
) {
    fun routePatternsByStop(): List<Pair<RoutePattern, Stop>> = buildList {
        val routePatternsUsed = mutableSetOf<String>()
        stops.forEach { stop ->
            val newPatternIds =
                patternIdsByStop
                    .getOrElse(stop.id) { emptyList() }
                    .filter { !routePatternsUsed.contains(it) }
            routePatternsUsed.addAll(newPatternIds)
            val newPatterns =
                newPatternIds
                    .mapNotNull { patternId -> routePatterns[patternId]?.let { Pair(it, stop) } }
                    .sortedBy { it.first.sortOrder }
            this@buildList.addAll(newPatterns)
        }
    }

    fun byRouteAndStop(): List<NearbyRoute> {
        val pairsByRoute = routePatternsByStop().groupBy { it.first.route }

        return pairsByRoute.map { entry ->
            val route = entry.key
            val patterns = entry.value

            val patternsByStop =
                patterns.groupBy({ it.second }, { it.first }).map { (stop, routePatterns) ->
                    NearbyPatternsByStop(stop, routePatterns)
                }
            NearbyRoute(route, patternsByStop)
        }
    }
}
