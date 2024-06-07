package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse

class AlertAssociatedStop(val stop: Stop) {
    var relevantAlerts: List<Alert> = mutableListOf()
    var serviceAlerts: List<Alert> = mutableListOf()
    var childAlerts: Map<String, AlertAssociatedStop> = mutableMapOf()
    var serviceStatus: Map<MapStopRoute, StopAlertState> = mutableMapOf()

    constructor(
        stop: Stop,
        alertsByStop: Map<String, Set<Alert>>,
        global: GlobalResponse
    ) : this(stop) {
        global.run {
            relevantAlerts = alertsByStop[stop.id]?.toList() ?: emptyList()
            serviceAlerts = getServiceAlerts(relevantAlerts)

            val childStops =
                stop.childStopIds.mapNotNull { childId -> stops[childId] }.associateBy { it.id }
            childAlerts =
                stop.childStopIds
                    .mapNotNull { childId ->
                        val child = stops[childId] ?: return@mapNotNull null
                        val childAlert = AlertAssociatedStop(child, alertsByStop, this)
                        if (childAlert.relevantAlerts.isEmpty()) {
                            return@mapNotNull null
                        }
                        return@mapNotNull childAlert
                    }
                    .associateBy { it.stop.id }

            val patternsByMapRoute = mutableMapOf<MapStopRoute, List<RoutePattern>>()
            for (patternId in patternIdsByStop.getOrElse(stop.id) { listOf() }) {
                val pattern = routePatterns[patternId] ?: continue
                val route = routes[pattern.routeId] ?: continue
                val mapRoute = MapStopRoute.matching(route) ?: continue
                patternsByMapRoute[mapRoute] =
                    patternsByMapRoute.getOrElse(mapRoute) { listOf() } + pattern
            }

            serviceStatus =
                getServiceStatus(
                    stop,
                    getServiceAlerts(relevantAlerts),
                    patternsByMapRoute,
                    childStops,
                    childAlerts
                )
        }
    }

    constructor(
        stop: Stop,
        relevantAlerts: List<Alert>,
        serviceStatus: Map<MapStopRoute, StopAlertState>
    ) : this(stop) {
        this.relevantAlerts = relevantAlerts
        this.serviceAlerts = getServiceAlerts(relevantAlerts)
        this.childAlerts = mapOf()
        this.serviceStatus = serviceStatus
    }

    constructor(
        stop: Stop,
        relevantAlerts: List<Alert>,
        childAlerts: Map<String, AlertAssociatedStop>,
        serviceStatus: Map<MapStopRoute, StopAlertState>
    ) : this(stop) {
        this.relevantAlerts = relevantAlerts
        this.serviceAlerts = getServiceAlerts(relevantAlerts)
        this.childAlerts = childAlerts
        this.serviceStatus = serviceStatus
    }
}

enum class StopAlertState {
    Issue,
    Normal,
    Shuttle,
    Suspension
}

private fun entityMatcher(
    entity: Alert.InformedEntity,
    stop: Stop,
    pattern: RoutePattern
): Boolean {
    return entity.appliesTo(
        stopId = stop.id,
        routeId = pattern.routeId,
        directionId = pattern.directionId
    )
}

private fun getDisruptableChildren(childStops: Map<String, Stop>): List<Stop> {
    return childStops.values.filter {
        listOf(LocationType.STOP, LocationType.STATION).contains(it.locationType)
    }
}

private fun getServiceAlerts(alerts: List<Alert>): List<Alert> {
    return alerts.filter { Alert.serviceDisruptionEffects.contains(it.effect) }
}

private fun getServiceStatus(
    stop: Stop,
    serviceAlerts: List<Alert>,
    patternsByMapRoute: Map<MapStopRoute, List<RoutePattern>>,
    childStops: Map<String, Stop>,
    childAlerts: Map<String, AlertAssociatedStop>
): Map<MapStopRoute, StopAlertState> {
    val children = getDisruptableChildren(childStops)
    return patternsByMapRoute.mapValues { (mapRoute, patterns) ->
        val hasSuspension =
            if (patterns.isEmpty()) {
                // No route patterns and every child station/stop has no service
                childStops.isNotEmpty() && children.all { hasSuspension(it, mapRoute, childAlerts) }
            } else {
                // All route patterns and child stations/stops have no service
                patterns.all { isDisruptedPattern(it, stop, serviceAlerts) } &&
                    children.all { hasSuspension(it, mapRoute, childAlerts) }
            }
        if (hasSuspension) {
            return@mapValues StopAlertState.Suspension
        }

        val hasSomeDisruptedService: Boolean =
            patterns.any { isDisruptedPattern(it, stop, serviceAlerts) } ||
                children.any { hasSuspension(it, mapRoute, childAlerts) }
        if (hasSomeDisruptedService) {
            return@mapValues StopAlertState.Issue
        }

        return@mapValues StopAlertState.Normal
    }
}

private fun hasSuspension(
    stop: Stop,
    on: MapStopRoute,
    alerts: Map<String, AlertAssociatedStop>
): Boolean {
    return alerts[stop.id]?.serviceStatus?.get(on) == StopAlertState.Suspension
}

private fun hasShuttle(
    stop: Stop,
    on: MapStopRoute,
    alerts: Map<String, AlertAssociatedStop>
): Boolean {
    return alerts[stop.id]?.serviceStatus?.get(on) == StopAlertState.Shuttle
}

private fun hasIssue(
    stop: Stop,
    on: MapStopRoute,
    alerts: Map<String, AlertAssociatedStop>
): Boolean {
    return alerts[stop.id]?.serviceStatus?.get(on) == StopAlertState.Issue
}

private fun isDisruptedPattern(
    pattern: RoutePattern,
    stop: Stop,
    serviceAlerts: List<Alert>
): Boolean {
    return serviceAlerts.any { alert ->
        alert.anyInformedEntity { entityMatcher(it, stop, pattern) }
    }
}
