package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant

public data class MapStop(
    val stop: Stop,
    val routes: Map<MapStopRoute, List<Route>>,
    val routeTypes: List<MapStopRoute>,
    val routeDirections: Map<Route.Id, Set<Int>>,
    val isTerminal: Boolean,
    val alerts: Map<MapStopRoute, StopAlertState>?,
)

public data class GlobalMapData(
    internal val mapStops: Map<String, MapStop>,
    internal val alertsByStop: Map<String, AlertAssociatedStop>?,
) {
    internal companion object {
        /*
        Only stops without a parent stop (stations and isolated stops) are returned in the result.
        Each AlertAssociatedStop will have entries in childAlerts if there are any active alerts on
        their children, but those child alerts aren't included in the map returned by this function.
         */
        internal fun getAlertsByStop(
            globalData: GlobalResponse,
            alerts: AlertsStreamDataResponse?,
            filterAtTime: EasternTimeInstant,
        ): Map<String, AlertAssociatedStop>? {
            val activeAlerts =
                alerts?.alerts?.values?.filter { it.isActive(filterAtTime) } ?: return null
            val alertsByStop: MutableMap<String, MutableSet<Alert>> = mutableMapOf()
            val nullStopAlerts: MutableSet<Alert> = mutableSetOf()
            activeAlerts.forEach { alert ->
                alert.informedEntity.forEach {
                    if (it.stop != null) {
                        val alertSet = alertsByStop.getOrPut(it.stop) { mutableSetOf() }
                        alertSet.add(alert)
                    } else {
                        nullStopAlerts.add(alert)
                    }
                }
            }

            // Only parent stations with alerts (including on any of their children) are returned
            val alertingStopsById: Map<String, AlertAssociatedStop> =
                globalData.stops.values
                    .mapNotNull {
                        return@mapNotNull if (it.parentStationId != null) {
                            null
                        } else {
                            generateAlertingStopFor(
                                it,
                                alertsByStop,
                                nullStopAlerts,
                                filterAtTime,
                                globalData,
                            )
                        }
                    }
                    .associateBy { it.stop.id }

            return alertingStopsById
        }

        private fun generateAlertingStopFor(
            stop: Stop,
            alertsByStop: Map<String, Set<Alert>>,
            nullStopAlerts: Set<Alert>,
            now: EasternTimeInstant,
            globalData: GlobalResponse,
        ): AlertAssociatedStop? {
            val alertingStop =
                AlertAssociatedStop(
                    stop = stop,
                    alertsByStop = alertsByStop,
                    nullStopAlerts = nullStopAlerts,
                    now = now,
                    global = globalData,
                )

            // Return null for any stops without alerts or child alerts
            if (alertingStop.relevantAlerts.isEmpty() && alertingStop.childAlerts.isEmpty()) {
                return null
            }
            return alertingStop
        }
    }

    public constructor(
        globalData: GlobalResponse,
        alerts: AlertsStreamDataResponse?,
        filterAtTime: EasternTimeInstant,
    ) : this(globalData, getAlertsByStop(globalData, alerts, filterAtTime))

    internal constructor(
        globalData: GlobalResponse,
        alertsByStop: Map<String, AlertAssociatedStop>?,
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
                        !it.routeId.idText.startsWith("Shuttle-") &&
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

                val allRoutes = mutableSetOf<Route>()
                val routeDirections = mutableMapOf<Route.Id, MutableSet<Int>>()

                for (pattern in typicalPatterns.sorted()) {
                    routeDirections
                        .getOrPut(pattern.routeId, ::mutableSetOf)
                        .add(pattern.directionId)
                    val route = globalData.routes[pattern.routeId] ?: continue
                    allRoutes.add(route)
                }

                val mapRouteList = mutableListOf<MapStopRoute>()
                val categorizedRoutes = mutableMapOf<MapStopRoute, List<Route>>()

                for (route in allRoutes.sorted()) {
                    val category = MapStopRoute.matching(route) ?: continue
                    if (!mapRouteList.contains(category)) {
                        mapRouteList += category
                    }
                    categorizedRoutes[category] = (categorizedRoutes[category] ?: listOf()) + route
                }

                var categorizedAlerts: Map<MapStopRoute, StopAlertState>? = null
                if (alertsByStop != null) {
                    val alertsHere = alertsByStop[stop.id]
                    categorizedAlerts = alertsHere?.stateByRoute ?: emptyMap()
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
                        routeDirections = routeDirections,
                        isTerminal = isTerminal,
                        alerts = categorizedAlerts,
                    )
            }
            .toMap(),
        alertsByStop,
    )

    override fun toString(): String = "[GlobalMapData]"
}
