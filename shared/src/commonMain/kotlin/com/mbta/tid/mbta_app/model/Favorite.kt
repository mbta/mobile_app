package com.mbta.tid.mbta_app.model

import kotlinx.serialization.Serializable

@Serializable public data class Favorites(val routeStopDirection: Set<RouteStopDirection>? = null)

@Serializable
public data class RouteStopDirection(val route: String, val stop: String, val direction: Int)

/** Temporary class while we are supporting old pinned routes & enhanced favorites */
public sealed class FavoriteBridge {
    public data class Favorite(val routeStopDirection: RouteStopDirection) : FavoriteBridge()

    public data class Pinned(val routeId: String) : FavoriteBridge()
}

/** Temporary class while we are supporting old pinned routes & enhanced favorites */
public sealed class FavoriteUpdateBridge {
    public data class Favorites(
        val updatedValues: Map<RouteStopDirection, Boolean>,
        val defaultDirection: Int,
    ) : FavoriteUpdateBridge()

    public data class Pinned(val routeId: String) : FavoriteUpdateBridge()
}
