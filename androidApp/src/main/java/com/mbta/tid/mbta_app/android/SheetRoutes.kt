package com.mbta.tid.mbta_app.android

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SheetRoutes {
    @Serializable @SerialName("nearby") data object NearbyTransit : SheetRoutes()

    @Serializable
    @SerialName("stop_details")
    class StopDetails(val stopId: String, val filterRouteId: String?, val filterDirectionId: Int?) :
        SheetRoutes()
}
