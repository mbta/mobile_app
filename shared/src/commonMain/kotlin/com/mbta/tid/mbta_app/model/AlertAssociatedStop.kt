package com.mbta.tid.mbta_app.model

class AlertAssociatedStop(
    val stop: Stop,
    val relevantAlerts: List<Alert>,
    val serviceAlerts: List<Alert>,
    val childAlerts: Map<String, AlertAssociatedStop>,
    val serviceStatus: StopServiceStatus
) {
    constructor(
        stop: Stop,
        relevantAlerts: List<Alert>,
        routePatterns: List<RoutePattern>,
        childStops: Map<String, Stop>,
        childAlerts: Map<String, AlertAssociatedStop>
    ) : this(
        stop,
        relevantAlerts,
        getServiceAlerts(relevantAlerts),
        childAlerts,
        getServiceStatus(
            stop,
            getServiceAlerts(relevantAlerts),
            routePatterns,
            childStops,
            childAlerts
        )
    )
}

enum class StopServiceStatus {
    NORMAL,
    NO_SERVICE,
    PARTIAL_SERVICE
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
    routePatterns: List<RoutePattern>,
    childStops: Map<String, Stop>,
    childAlerts: Map<String, AlertAssociatedStop>
): StopServiceStatus {
    val children = getDisruptableChildren(childStops)

    val hasNoService =
        if (routePatterns.isEmpty()) {
            // No route patterns and every child station/stop has no service
            childStops.isNotEmpty() && children.all { hasNoService(it, childAlerts) }
        } else {
            // All route patterns and child stations/stops have no service
            routePatterns.all { isDisruptedPattern(it, stop, serviceAlerts) } &&
                children.all { hasNoService(it, childAlerts) }
        }
    if (hasNoService) {
        return StopServiceStatus.NO_SERVICE
    }

    val hasSomeDisruptedService: Boolean =
        routePatterns.any { isDisruptedPattern(it, stop, serviceAlerts) } ||
            children.any { hasNoService(it, childAlerts) }
    if (hasSomeDisruptedService) {
        return StopServiceStatus.PARTIAL_SERVICE
    }

    return StopServiceStatus.NORMAL
}

private fun hasNoService(stop: Stop, alerts: Map<String, AlertAssociatedStop>): Boolean {
    return alerts[stop.id]?.serviceStatus == StopServiceStatus.NO_SERVICE
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
