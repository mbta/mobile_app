package com.mbta.tid.mbta_app.android

import kotlinx.serialization.Serializable

object SheetRoutes {
    @Serializable object NearbyTransit

    @Serializable
    data class StopDetails(
        val stopId: String,
        val filterRouteId: String?,
        val filterDirectionId: Int?
    )
}
