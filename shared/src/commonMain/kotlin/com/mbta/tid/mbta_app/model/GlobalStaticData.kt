package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.datetime.Instant

data class GlobalStaticData(val globalData: GlobalResponse) {
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
                relevantAlerts = alertsByStop[stop.id]?.toSet() ?: emptySet(),
                routePatterns = getRoutePatternsFor(stop.id),
                childStops =
                    stop.childStopIds
                        ?.mapNotNull { childId -> globalData.stops[childId] }
                        ?.associateBy { it.id }
                        ?: emptyMap(),
                childAlerts =
                    stop.childStopIds
                        ?.mapNotNull { childId ->
                            generateAlertingStopFor(globalData.stops[childId], alertsByStop)
                        }
                        ?.associateBy { it.stop.id }
                        ?: emptyMap()
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
