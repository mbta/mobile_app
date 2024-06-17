package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.Trip
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GlobalResponse(
    val lines: Map<String, Line>,
    @SerialName("pattern_ids_by_stop") val patternIdsByStop: Map<String, List<String>>,
    val routes: Map<String, Route>,
    @SerialName("route_patterns") val routePatterns: Map<String, RoutePattern>,
    val stops: Map<String, Stop>,
    val trips: Map<String, Trip>,
) {
    constructor(
        objects: ObjectCollectionBuilder,
        patternIdsByStop: Map<String, List<String>>,
    ) : this(
        objects.lines,
        patternIdsByStop,
        objects.routes,
        objects.routePatterns,
        objects.stops,
        objects.trips
    )
}
