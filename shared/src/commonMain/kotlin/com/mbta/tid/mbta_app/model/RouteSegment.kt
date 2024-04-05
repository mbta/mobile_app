package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/**
 * A sequential chunk of stops on a route pattern that don't overlap with segments for other route
 * patterns. A segment may only intersect with other segments at the first and last stop.
 */
data class RouteSegment(
    val id: String,
    @SerialName("source_route_pattern_id") val sourceRoutePatternId: String,
    @SerialName("source_route_id") val sourceRouteId: String,
    @SerialName("stop_ids") val stopIds: List<String>,
    @SerialName("other_patterns_by_stop_id")
    val otherPatternsByStopId: Map<String, List<RoutePatternKey>>
) {
    @Serializable
    data class RoutePatternKey(
        @SerialName("route_id") val routeId: String,
        @SerialName("route_pattern_id") val routePatternId: String
    )
}
