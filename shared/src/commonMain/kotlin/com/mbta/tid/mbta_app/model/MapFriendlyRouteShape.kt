package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MapFriendlyRouteShape(
    val color: String,
    @SerialName("route_pattern_id") val routePatternId: String,
    @SerialName("route_segments") val routeSegments: List<RouteSegment>
)
