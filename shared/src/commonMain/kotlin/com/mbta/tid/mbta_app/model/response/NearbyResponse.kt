package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.Stop
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NearbyResponse(
    val stops: List<Stop>,
    @SerialName("route_patterns") val routePatterns: Map<String, RoutePattern>,
    @SerialName("pattern_ids_by_stop") val patternIdsByStop: Map<String, List<String>>,
)
