package com.mbta.tid.mbta_app

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoutePattern(
    val id: String,
    @SerialName("direction_id") val directionId: Int,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int,
    val route: Route?
)
