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
}
