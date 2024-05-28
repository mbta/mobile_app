package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface IRouteSegment {
    val sourceRoutePatternId: String
    val sourceRouteId: String
    val stopIds: List<String>
    val otherPatternsByStopId: Map<String, List<RoutePatternKey>>
}

@Serializable
data class RoutePatternKey(
    @SerialName("route_id") val routeId: String,
    @SerialName("route_pattern_id") val routePatternId: String
)

@Serializable
/**
 * A sequential chunk of stops on a route pattern. A segment may only intersect with other segments
 * at the first and last stop.
 */
data class RouteSegment(
    val id: String,
    @SerialName("source_route_pattern_id") override val sourceRoutePatternId: String,
    @SerialName("source_route_id") override val sourceRouteId: String,
    @SerialName("stop_ids") override val stopIds: List<String>,
    @SerialName("other_patterns_by_stop_id")
    override val otherPatternsByStopId: Map<String, List<RoutePatternKey>>
) : IRouteSegment {

    /**
     * Split this route segment into one or more `AlertAwareRouteSegments` based on the alerts for
     * the stops within this segment.
     */
    fun splitAlertingSegments(
        alertsByStop: Map<String, AlertAssociatedStop>
    ): List<AlertAwareRouteSegment> {
        val stopsWithServiceAlerts = hasServiceAlertByStopId(alertsByStop)

        val alertingSegments = alertingSegments(stopIds, stopsWithServiceAlerts)

        return alertingSegments.mapIndexed { index, (isAlerting, segmentStops) ->
            val stopIdSet = segmentStops.toSet()

            AlertAwareRouteSegment(
                id = "$id-$index",
                sourceRoutePatternId = sourceRoutePatternId,
                sourceRouteId = sourceRouteId,
                stopIds = segmentStops,
                isAlerting = isAlerting,
                otherPatternsByStopId = otherPatternsByStopId.filter { stopIdSet.contains(it.key) }
            )
        }
    }

    /**
     * Get the set of stop IDs that have a service alert relevant to this route segment. A service
     * alert for a stop is relevant if it applies to the `sourceRouteId` for the segment or any
     * route included in the `otherPatternsByStopId` for that stop.
     */
    fun hasServiceAlertByStopId(alertsByStop: Map<String, AlertAssociatedStop>): Set<String> {
        return stopIds
            .filter { stopId ->
                if (!alertsByStop.containsKey(stopId)) {
                    false
                } else {

                    var routes: Set<String> =
                        otherPatternsByStopId
                            .getOrElse(stopId) { listOf() }
                            .map { it.routeId }
                            .toSet()
                    routes = routes.plus(sourceRouteId)

                    val hasServiceAlert: Boolean =
                        alertsByStop[stopId]?.serviceAlerts?.any { alert ->
                            alert.anyInformedEntity { informedEntity ->
                                informedEntity.route != null &&
                                    routes.contains(informedEntity.route)
                            }
                        }
                            ?: false
                    hasServiceAlert
                }
            }
            .toSet()
    }

    /**
     * Split the list of stops into segments based on whether or not they are alerting. Stops on
     * boundaries are included in both segments.
     */
    companion object {
        fun alertingSegments(
            stopIds: List<String>,
            alertingStopIds: Set<String>
        ): List<Pair<Boolean, List<String>>> {

            if (stopIds.isEmpty()) {
                return listOf()
            }

            val stopPairSegments =
                stopIds
                    .map { Pair(it, alertingStopIds.contains(it)) }
                    .windowed(size = 2, step = 1) { (firstStop, secondStop) ->
                        val (firstStopId, firstStopAlerting) = firstStop
                        val (secondStopId, secondStopAlerting) = secondStop
                        val segmentAlerting = firstStopAlerting && secondStopAlerting
                        Pair(segmentAlerting, listOf(firstStopId, secondStopId))
                    }

            return stopPairSegments.fold(emptyList()) { prevSegments, currSegment ->
                if (prevSegments.isEmpty()) {
                    return@fold listOf(currSegment)
                }
                val oldSegments = prevSegments.dropLast(1)
                val lastSegment = prevSegments.last()
                check(lastSegment.second.last() == currSegment.second.first())
                if (lastSegment.first == currSegment.first) {
                    oldSegments +
                        listOf(
                            Pair(lastSegment.first, lastSegment.second + currSegment.second.drop(1))
                        )
                } else {
                    prevSegments + listOf(currSegment)
                }
            }
        }
    }
}

/**
 * A route segment of consecutive stops that form an alerting or non-alerting segment. Non-alerting
 * segments that are adjacent to an alerting segment will include the consecutive stops up to and
 * including the adjacent alerting stop.
 */
data class AlertAwareRouteSegment(
    val id: String,
    override val sourceRoutePatternId: String,
    override val sourceRouteId: String,
    override val stopIds: List<String>,
    override val otherPatternsByStopId: Map<String, List<RoutePatternKey>>,
    val isAlerting: Boolean
) : IRouteSegment
