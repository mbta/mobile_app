package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MapFriendlyRouteShape(
    @SerialName("source_route_pattern_id") val sourceRoutePatternId: String,
    @SerialName("source_route_id") val sourceRouteId: String,
    @SerialName("route_segments") val routeSegments: List<RouteSegment>,
    val shape: Shape
)
