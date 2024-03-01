package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.Trip
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StopAndRoutePatternResponse(
    @SerialName("parent_stops") val parentStops: Map<String, Stop>? = null,
    @SerialName("pattern_ids_by_stop") val patternIdsByStop: Map<String, List<String>>,
    val routes: Map<String, Route>,
    @SerialName("route_patterns") val routePatterns: Map<String, RoutePattern>,
    val stops: List<Stop>,
    val trips: Map<String, Trip>,
) {
    constructor(
        objects: ObjectCollectionBuilder,
        patternIdsByStop: Map<String, List<String>>
    ) : this(
        // assume all existing stops with no patterns are parents
        objects.stops.filter { (stopId, _) -> !patternIdsByStop.containsKey(stopId) },
        patternIdsByStop,
        objects.routes,
        objects.routePatterns,
        // stops with patterns would've been included directly by the backend
        objects.stops.values.filter { patternIdsByStop.containsKey(it.id) },
        objects.trips
    )
}
