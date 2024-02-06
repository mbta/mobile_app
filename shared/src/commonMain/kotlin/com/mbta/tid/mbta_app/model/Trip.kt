package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.GetReferenceIdSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Trip(
    val id: String,
    @Serializable(with = GetReferenceIdSerializer::class)
    @SerialName("route_pattern")
    val routePatternId: String?,
    val stops: List<Stop>?
)
