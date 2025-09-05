package com.mbta.tid.mbta_app.model

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName
import kotlinx.serialization.Serializable

@Serializable
public data class Favorites(val routeStopDirection: Set<RouteStopDirection>? = null) {
    @OptIn(ExperimentalObjCName::class)
    public fun isFavorite(@ObjCName(swiftName = "_") rsd: RouteStopDirection): Boolean =
        routeStopDirection?.contains(rsd) ?: false
}

@Serializable
public data class RouteStopDirection(val route: String, val stop: String, val direction: Int)
