package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TripShapeResponse(
    @SerialName("map_friendly_route_shapes") val mapFriendlyRouteShape: SegmentedRouteShape
)
