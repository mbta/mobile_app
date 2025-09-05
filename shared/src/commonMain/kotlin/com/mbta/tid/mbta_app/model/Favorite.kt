package com.mbta.tid.mbta_app.model

import kotlinx.serialization.Serializable

@Serializable
public data class Favorites(val routeStopDirection: Set<RouteStopDirection>? = null) {
    public fun isFavorite(rsd: RouteStopDirection): Boolean =
        routeStopDirection?.contains(rsd) ?: false
}

@Serializable
public data class RouteStopDirection(val route: String, val stop: String, val direction: Int)
