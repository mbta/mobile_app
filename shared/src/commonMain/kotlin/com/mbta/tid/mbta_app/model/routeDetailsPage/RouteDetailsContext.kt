package com.mbta.tid.mbta_app.model.routeDetailsPage

import kotlinx.serialization.Serializable

@Serializable
sealed class RouteDetailsContext {
    @Serializable data object Favorites : RouteDetailsContext()

    @Serializable data object Details : RouteDetailsContext()
}
