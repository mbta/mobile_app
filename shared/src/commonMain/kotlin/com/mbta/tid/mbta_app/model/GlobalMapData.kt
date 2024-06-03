package com.mbta.tid.mbta_app.model

data class MapStop(
    val stop: Stop,
    val routes: Map<MapStopRoute, List<Route>>,
    val routeTypes: List<MapStopRoute>
)

data class GlobalMapData(val mapStops: Map<String, MapStop>) {
    constructor(
        globalStatic: GlobalStaticData
    ) : this(
        globalStatic.globalData.stops.values
            .map { stop ->
                val globalData = globalStatic.globalData
                val patterns =
                    (listOf(stop.id) + stop.childStopIds)
                        .flatMap { stopId -> globalData.patternIdsByStop[stopId] ?: listOf() }
                        .mapNotNull { globalData.routePatterns[it] }

                val allRoutes =
                    patterns
                        .filter {
                            it.typicality == RoutePattern.Typicality.Typical ||
                                it.typicality == RoutePattern.Typicality.CanonicalOnly
                        }
                        .mapNotNull { globalData.routes[it.routeId] }
                        .toSet()
                        .sortedBy { it.sortOrder }

                val mapRouteList = mutableListOf<MapStopRoute>()
                val categorizedRoutes = mutableMapOf<MapStopRoute, List<Route>>()

                for (route in allRoutes) {
                    val category = MapStopRoute.matching(route) ?: continue
                    if (route.id.startsWith("Shuttle-")) {
                        continue
                    }
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
                    MapStop(stop = stop, routes = categorizedRoutes, routeTypes = mapRouteList)
            }
            .toMap()
    )
}
