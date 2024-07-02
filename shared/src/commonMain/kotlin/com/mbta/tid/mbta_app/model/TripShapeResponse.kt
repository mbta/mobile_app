package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.ShapeWithStops
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class TripShapeResponse {
    @Serializable
    @SerialName("unknown")
    data class NotFound(val message: String) : TripShapeResponse()

    @Serializable
    @SerialName("single_shape")
    data class TripShape(@SerialName("shape_with_stops") val shapeWithStops: ShapeWithStops) :
        TripShapeResponse()
}
