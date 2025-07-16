package com.mbta.tid.mbta_app.model

import io.github.dellisd.spatialk.geojson.Position

object PatternSorting {
    private fun patternServiceBucket(leafData: RouteCardData.Leaf) =
        when {
            // showing either a trip or an alert
            leafData.hasMajorAlerts || leafData.upcomingTrips.isNotEmpty() -> 1
            // service ended
            leafData.hasSchedulesToday -> 2
            // no service today
            else -> 3
        }

    private fun pinnedRouteBucket(route: Route, pinnedRoutes: Set<String>) =
        when {
            pinnedRoutes.contains(route.id) || pinnedRoutes.contains(route.lineId) -> 1
            else -> 2
        }

    private fun subwayBucket(route: Route) =
        when {
            route.type.isSubway() -> 1
            else -> 2
        }

    fun compareRouteCards(
        pinnedRoutes: Set<String>,
        sortByDistanceFrom: Position?,
        context: RouteCardData.Context,
    ): Comparator<RouteCardData> =
        compareBy(
            { pinnedRouteBucket(it.lineOrRoute.sortRoute, pinnedRoutes) },
            { patternServiceBucket(it.stopData.first().data.first()) },
            if (context != RouteCardData.Context.Favorites) {
                { subwayBucket(it.lineOrRoute.sortRoute) }
            } else {
                { 0 }
            },
            if (sortByDistanceFrom != null) {
                { it.distanceFrom(sortByDistanceFrom) }
            } else {
                { 0 }
            },
            { it.lineOrRoute.sortRoute },
        )

    fun compareStopsOnRoute(
        sortByDistanceFrom: Position?
    ): Comparator<RouteCardData.RouteStopData> =
        compareBy(
            { patternServiceBucket(it.data.first()) },
            if (sortByDistanceFrom != null) {
                { it.stop.distanceFrom(sortByDistanceFrom) }
            } else {
                { 0 }
            },
        )

    fun compareLeavesAtStop(): Comparator<RouteCardData.Leaf> =
        compareBy({ patternServiceBucket(it) }, { it.directionId })
}
