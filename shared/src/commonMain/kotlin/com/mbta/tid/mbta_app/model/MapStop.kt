package com.mbta.tid.mbta_app.model

data class MapStop(
    val stop: Stop,
    val routes: Map<MapStopRoute, List<Route>>,
    val routeTypes: List<MapStopRoute>
)
