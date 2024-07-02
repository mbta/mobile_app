package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class TripShapeResponse {
    @Serializable
    @SerialName("unknown")
    data class NotFound(val message: String) : TripShapeResponse()

    @Serializable
    @SerialName("single_shape")
    data class TripShape(
        @SerialName("direction_id") val directionId: Int,
        @SerialName("route_id") val routeId: String,
        @SerialName("route_pattern_id") val routePatternId: String,
        @SerialName("shape") val shape: Shape?,
        @SerialName("stop_ids") val stopIds: List<String>
    ) : TripShapeResponse()
}
