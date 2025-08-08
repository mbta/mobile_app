package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class LocationType {
    @SerialName("stop") STOP,
    @SerialName("station") STATION,
    @SerialName("entrance_exit") ENTRANCE_EXIT,
    @SerialName("generic_node") GENERIC_NODE,
    @SerialName("boarding_area") BOARDING_AREA,
}
