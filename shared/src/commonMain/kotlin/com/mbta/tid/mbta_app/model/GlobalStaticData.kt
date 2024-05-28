package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.datetime.Instant

data class GlobalStaticData(val globalData: GlobalResponse, val mapStops: Map<String, MapStop>) {
    constructor(
        globalData: GlobalResponse
    ) : this(
        globalData,
        globalData.stops.values
            .map { stop ->
                val patterns =
                    (listOf(stop.id) + stop.childStopIds)
                        .flatMap { stopId -> globalData.patternIdsByStop[stopId] ?: listOf() }
                        .mapNotNull { globalData.routePatterns[it] }

                val allRoutes =
                    patterns
                        .filter {
                            it.typicality == RoutePattern.Typicality.Typical ||
                                it.typicality == RoutePattern.Typicality.CanonicalOnly
                        }
                        .mapNotNull { globalData.routes[it.routeId] }
                        .toSet()
                        .sortedBy { it.sortOrder }

                val mapRouteList = mutableListOf<MapStopRoute>()
                val categorizedRoutes = mutableMapOf<MapStopRoute, List<Route>>()

                for (route in allRoutes) {
                    val category = MapStopRoute.matching(route) ?: continue
                    if (route.id.startsWith("Shuttle-")) {
                        continue
                    }
                    if (!mapRouteList.contains(category)) {
                        mapRouteList += category
                    }
                    categorizedRoutes[category] = (categorizedRoutes[category] ?: listOf()) + route
                }

                if (mapRouteList == listOf(MapStopRoute.SILVER, MapStopRoute.BUS)) {
                    mapRouteList.remove(MapStopRoute.SILVER)
                } else if (
                    mapRouteList == listOf(MapStopRoute.SILVER) &&
                        stop.locationType == LocationType.STOP
                ) {
                    mapRouteList[0] = MapStopRoute.BUS
                }

                return@map stop.id to
                    MapStop(stop = stop, routes = categorizedRoutes, routeTypes = mapRouteList)
            }
            .toMap()
    )

    /*
    Only stops that don't have a parent stop (stations and isolated stops) are returned in the map.
    Each AlertAssociatedStop will have entries in childAlerts if there are any active alerts on
    their children, but those child alerts aren't included in the map returned by this function.
     */
    fun withRealtimeAlertsByStop(
        alerts: AlertsStreamDataResponse?,
        filterAtTime: Instant
    ): Map<String, AlertAssociatedStop>? {
        val activeAlerts =
            alerts?.alerts?.values?.filter { it.isActive(filterAtTime) } ?: return emptyMap()
        val alertsByStop: MutableMap<String, MutableSet<Alert>> = mutableMapOf()
        activeAlerts.forEach { alert ->
            alert.informedEntity.forEach {
                if (it.stop != null) {
                    val alertList = alertsByStop.getOrPut(it.stop) { mutableSetOf() }
                    alertList.add(alert)
                }
            }
        }

        // Only parent stations with alerts (including on any of their children) are returned
        val alertingStopsById: Map<String, AlertAssociatedStop> =
            globalData.stops.values
                .mapNotNull { stop ->
                    if (stop.parentStationId == null) {
                        generateAlertingStopFor(stop, alertsByStop)
                    } else {
                        null
                    }
                }
                .associateBy { it.stop.id }

        return alertingStopsById
    }

    private fun generateAlertingStopFor(
        possibleStop: Stop?,
        alertsByStop: Map<String, Set<Alert>>
    ): AlertAssociatedStop? {
        val stop = possibleStop ?: return null

        val alertingStop =
            AlertAssociatedStop(
                stop = stop,
                relevantAlerts = alertsByStop[stop.id]?.toList() ?: emptyList(),
                routePatterns = getRoutePatternsFor(stop.id),
                childStops =
                    stop.childStopIds
                        .mapNotNull { childId -> globalData.stops[childId] }
                        .associateBy { it.id },
                childAlerts =
                    stop.childStopIds
                        .mapNotNull { childId ->
                            generateAlertingStopFor(globalData.stops[childId], alertsByStop)
                        }
                        .associateBy { it.stop.id }
            )

        // Return null for any stops without alerts or child alerts
        if (alertingStop.relevantAlerts.isEmpty() && alertingStop.childAlerts.isEmpty()) {
            return null
        }
        return alertingStop
    }

    private fun getRoutePatternsFor(stopId: String): List<RoutePattern> {
        return globalData.patternIdsByStop
            .getOrElse(stopId) { listOf() }
            .mapNotNull { globalData.routePatterns[it] }
    }
}
