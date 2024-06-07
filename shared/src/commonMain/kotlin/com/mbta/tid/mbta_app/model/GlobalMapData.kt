package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse

data class MapStop(
    val stop: Stop,
    val routes: Map<MapStopRoute, List<Route>>,
    val routeTypes: List<MapStopRoute>,
    val isTerminal: Boolean
)

data class GlobalMapData(val mapStops: Map<String, MapStop>) {
    constructor(
        globalData: GlobalResponse
    ) : this(
        globalData.stops.values
            .map { stop ->
                val stopIdSet = (setOf(stop.id) + stop.childStopIds)
                val patterns =
                    stopIdSet
                        .flatMap { stopId -> globalData.patternIdsByStop[stopId] ?: listOf() }
                        .mapNotNull { globalData.routePatterns[it] }
                val typicalPatterns =
                    patterns.filter {
                        !it.routeId.startsWith("Shuttle-") &&
                            (it.typicality == RoutePattern.Typicality.Typical ||
                                it.typicality == RoutePattern.Typicality.CanonicalOnly)
                    }

                val isTerminal =
                    typicalPatterns.any { pattern ->
                        val route = globalData.routes[pattern.routeId]
                        if (route == null || route.type == RouteType.BUS) {
                            // Don't mark bus terminals, only rail and ferry
                            return@any false
                        }
                        val trip =
                            globalData.trips[pattern.representativeTripId] ?: return@any false
                        val tripIds = trip.stopIds ?: listOf()
                        if (tripIds.size < 2) {
                            return@any false
                        }
                        return@any setOf(tripIds.first(), tripIds.last())
                            .intersect(stopIdSet)
                            .isNotEmpty()
                    }

                val allRoutes =
                    typicalPatterns
                        .mapNotNull { globalData.routes[it.routeId] }
                        .toSet()
                        .sortedBy { it.sortOrder }

                val mapRouteList = mutableListOf<MapStopRoute>()
                val categorizedRoutes = mutableMapOf<MapStopRoute, List<Route>>()

                for (route in allRoutes) {
                    val category = MapStopRoute.matching(route) ?: continue
                    if (!mapRouteList.contains(category)) {
                        mapRouteList += category
                    }
                    categorizedRoutes[category] = (categorizedRoutes[category] ?: listOf()) + route
                }

                if (mapRouteList == listOf(MapStopRoute.SILVER, MapStopRoute.BUS)) {
                    mapRouteList.remove(MapStopRoute.SILVER)
                } else if (
                    mapRouteList == listOf(MapStopRoute.SILVER) &&
                        stop.locationType == LocationType.STOP
                ) {
                    mapRouteList[0] = MapStopRoute.BUS
                }

                return@map stop.id to
                    MapStop(
                        stop = stop,
                        routes = categorizedRoutes,
                        routeTypes = mapRouteList,
                        isTerminal = isTerminal
                    )
            }
            .toMap()
    )
}
