package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RouteType {
    @SerialName("light_rail") LIGHT_RAIL,
    @SerialName("heavy_rail") HEAVY_RAIL,
    @SerialName("commuter_rail") COMMUTER_RAIL,
    @SerialName("bus") BUS,
    @SerialName("ferry") FERRY;

    companion object {
        fun isSubway(routeType: RouteType) = routeType === HEAVY_RAIL || routeType === LIGHT_RAIL
    }
}
