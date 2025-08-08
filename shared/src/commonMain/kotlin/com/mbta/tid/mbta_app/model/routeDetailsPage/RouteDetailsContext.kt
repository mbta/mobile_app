package com.mbta.tid.mbta_app.model.routeDetailsPage

import kotlinx.serialization.Serializable

@Serializable
public sealed class RouteDetailsContext {
    @Serializable public data object Favorites : RouteDetailsContext()

    @Serializable public data object Details : RouteDetailsContext()
}
