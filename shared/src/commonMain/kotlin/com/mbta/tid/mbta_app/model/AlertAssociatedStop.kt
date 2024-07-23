package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.serialization.Serializable

class AlertAssociatedStop(val stop: Stop) {
    var relevantAlerts: List<Alert> = mutableListOf()
    var serviceAlerts: List<Alert> = mutableListOf()
    var childAlerts: Map<String, AlertAssociatedStop> = mutableMapOf()
    var stateByRoute: Map<MapStopRoute, StopAlertState> = mutableMapOf()

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

            for (patternId in global.patternIdsByStop.getOrElse(stop.id) { listOf() }) {
                val pattern = global.routePatterns[patternId] ?: continue
                relevantAlerts =
                    nullStopAlerts.filter { alert ->
                        alert.anyInformedEntity { entityMatcher(it, null, pattern) }
                    } + relevantAlerts
            }

            stateByRoute =
                getAlertStateByRoute(stop, getServiceAlerts(relevantAlerts), childAlerts, global)
        }
    }

    constructor(
        stop: Stop,
        relevantAlerts: List<Alert>,
        stateByRoute: Map<MapStopRoute, StopAlertState>
    ) : this(stop) {
        this.relevantAlerts = relevantAlerts
        this.serviceAlerts = getServiceAlerts(relevantAlerts)
        this.childAlerts = mapOf()
        this.stateByRoute = stateByRoute
    }

    constructor(
        stop: Stop,
        relevantAlerts: List<Alert>,
        childAlerts: Map<String, AlertAssociatedStop>,
        stateByRoute: Map<MapStopRoute, StopAlertState>
    ) : this(stop) {
        this.relevantAlerts = relevantAlerts
        this.serviceAlerts = getServiceAlerts(relevantAlerts)
        this.childAlerts = childAlerts
        this.stateByRoute = stateByRoute
    }
}

@Serializable
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

private fun getAlertStateByRoute(
    stop: Stop,
    serviceAlerts: List<Alert>,
    childAlerts: Map<String, AlertAssociatedStop>,
    global: GlobalResponse
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
                childAlerts.values.mapNotNull childState@{
                    stopState(it.stop, mapRoute, childAlerts)
                }

            val patternStates =
                (patterns ?: emptyList()).map { statesForPattern(it, stop, serviceAlerts) }

            // Children will always have a Normal StopAlertState if they have any service for the
            // given MapStopRoute, so patterns and child states being empty here means that the
            // station doesn't serve any matching routes, and we don't need to set a state for it.
            if (patternStates.isEmpty() && childStates.isEmpty()) {
                return@mapNotNull null
            }

            // Check if the stop is disrupted but still has service running in one direction
            if (
                patternStates.isEmpty() &&
                    serviceAlerts.any { alert ->
                        isPartiallyDisrupted(stop, alert, mapRoute, global)
                    }
            ) {
                return@mapNotNull mapRoute to StopAlertState.Issue
            }

            // Check if all the parent and child alert states are the same, if they are, return
            // that state, otherwise return a more generic Issue state.
            val representativeState = patternStates.getOrNull(0) ?: childStates.first()
            val finalState =
                if ((patternStates + childStates).all { it == representativeState }) {
                    representativeState
                } else {
                    return@mapNotNull mapRoute to StopAlertState.Issue
                }
            return@mapNotNull mapRoute to finalState
        }
        .toMap()
}

private fun stopState(
    stop: Stop,
    on: MapStopRoute,
    alerts: Map<String, AlertAssociatedStop>
): StopAlertState? {
    return alerts[stop.id]?.stateByRoute?.get(on)
}

private fun statesForPattern(
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

// This function returns true if the alert applies to the stop for the given mapRoute in one
// direction but not the other. Terminal stops will always return false, but if a stop is in the
// middle of a route and at the boundary of the alert, it will return true.
private fun isPartiallyDisrupted(
    stop: Stop,
    alert: Alert,
    mapRoute: MapStopRoute,
    global: GlobalResponse
): Boolean {
    if (stop.parentStationId != null) {
        return false
    }

    // We only want to check for a specific MapStopRoute, so this gets all the informed entities
    // on the provided alert which have a route matching that MapStopRoute.
    val entitiesAtStopAndRoute =
        alert.matchingEntities {
            val entityRoute = global.routes[it.route] ?: return@matchingEntities false
            it.appliesTo(stopId = stop.id) && MapStopRoute.matching(entityRoute) == mapRoute
        }

    if (entitiesAtStopAndRoute.isEmpty()) {
        return false
    }

    // Then we need to associate each child stop with all their patterns on routes matching the
    // provided MapStopRoute that are typical or canonical
    val patternsByStop =
        (stop.childStopIds + stop.id)
            .map { stopId ->
                stopId to
                    (global.patternIdsByStop[stopId] ?: emptyList())
                        .mapNotNull { patternId -> global.routePatterns[patternId] }
                        .filter { pattern ->
                            val route = global.routes[pattern.routeId] ?: return@filter false
                            return@filter setOf(
                                    RoutePattern.Typicality.Typical,
                                    RoutePattern.Typicality.CanonicalOnly
                                )
                                .contains(pattern.typicality) &&
                                mapRoute == MapStopRoute.matching(route)
                        }
            }
            .filter { (_, patterns) -> patterns.isNotEmpty() }
            .toMap()

    // If this stop is the terminal at all patterns found above, we don't want to consider it as
    // being on the alert boundary. If we don't return separately, it will fulfill the entity check.
    if (isTerminalStop(patternsByStop, global)) {
        return false
    }

    return isServingBothDirections(stop, alert, entitiesAtStopAndRoute, patternsByStop)
}

// If the alert has an entity at the parent stop, it should also have entities for any child stops
// on the same route, and if those children have service in one direction but not the other, it
// should be missing either the Board or Exit activity depending which direction its for.
// There do seem to be some poorly understood edge cases where even if a child has service in one
// direction, it will still have both Board and Exit activities (for example, alerts at Kenmore).
private fun isServingBothDirections(
    stop: Stop,
    alert: Alert,
    entitiesAtStopAndRoute: List<Alert.InformedEntity>,
    patternsByStop: Map<String, List<RoutePattern>>
): Boolean {
    return entitiesAtStopAndRoute.any { entity ->
        val childEntities =
            stop.childStopIds.associateWith { childId ->
                alert.informedEntity.find { it.appliesTo(routeId = entity.route, stopId = childId) }
            }
        return@any patternsByStop.keys.any { stopId ->
            stopId == stop.id ||
                childEntities[stopId]
                    ?.activities
                    ?.containsAll(
                        setOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Exit
                        )
                    ) == false
        }
    }
}

// Check if a stop is a terminal if all the provided patterns, associated with their stop IDs,
// have that stop ID as either the first or last item in the representative trip stop list
private fun isTerminalStop(
    patternsByStop: Map<String, List<RoutePattern>>,
    global: GlobalResponse
): Boolean {
    return patternsByStop.all { (stopId, patterns) ->
        patterns.all terminalCheck@{ pattern ->
            val trip = global.trips[pattern.representativeTripId] ?: return@terminalCheck false
            val stopIds = trip.stopIds ?: return@terminalCheck false
            if (stopIds.isEmpty()) {
                return@terminalCheck false
            }
            return@terminalCheck stopIds.first() == stopId || stopIds.last() == stopId
        }
    }
}
