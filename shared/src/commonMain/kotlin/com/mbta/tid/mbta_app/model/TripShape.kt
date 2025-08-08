package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.ShapeWithStops
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class TripShape
internal constructor(@SerialName("shape_with_stops") internal val shapeWithStops: ShapeWithStops)
