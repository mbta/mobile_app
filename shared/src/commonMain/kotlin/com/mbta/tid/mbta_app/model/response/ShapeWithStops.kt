package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Shape
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShapeWithStops(
    @SerialName("direction_id") val directionId: Int,
    @SerialName("route_id") val routeId: String,
    @SerialName("route_pattern_id") val routePatternId: String,
    @SerialName("shape") val shape: Shape?,
    @SerialName("stop_ids") val stopIds: List<String>,
)
