package com.mbta.tid.mbta_app

data class NearbyPatternsByStop(val stop: Stop, val routePatterns: List<RoutePattern>)

data class NearbyRoute(
    val route: Route,
    val nearbyPatterns: List<NearbyPatternsByStop>,
)
