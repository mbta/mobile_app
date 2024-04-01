package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteSegment(
    val id: String,
    @SerialName("first_stop") val firstStop: Stop,
    @SerialName("last_stop") val lastStop: Stop
)
