package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.kdTree.KdTree
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.Trip
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
        objects: ObjectCollectionBuilder
    ) : this(
        objects.lines,
        objects.routePatterns
            .flatMap {
                objects.trips[it.value.representativeTripId]?.stopIds?.map { stopId ->
                    stopId to it.key
                }
                    ?: emptyList()
            }
            .groupBy({ it.first }, { it.second }),
        objects.routes,
        objects.routePatterns,
        objects.stops,
        objects.trips
    )

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

    @Transient
    internal val leafStopsKdTree =
        KdTree(
            stops.values
                .filter {
                    it.locationType in setOf(LocationType.STOP, LocationType.STATION) &&
                        it.vehicleType != null
                }
                .map { it.id to it.position }
        )

    fun getLine(lineId: String?) =
        if (lineId != null) {
            lines[lineId]
        } else {
            null
        }

    fun getTypicalRoutesFor(stopId: String): List<Route> {
        val stop = stops[stopId] ?: return emptyList()
        val stopIds = stop.childStopIds + listOf(stopId)
        val patternIds = stopIds.flatMap { patternIdsByStop[it] ?: emptyList() }
        return patternIds
            .mapNotNull {
                val routePattern = routePatterns[it] ?: return@mapNotNull null
                if (routePattern.typicality != RoutePattern.Typicality.Typical) {
                    return@mapNotNull null
                }
                routes[routePattern.routeId]
            }
            .distinct()
    }
}
