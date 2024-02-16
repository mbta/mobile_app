package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteResult(
    val id: String,
    val rank: Int,
    @SerialName("long_name") val longName: String,
    @SerialName("name") val shortName: String,
    @SerialName("route_type") val routeType: RouteType,
)
