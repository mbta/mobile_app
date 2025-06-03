package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.kdTree.KdTree
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Facility
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
    internal val facilities: Map<String, Facility>,
    internal val lines: Map<String, Line>,
    @SerialName("pattern_ids_by_stop") internal val patternIdsByStop: Map<String, List<String>>,
    internal val routes: Map<String, Route>,
    @SerialName("route_patterns") internal val routePatterns: Map<String, RoutePattern>,
    internal val stops: Map<String, Stop>,
    internal val trips: Map<String, Trip>,
) {
    constructor(
        objects: ObjectCollectionBuilder
    ) : this(
        objects.facilities,
        objects.lines,
        objects.routePatterns
            .flatMap {
                objects.trips[it.value.representativeTripId]?.stopIds?.map { stopId ->
                    stopId to it.key
                } ?: emptyList()
            }
            .groupBy({ it.first }, { it.second }),
        objects.routes,
        objects.routePatterns,
        objects.stops,
        objects.trips,
    )

    constructor(
        objects: ObjectCollectionBuilder,
        patternIdsByStop: Map<String, List<String>>,
    ) : this(
        objects.facilities,
        objects.lines,
        patternIdsByStop,
        objects.routes,
        objects.routePatterns,
        objects.stops,
        objects.trips,
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

    @Transient
    /** lines with their associated non-shuttle routes */
    internal val routesByLineId: Map<String, List<Route>> =
        routes.values.filter { it.lineId != null && !it.isShuttle }.groupBy { it.lineId!! }

    fun getFacility(facilityId: String?) = facilities[facilityId]

    fun getRoute(routeId: String?) = routes[routeId]

    fun getStop(stopId: String?) = stops[stopId]

    fun getLine(lineId: String?) =
        if (lineId != null) {
            lines[lineId]
        } else {
            null
        }

    fun getPatternsFor(stopId: String): List<RoutePattern> {
        val stop = stops[stopId] ?: return emptyList()
        val stopIds = stop.childStopIds + listOf(stopId)
        val patternIds = stopIds.flatMap { patternIdsByStop[it] ?: emptyList() }
        return patternIds.mapNotNull { routePatterns[it] }
    }

    fun getPatternsFor(stopId: String, routeId: String): List<RoutePattern> {
        return getPatternsFor(stopId).filter { it.routeId == routeId }
    }

    fun getTypicalRoutesFor(stopId: String): List<Route> {
        return getPatternsFor(stopId)
            .mapNotNull { routePattern ->
                if (routePattern.typicality != RoutePattern.Typicality.Typical) {
                    return@mapNotNull null
                }
                routes[routePattern.routeId]
            }
            .distinct()
    }

    fun getAlertAffectedStops(alert: Alert?, routes: List<Route>?): List<Stop>? {
        if (alert == null || routes == null) return null
        val routeEntities =
            routes.flatMap { route ->
                alert.matchingEntities { entity -> entity.satisfies { checkRoute(route.id) } }
            }
        val parentStops =
            routeEntities.mapNotNull { this.stops[it.stop]?.resolveParent(this.stops) }
        return parentStops.distinct()
    }
}
