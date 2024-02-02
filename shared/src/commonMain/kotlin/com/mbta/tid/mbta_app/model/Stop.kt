package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Stop(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    @SerialName("parent_station") val parentStation: Stop?
)
