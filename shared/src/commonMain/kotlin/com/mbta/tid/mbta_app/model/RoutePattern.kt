package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class RoutePattern
internal constructor(
    override val id: String,
    @SerialName("direction_id") val directionId: Int,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int,
    val typicality: Typicality?,
    @SerialName("representative_trip_id") val representativeTripId: String,
    @SerialName("route_id") val routeId: String,
) : Comparable<RoutePattern>, BackendObject {
    @Serializable
    public enum class Typicality {
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
    public fun isTypical(): Boolean = typicality == null || typicality == Typicality.Typical

    override fun compareTo(other: RoutePattern): Int = sortOrder.compareTo(other.sortOrder)

    internal data class PatternsForStop(
        val allPatterns: List<RoutePattern>,
        val patternsNotSeenAtEarlierStops: Set<String>,
    )

    internal companion object {

        /**
         * Return the map of LineOrRoute => Stop => Patterns that are served by the given [stopIds].
         * A stop is only included for a LineOrRoute if it has any patterns that haven't been seen
         * at an earlier stop for that LineOrRoute.
         */
        fun patternsGroupedByLineOrRouteAndStop(
            parentToAllStops: Map<Stop, Set<String>>,
            globalData: GlobalResponse,
            context: RouteCardData.Context,
            favorites: Set<RouteStopDirection>? = null,
        ): Map<LineOrRoute, Map<Stop, PatternsForStop>> {
            val usedPatternIds = mutableSetOf<String>()
            val patternsGrouped = mutableMapOf<LineOrRoute, MutableMap<Stop, PatternsForStop>>()

            val inFavorites = context == RouteCardData.Context.Favorites
            val favoriteRouteStops = favorites?.map { Pair(it.route, it.stop) }?.toSet()
            fun skipNonFavorite(lineOrRoute: LineOrRoute, stop: Stop) =
                inFavorites && favoriteRouteStops?.contains(Pair(lineOrRoute.id, stop.id)) == false
            fun rsd(lineOrRoute: LineOrRoute, stop: Stop, pattern: RoutePattern) =
                RouteStopDirection(lineOrRoute.id, stop.id, pattern.directionId)

            globalData.run {
                parentToAllStops.forEach { (parentStop, allStopsForParent) ->
                    val patternsByRouteOrLine =
                        patternsByRouteOrLine(allStopsForParent, globalData)
                            // filter out a route if we've already seen all of its patterns
                            .filterNot { (_key, routePatterns) ->
                                usedPatternIds.containsAll(routePatterns.map { it.id }.toSet())
                            }
                    for ((lineOrRoute, routePatterns) in patternsByRouteOrLine) {
                        if (skipNonFavorite(lineOrRoute, parentStop)) continue

                        val routeStops = patternsGrouped.getOrPut(lineOrRoute) { mutableMapOf() }
                        val patternsNotSeenAtEarlierStops =
                            routePatterns.map { it.id }.toSet().minus(usedPatternIds)

                        fun skipNonFavorite(pattern: RoutePattern): Boolean =
                            inFavorites &&
                                favorites?.contains(rsd(lineOrRoute, parentStop, pattern)) == false

                        routeStops.getOrPut(parentStop) {
                            PatternsForStop(
                                allPatterns =
                                    routePatterns.mapNotNull {
                                        if (skipNonFavorite(it)) null else it
                                    },
                                patternsNotSeenAtEarlierStops = patternsNotSeenAtEarlierStops,
                            )
                        }
                        // We need stops from the same route pattern to show up for favorites
                        if (!inFavorites) {
                            usedPatternIds.addAll(routePatterns.map { it.id })
                        }
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
