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
        nullStopAlerts: Set<Alert>,
        global: GlobalResponse
    ) : this(stop) {
        global.run {
            relevantAlerts = alertsByStop[stop.id]?.toList() ?: emptyList()
            serviceAlerts = getServiceAlerts(relevantAlerts)

            val childStops =
                stop.childStopIds
                    .mapNotNull { childId -> stops[childId] }
                    .filter {
                        listOf(LocationType.STOP, LocationType.STATION).contains(it.locationType)
                    }
            childAlerts =
                childStops
                    .map { child ->
                        val childAlert =
                            AlertAssociatedStop(child, alertsByStop, nullStopAlerts, this)
                        return@map child.id to childAlert
                    }
                    .toMap()

            val patternsByMapRoute = mutableMapOf<MapStopRoute, List<RoutePattern>>()
            for (patternId in patternIdsByStop.getOrElse(stop.id) { listOf() }) {
                val pattern = routePatterns[patternId] ?: continue
                val route = routes[pattern.routeId] ?: continue
                val mapRoute = MapStopRoute.matching(route) ?: continue

                relevantAlerts =
                    nullStopAlerts.filter { alert ->
                        alert.anyInformedEntity { entityMatcher(it, null, pattern) }
                    } + relevantAlerts
                patternsByMapRoute[mapRoute] =
                    patternsByMapRoute.getOrElse(mapRoute) { listOf() } + pattern
            }

            serviceStatus =
                getServiceStatus(
                    stop,
                    getServiceAlerts(relevantAlerts),
                    patternsByMapRoute,
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
    stop: Stop?,
    pattern: RoutePattern
): Boolean {
    return entity.appliesTo(
        stopId = stop?.id,
        routeId = pattern.routeId,
        directionId = pattern.directionId
    )
}

private fun getServiceAlerts(alerts: List<Alert>): List<Alert> {
    return alerts.filter { Alert.serviceDisruptionEffects.contains(it.effect) }
}

private fun getServiceStatus(
    stop: Stop,
    serviceAlerts: List<Alert>,
    patternsByMapRoute: Map<MapStopRoute, List<RoutePattern>>,
    childAlerts: Map<String, AlertAssociatedStop>
): Map<MapStopRoute, StopAlertState> {
    val result =
        MapStopRoute.entries
            .mapNotNull { mapRoute ->
                val patterns = patternsByMapRoute[mapRoute]
                if (stop.parentStationId != null && patterns == null) {
                    // If this is a child stop and has no patterns in this route category,
                    // don't add an entry for it to the result map.
                    return@mapNotNull null
                }

                val childStates =
                    childAlerts.values
                        .mapNotNull childState@{
                            val state = stopState(it.stop, mapRoute, childAlerts)
                            return@childState if (state != null) {
                                it.stop.id to state
                            } else {
                                null
                            }
                        }
                        .toMap()

                val patternStates =
                    (patterns ?: emptyList()).map { statusForPattern(it, stop, serviceAlerts) }

                if (patternStates.isEmpty() && childStates.isEmpty()) {
                    return@mapNotNull null
                }

                if (
                    patternStates.isEmpty() &&
                        serviceAlerts.any { alert -> isBoundaryParent(stop, alert, childStates) }
                ) {
                    return@mapNotNull mapRoute to StopAlertState.Issue
                }

                val representativeState = patternStates.getOrNull(0) ?: childStates.values.first()
                val finalState =
                    if ((patternStates + childStates.values).all { it == representativeState }) {
                        representativeState
                    } else {
                        return@mapNotNull mapRoute to StopAlertState.Issue
                    }

                return@mapNotNull mapRoute to finalState
            }
            .toMap()
    return result
}

private fun stopState(
    stop: Stop,
    on: MapStopRoute,
    alerts: Map<String, AlertAssociatedStop>
): StopAlertState? {
    return alerts[stop.id]?.serviceStatus?.get(on)
}

private fun statusForPattern(
    pattern: RoutePattern,
    stop: Stop,
    serviceAlerts: List<Alert>
): StopAlertState {

    val matchingAlert =
        serviceAlerts.find { alert -> alert.anyInformedEntity { entityMatcher(it, stop, pattern) } }
            ?: return StopAlertState.Normal
    if (matchingAlert.effect == Alert.Effect.Shuttle) {
        return StopAlertState.Shuttle
    }
    if (
        listOf(Alert.Effect.Detour, Alert.Effect.StopMoved, Alert.Effect.StopMove)
            .contains(matchingAlert.effect)
    ) {
        return StopAlertState.Issue
    }
    return StopAlertState.Suspension
}

// For stations at the boundary of a disruption, with service in one direction but not the other,
// they will always have informed entities for each child platform with a different set of
// activities depending on that platform's service. So either "Board" or "Ride" will always be
// missing on the respective child platforms at every parent stop on the boundary of an alert.
private fun isBoundaryParent(
    stop: Stop,
    alert: Alert,
    childStates: Map<String, StopAlertState>
): Boolean {
    if (stop.parentStationId != null) {
        return false
    }
    return alert
        .matchingEntities { it.appliesTo(stopId = stop.id) }
        .all { parentAlert ->
            val childEntities =
                stop.childStopIds.associateWith { childId ->
                    alert.informedEntity.find {
                        it.appliesTo(routeId = parentAlert.route, stopId = childId)
                    }
                }

            return childStates.isNotEmpty() &&
                childStates.keys.all { relevantChild ->
                    childStates[relevantChild] != StopAlertState.Normal &&
                        childEntities[relevantChild]
                            ?.activities
                            ?.containsAll(
                                setOf(
                                    Alert.InformedEntity.Activity.Board,
                                    Alert.InformedEntity.Activity.Exit,
                                    Alert.InformedEntity.Activity.Ride
                                )
                            ) == false
                }
        }
}
