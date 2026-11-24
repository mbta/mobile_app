package com.mbta.tid.mbta_app.model

public object StopOverviewUtils {
    /**
     * If the stop serves only 1 route in a single direction, returns a new filter for that route
     * and direction.
     */
    public fun autoRouteStopFilter(routeCardData: List<RouteCardData>?): RouteStopFilter? {
        val route = routeCardData?.singleOrNull() ?: return null
        val directions = route.stopData.flatMap { it.data }.map { it.direction.id }.toSet()
        if (directions.size != 1) {
            return null
        }
        val direction = directions.first()
        return RouteStopFilter(route.lineOrRoute.id, direction, autoFilter = true)
    }
}
