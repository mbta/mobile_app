package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal interface IRouteSegment {
    val sourceRoutePatternId: String
    val sourceRouteId: String
    val stopIds: List<String>
    val otherPatternsByStopId: Map<String, List<RoutePatternKey>>
}

@Serializable
public data class RoutePatternKey(
    @SerialName("route_id") val routeId: String,
    @SerialName("route_pattern_id") val routePatternId: String,
)

@Serializable
/**
 * A sequential chunk of stops on a route pattern. A segment may only intersect with other segments
 * at the first and last stop.
 */
public data class RouteSegment(
    val id: String,
    @SerialName("source_route_pattern_id") override val sourceRoutePatternId: String,
    @SerialName("source_route_id") override val sourceRouteId: String,
    @SerialName("stop_ids") override val stopIds: List<String>,
    @SerialName("other_patterns_by_stop_id")
    override val otherPatternsByStopId: Map<String, List<RoutePatternKey>>,
) : IRouteSegment {

    /**
     * Split this route segment into one or more `AlertAwareRouteSegments` based on the alerts for
     * the stops within this segment.
     */
    internal fun splitAlertingSegments(
        alertsByStop: Map<String, AlertAssociatedStop>
    ): List<AlertAwareRouteSegment> {
        val stopsAlertState = alertStateByStopId(alertsByStop)

        val alertingSegments = alertingSegments(stopIds, stopsAlertState)

        return alertingSegments.mapIndexed { index, (alertState, segmentStops) ->
            val stopIdSet = segmentStops.toSet()

            AlertAwareRouteSegment(
                id = "$id-$index",
                sourceRoutePatternId = sourceRoutePatternId,
                sourceRouteId = sourceRouteId,
                stopIds = segmentStops,
                alertState = alertState,
                otherPatternsByStopId = otherPatternsByStopId.filter { stopIdSet.contains(it.key) },
            )
        }
    }

    internal data class StopAlertState(val hasSuspension: Boolean, val hasShuttle: Boolean)

    /**
     * Checks if each stop ID has a service alert relevant to this route segment. A service alert
     * for a stop is relevant if it applies to the `sourceRouteId` for the segment or any route
     * included in the `otherPatternsByStopId` for that stop.
     *
     * Only contains keys for stops with an alert.
     */
    internal fun alertStateByStopId(
        alertsByStop: Map<String, AlertAssociatedStop>
    ): Map<String, StopAlertState> {
        return stopIds
            .associateWith { stopId ->
                if (!alertsByStop.containsKey(stopId)) {
                    StopAlertState(hasSuspension = false, hasShuttle = false)
                } else {

                    var routes: Set<String> =
                        otherPatternsByStopId
                            .getOrElse(stopId) { listOf() }
                            .map { it.routeId }
                            .toSet()
                    routes = routes.plus(sourceRouteId)

                    val allServiceAlerts =
                        (alertsByStop[stopId]?.serviceAlerts.orEmpty() +
                                alertsByStop[stopId]
                                    ?.childAlerts
                                    ?.values
                                    ?.flatMap { it.serviceAlerts }
                                    .orEmpty())
                            .distinct()
                    val serviceAlerts =
                        allServiceAlerts.filter { alert ->
                            alert.anyInformedEntity { informedEntity ->
                                informedEntity.route != null &&
                                    routes.contains(informedEntity.route)
                            }
                        }

                    // TODO determine effects that count
                    StopAlertState(
                        hasSuspension = serviceAlerts.any { it.effect == Alert.Effect.Suspension },
                        hasShuttle = serviceAlerts.any { it.effect == Alert.Effect.Shuttle },
                    )
                }
            }
            .filterValues { it.hasSuspension || it.hasShuttle }
    }

    /**
     * Split the list of stops into segments based on whether or not they are alerting. Stops on
     * boundaries are included in both segments.
     */
    internal companion object {
        internal fun alertingSegments(
            stopIds: List<String>,
            stopsAlertState: Map<String, StopAlertState>,
        ): List<Pair<SegmentAlertState, List<String>>> {

            if (stopIds.isEmpty()) {
                return listOf()
            }

            val stopPairSegments =
                stopIds
                    .map {
                        Pair(
                            it,
                            stopsAlertState.getOrElse(it) {
                                StopAlertState(hasSuspension = false, hasShuttle = false)
                            },
                        )
                    }
                    .windowed(size = 2, step = 1) { (firstStop, secondStop) ->
                        val (firstStopId, firstStopAlerting) = firstStop
                        val (secondStopId, secondStopAlerting) = secondStop
                        val segmentState =
                            if (
                                firstStopAlerting.hasSuspension && secondStopAlerting.hasSuspension
                            ) {
                                SegmentAlertState.Suspension
                            } else if (
                                firstStopAlerting.hasShuttle && secondStopAlerting.hasShuttle
                            ) {
                                SegmentAlertState.Shuttle
                            } else {
                                SegmentAlertState.Normal
                            }
                        Pair(segmentState, listOf(firstStopId, secondStopId))
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
internal data class AlertAwareRouteSegment(
    val id: String,
    override val sourceRoutePatternId: String,
    override val sourceRouteId: String,
    override val stopIds: List<String>,
    override val otherPatternsByStopId: Map<String, List<RoutePatternKey>>,
    val alertState: SegmentAlertState,
) : IRouteSegment

public enum class SegmentAlertState {
    Suspension,
    Shuttle,
    Normal,
}
