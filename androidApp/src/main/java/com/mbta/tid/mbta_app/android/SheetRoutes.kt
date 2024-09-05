package com.mbta.tid.mbta_app.android

import kotlinx.serialization.Serializable

sealed class SheetRoutes {
    @Serializable data object NearbyTransit : SheetRoutes()

    @Serializable
    data class StopDetails(
        val stopId: String,
        val filterRouteId: String?,
        val filterDirectionId: Int?
    ) : SheetRoutes()
}
