package com.mbta.tid.mbta_app.model

import kotlinx.serialization.Serializable

@Serializable data class Favorites(val routeStopDirection: Set<RouteStopDirection>? = null)

@Serializable
data class RouteStopDirection(val route: String, val stop: String, val direction: Int)

/** Temporary class while we are supporting old pinned routes & enhanced favorites */
sealed class FavoriteBridge {
    data class Favorite(val routeStopDirection: RouteStopDirection) : FavoriteBridge()

    data class Pinned(val routeId: String) : FavoriteBridge()
}

/** Temporary class while we are supporting old pinned routes & enhanced favorites */
sealed class FavoriteUpdateBridge {
    data class Favorites(
        val updatedValues: Map<RouteStopDirection, Boolean>,
        val defaultDirection: Int,
    ) : FavoriteUpdateBridge()

    data class Pinned(val routeId: String) : FavoriteUpdateBridge()
}
