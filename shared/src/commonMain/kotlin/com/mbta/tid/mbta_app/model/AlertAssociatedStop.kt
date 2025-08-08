package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.serialization.Serializable

public class AlertAssociatedStop internal constructor(internal val stop: Stop) {
    internal var relevantAlerts: List<Alert> = mutableListOf()
    internal var serviceAlerts: List<Alert> = mutableListOf()
    internal var childAlerts: Map<String, AlertAssociatedStop> = mutableMapOf()
    internal var stateByRoute: Map<MapStopRoute, StopAlertState> = mutableMapOf()

    internal constructor(
        stop: Stop,
        alertsByStop: Map<String, Set<Alert>>,
        nullStopAlerts: Set<Alert>,
        global: GlobalResponse,
    ) : this(stop) {
        global.run {
            relevantAlerts = alertsByStop[stop.id]?.toList() ?: emptyList()

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

            for (patternId in global.patternIdsByStop.getOrElse(stop.id) { listOf() }) {
                val pattern = global.routePatterns[patternId] ?: continue
                relevantAlerts =
                    nullStopAlerts.filter { alert ->
                        alert.anyInformedEntity { entityMatcher(it, null, pattern) }
                    } + relevantAlerts
            }

            serviceAlerts = getServiceAlerts(relevantAlerts)

            stateByRoute = getAlertStateByRoute(stop, serviceAlerts, childAlerts, global)
        }
    }

    internal constructor(
        stop: Stop,
        relevantAlerts: List<Alert>,
        stateByRoute: Map<MapStopRoute, StopAlertState>,
    ) : this(stop) {
        this.relevantAlerts = relevantAlerts
        this.serviceAlerts = getServiceAlerts(relevantAlerts)
        this.childAlerts = mapOf()
        this.stateByRoute = stateByRoute
    }

    internal constructor(
        stop: Stop,
        relevantAlerts: List<Alert>,
        childAlerts: Map<String, AlertAssociatedStop>,
        stateByRoute: Map<MapStopRoute, StopAlertState>,
    ) : this(stop) {
        this.relevantAlerts = relevantAlerts
        this.serviceAlerts = getServiceAlerts(relevantAlerts)
        this.childAlerts = childAlerts
        this.stateByRoute = stateByRoute
    }
}

@Serializable
public enum class StopAlertState {
    Elevator,
    Issue,
    Normal,
    Shuttle,
    Suspension,
}

private fun entityMatcher(
    entity: Alert.InformedEntity,
    stop: Stop?,
    pattern: RoutePattern,
): Boolean {
    return entity.appliesTo(
        stopId = stop?.id,
        routeId = pattern.routeId,
        directionId = pattern.directionId,
    )
}

private fun getServiceAlerts(alerts: List<Alert>): List<Alert> {
    // In practice, AlertAssociatedStop is only used for the map, where only Major alerts are shown.
    return alerts.filter { it.significance >= AlertSignificance.Major }
}

private fun getAlertStateByRoute(
    stop: Stop,
    serviceAlerts: List<Alert>,
    childAlerts: Map<String, AlertAssociatedStop>,
    global: GlobalResponse,
): Map<MapStopRoute, StopAlertState> {
    val patternsByMapRoute = mutableMapOf<MapStopRoute, List<RoutePattern>>()
    for (patternId in global.patternIdsByStop.getOrElse(stop.id) { listOf() }) {
        val pattern = global.routePatterns[patternId] ?: continue
        val route = global.routes[pattern.routeId] ?: continue
        val mapRoute = MapStopRoute.matching(route) ?: continue

        patternsByMapRoute[mapRoute] = patternsByMapRoute.getOrElse(mapRoute) { listOf() } + pattern
    }

    return MapStopRoute.entries
        .mapNotNull { mapRoute ->
            val patterns = patternsByMapRoute[mapRoute]
            // If this is a child stop without any relevant patterns, don't add an entry for it
            // to the result map. A parent stop usually doesn't have any relevant patterns, they're
            // only attached to child stops, so parents always need to be checked further.
            if (stop.parentStationId != null && patterns == null) {
                return@mapNotNull null
            }

            val childStates =
                childAlerts.values.mapNotNull { stopState(it.stop, mapRoute, childAlerts) }

            val patternStates =
                (patterns ?: emptyList()).map { statesForPattern(it, stop, serviceAlerts) }

            // Children will always have a Normal StopAlertState if they have any service for the
            // given MapStopRoute, so patterns and child states being empty here means that the
            // station doesn't serve any matching routes, and we don't need to set a state for it.
            if (patternStates.isEmpty() && childStates.isEmpty()) {
                return@mapNotNull null
            }

            val distinctStates = (patternStates + childStates).distinct()
            val finalState =
                if (distinctStates.size == 1) {
                    // If all the parent and child alert states are the same, return that state
                    distinctStates.single()
                } else if (
                    distinctStates.size == 2 && distinctStates.contains(StopAlertState.Normal)
                ) {
                    // If there's one alert that's only present on some patterns/children, return
                    // that alert
                    distinctStates.filterNot { it == StopAlertState.Normal }.single()
                } else {
                    // If there are multiple kinds of alert, return a generic Issue
                    StopAlertState.Issue
                }
            return@mapNotNull mapRoute to finalState
        }
        .toMap()
}

private fun stopState(
    stop: Stop,
    on: MapStopRoute,
    alerts: Map<String, AlertAssociatedStop>,
): StopAlertState? {
    return alerts[stop.id]?.stateByRoute?.get(on)
}

private fun statesForPattern(
    pattern: RoutePattern,
    stop: Stop,
    serviceAlerts: List<Alert>,
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
