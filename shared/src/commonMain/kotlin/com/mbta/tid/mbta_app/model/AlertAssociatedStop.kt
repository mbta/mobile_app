package com.mbta.tid.mbta_app.model

private val disruptableStopTypes: List<LocationType> =
    listOf(LocationType.STOP, LocationType.STATION)

class AlertAssociatedStop(
    val stop: Stop,
    val relevantAlerts: Set<Alert>,
    val routePatterns: List<RoutePattern>,
    val childStops: Map<String, Stop>,
    val childAlerts: Map<String, AlertAssociatedStop>,
) {
    val serviceAlerts = relevantAlerts.filter { Alert.serviceDisruptionEffects.contains(it.effect) }

    var hasNoService: Boolean =
        if (routePatterns.isEmpty()) {
            // No route patterns and every child station/stop has no service
            childStops.isNotEmpty() &&
                childStops.values
                    .filter { disruptableStopTypes.contains(it.locationType) }
                    .all { childAlerts[it.id]?.hasNoService == true }
        } else {
            // All route patterns and child stations/stops have no service
            routePatterns.all { pattern ->
                serviceAlerts.any { alert ->
                    alert.anyInformedEntity { entityMatcher(it, stop, pattern) }
                }
            } &&
                childStops.values
                    .filter { disruptableStopTypes.contains(it.locationType) }
                    .all { childAlerts[it.id]?.hasNoService == true }
        }

    var hasSomeDisruptedService: Boolean =
        routePatterns.any { pattern ->
            serviceAlerts.any { alert ->
                alert.anyInformedEntity { entityMatcher(it, stop, pattern) }
            }
        } || stop.childStopIds?.any { childAlerts[it]?.hasSomeDisruptedService == true } == true

    private fun entityMatcher(
        entity: Alert.InformedEntity,
        stop: Stop,
        pattern: RoutePattern
    ): Boolean {
        return entity.appliesTo(stopId = stop.id, routeId = pattern.routeId)
    }
}
