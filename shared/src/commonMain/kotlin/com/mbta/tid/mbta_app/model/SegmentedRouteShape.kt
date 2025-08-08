package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A route shape with segments of stops that don't overlap with other route shapes. Ideal for use
 * when drawing multiple lines on a map at once.
 *
 * @param sourceRoutePatternId the route pattern of the shape
 * @param sourceRouteId the route of the shape
 * @param directionId: the direction of the route pattern
 * @param shape The full shape for the source route pattern
 * @param routeSegments segments of stops that stopped at on the route pattern.
 */
@Serializable
public data class SegmentedRouteShape(
    @SerialName("source_route_pattern_id") val sourceRoutePatternId: String,
    @SerialName("source_route_id") val sourceRouteId: String,
    @SerialName("direction_id") val directionId: Int,
    @SerialName("route_segments") val routeSegments: List<RouteSegment>,
    val shape: Shape,
)
