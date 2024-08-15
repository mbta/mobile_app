package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.ShapeWithStops
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TripShape(@SerialName("shape_with_stops") val shapeWithStops: ShapeWithStops)
