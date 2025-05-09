package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.RouteCardData.LineOrRoute
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoutePattern(
    override val id: String,
    @SerialName("direction_id") val directionId: Int,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int,
    val typicality: Typicality?,
    @SerialName("representative_trip_id") val representativeTripId: String,
    @SerialName("route_id") val routeId: String,
) : Comparable<RoutePattern>, BackendObject {
    @Serializable
    enum class Typicality {
        @SerialName("typical") Typical,
        @SerialName("deviation") Deviation,
        @SerialName("atypical") Atypical,
        @SerialName("diversion") Diversion,
        @SerialName("canonical_only") CanonicalOnly,
    }

    /**
     * Checks if this pattern is [Typicality.Typical].
     *
     * If any typicality is unknown, the route should be shown, and so this will return true.
     */
    fun isTypical() = typicality == null || typicality == Typicality.Typical

    override fun compareTo(other: RoutePattern) = sortOrder.compareTo(other.sortOrder)

    data class PatternsForStop(
        val allPatterns: List<RoutePattern>,
        val patternsNotSeenAtEarlierStops: Set<String>,
    )

    companion object {

        /**
         * Return the map of LineOrRoute => Stop => Patterns that are served by the given [stopIds].
         * A stop is only included for a LineOrRoute if it has any patterns that haven't been seen
         * at an earlier stop for that LineOrRoute.
         */
        fun patternsGroupedByLineOrRouteAndStop(
            parentToAllStops: Map<Stop, Set<String>>,
            globalData: GlobalResponse,
        ): Map<LineOrRoute, Map<Stop, PatternsForStop>> {
            val usedPatternIds = mutableSetOf<String>()
            val patternsGrouped = mutableMapOf<LineOrRoute, MutableMap<Stop, PatternsForStop>>()

            globalData.run {
                parentToAllStops.forEach { (parentStop, allStopsForParent) ->
                    val patternsByRouteOrLine =
                        patternsByRouteOrLine(allStopsForParent, globalData)
                            // filter out a route if we've already seen all of its patterns
                            .filterNot { (_key, routePatterns) ->
                                usedPatternIds.containsAll(routePatterns.map { it.id }.toSet())
                            }

                    for ((lineOrRoute, routePatterns) in patternsByRouteOrLine) {
                        val routeStops = patternsGrouped.getOrPut(lineOrRoute) { mutableMapOf() }
                        val patternsNotSeenAtEarlierStops =
                            routePatterns.map { it.id }.toSet().minus(usedPatternIds)
                        routeStops.getOrPut(parentStop) {
                            PatternsForStop(
                                allPatterns = routePatterns,
                                patternsNotSeenAtEarlierStops = patternsNotSeenAtEarlierStops,
                            )
                        }
                        usedPatternIds.addAll(routePatterns.map { it.id })
                    }
                }
            }

            return patternsGrouped
        }

        private fun patternsByRouteOrLine(
            stopIds: Set<String>,
            globalData: GlobalResponse,
        ): Map<LineOrRoute, List<RoutePattern>> {

            val allPatternsAtStopWithRoute: List<Pair<Route, RoutePattern>> =
                stopIds.flatMap { stopId ->
                    val patternsIds = globalData.patternIdsByStop.getOrElse(stopId) { emptyList() }
                    patternsIds.mapNotNull { patternId ->
                        val pattern = globalData.routePatterns[patternId]
                        val route = pattern?.let { globalData.routes[it.routeId] }
                        if (route != null && pattern != null) {
                            Pair(route, pattern)
                        } else {
                            null
                        }
                    }
                }

            val patternsByRouteOrLine =
                allPatternsAtStopWithRoute.groupBy(
                    { (route, _pattern) ->
                        val line = route.lineId?.let { globalData.lines[it] }
                        if (line != null && !route.isShuttle && line.isGrouped) {
                            LineOrRoute.Line(
                                line,
                                routes =
                                    globalData.routesByLineId
                                        .getOrElse(line.id) { emptyList() }
                                        .toSet(),
                            )
                        } else LineOrRoute.Route(route)
                    },
                    { it.second },
                )

            return patternsByRouteOrLine
        }
    }
}
