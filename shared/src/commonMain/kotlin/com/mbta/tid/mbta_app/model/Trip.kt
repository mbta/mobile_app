package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Trip(
    override val id: String,
    @SerialName("direction_id") val directionId: Int,
    val headsign: String,
    @SerialName("route_pattern_id") val routePatternId: String? = null,
    @SerialName("shape_id") val shapeId: String? = null,
    @SerialName("stop_ids") val stopIds: List<String>? = null
) : BackendObject
