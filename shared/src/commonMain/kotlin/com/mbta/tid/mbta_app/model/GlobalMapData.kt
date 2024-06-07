package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.datetime.Instant

data class MapStop(
    val stop: Stop,
    val routes: Map<MapStopRoute, List<Route>>,
    val routeTypes: List<MapStopRoute>,
    val isTerminal: Boolean,
    val alerts: Map<MapStopRoute, StopAlertState>?
)

data class GlobalMapData(
    val mapStops: Map<String, MapStop>,
    val alertsByStop: Map<String, AlertAssociatedStop>?
) {
    companion object {
        /*
        Only stops without a parent stop (stations and isolated stops) are returned in the result.
        Each AlertAssociatedStop will have entries in childAlerts if there are any active alerts on
        their children, but those child alerts aren't included in the map returned by this function.
         */
        fun getAlertsByStop(
            globalData: GlobalResponse,
            alerts: AlertsStreamDataResponse?,
            filterAtTime: Instant
        ): Map<String, AlertAssociatedStop>? {
            val activeAlerts =
                alerts?.alerts?.values?.filter { it.isActive(filterAtTime) } ?: return null
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
                            generateAlertingStopFor(stop, alertsByStop, globalData)
                        } else {
                            null
                        }
                    }
                    .associateBy { it.stop.id }

            return alertingStopsById
        }

        private fun generateAlertingStopFor(
            possibleStop: Stop?,
            alertsByStop: Map<String, Set<Alert>>,
            globalData: GlobalResponse
        ): AlertAssociatedStop? {
            val stop = possibleStop ?: return null

            val alertingStop =
                AlertAssociatedStop(stop = stop, alertsByStop = alertsByStop, global = globalData)

            // Return null for any stops without alerts or child alerts
            if (alertingStop.relevantAlerts.isEmpty() && alertingStop.childAlerts.isEmpty()) {
                return null
            }
            return alertingStop
        }
    }

    constructor(
        globalData: GlobalResponse,
        alertsByStop: Map<String, AlertAssociatedStop>?
    ) : this(
        globalData.stops.values
            .map { stop ->
                val stopIdSet = (setOf(stop.id) + stop.childStopIds)
                val patterns =
                    stopIdSet
                        .flatMap { stopId -> globalData.patternIdsByStop[stopId] ?: listOf() }
                        .mapNotNull { globalData.routePatterns[it] }
                val typicalPatterns =
                    patterns.filter {
                        !it.routeId.startsWith("Shuttle-") &&
                            (it.typicality == RoutePattern.Typicality.Typical ||
                                it.typicality == RoutePattern.Typicality.CanonicalOnly)
                    }

                val isTerminal =
                    typicalPatterns.any { pattern ->
                        val route = globalData.routes[pattern.routeId]
                        if (route == null || route.type == RouteType.BUS) {
                            // Don't mark bus terminals, only rail and ferry
                            return@any false
                        }
                        val trip =
                            globalData.trips[pattern.representativeTripId] ?: return@any false
                        val tripIds = trip.stopIds ?: listOf()
                        if (tripIds.size < 2) {
                            return@any false
                        }
                        return@any setOf(tripIds.first(), tripIds.last())
                            .intersect(stopIdSet)
                            .isNotEmpty()
                    }

                val allRoutes =
                    typicalPatterns
                        .mapNotNull { globalData.routes[it.routeId] }
                        .toSet()
                        .sortedBy { it.sortOrder }

                val mapRouteList = mutableListOf<MapStopRoute>()
                val categorizedRoutes = mutableMapOf<MapStopRoute, List<Route>>()

                for (route in allRoutes) {
                    val category = MapStopRoute.matching(route) ?: continue
                    if (!mapRouteList.contains(category)) {
                        mapRouteList += category
                    }
                    categorizedRoutes[category] = (categorizedRoutes[category] ?: listOf()) + route
                }

                var categorizedAlerts: Map<MapStopRoute, StopAlertState>? = null
                if (alertsByStop != null) {
                    val alertsHere = alertsByStop[stop.id]
                    categorizedAlerts = alertsHere?.serviceStatus
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
                    MapStop(
                        stop = stop,
                        routes = categorizedRoutes,
                        routeTypes = mapRouteList,
                        isTerminal = isTerminal,
                        alerts = categorizedAlerts
                    )
            }
            .toMap(),
        alertsByStop
    )
}
