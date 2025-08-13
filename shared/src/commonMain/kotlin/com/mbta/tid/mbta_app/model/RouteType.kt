package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class RouteType {
    @SerialName("light_rail") LIGHT_RAIL,
    @SerialName("heavy_rail") HEAVY_RAIL,
    @SerialName("commuter_rail") COMMUTER_RAIL,
    @SerialName("bus") BUS,
    @SerialName("ferry") FERRY;

    internal fun isSubway(): Boolean = this === HEAVY_RAIL || this === LIGHT_RAIL

    internal val serialName: String
        get() = RouteType.serializer().descriptor.getElementName(this.ordinal)
}
