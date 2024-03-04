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
        patternIdsByStop: Map<String, List<String>>,
        parentStops: Map<String, Stop>? = null,
    ) : this(
        parentStops,
        patternIdsByStop,
        objects.routes,
        objects.routePatterns,
        objects.stops.values.filter {
            if (parentStops != null) {
                !parentStops.containsKey(it.id)
            } else {
                true
            }
        },
        objects.trips
    )
}
