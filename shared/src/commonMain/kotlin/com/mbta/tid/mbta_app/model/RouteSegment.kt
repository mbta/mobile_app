package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal interface IRouteSegment {
    val sourceRoutePatternId: String
    val sourceRouteId: Route.Id
    val stopIds: List<String>
    val otherPatternsByStopId: Map<String, List<RoutePatternKey>>
}

@Serializable
public data class RoutePatternKey(
    @SerialName("route_id") val routeId: Route.Id,
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
    @SerialName("source_route_id") override val sourceRouteId: Route.Id,
    @SerialName("stop_ids") override val stopIds: List<String>,
    @SerialName("other_patterns_by_stop_id")
    override val otherPatternsByStopId: Map<String, List<RoutePatternKey>>,
) : IRouteSegment {

    /**
     * Split this route segment into one or more `AlertAwareRouteSegments` based on the alerts for
     * the stops within this segment.
     *
     * If [visibleRouteIds] is provided, only those routes are considered when
     * [requireAllRoutesDisrupted] checks whether every route serving a stretch is disrupted, so
     * that routes which aren't displayed on the map don't hide a disruption on the displayed ones.
     */
    internal fun splitAlertingSegments(
        alertsByStop: Map<String, AlertAssociatedStop>,
        requireAllRoutesDisrupted: Boolean = false,
        visibleRouteIds: Set<Route.Id>? = null,
    ): List<AlertAwareRouteSegment> {
        val stopsAlertState = alertStateByStopId(alertsByStop)

        val otherRoutesByStopId =
            if (visibleRouteIds == null) otherRoutesByStopId()
            else otherRoutesByStopId().mapValues { (_, routes) -> routes intersect visibleRouteIds }

        val alertingSegments =
            alertingSegments(
                stopIds,
                sourceRouteId,
                otherRoutesByStopId,
                stopsAlertState,
                requireAllRoutesDisrupted,
            )

        return alertingSegments.mapIndexed { index, (alertState, segmentStops) ->
            val stopIdSet = segmentStops.toSet()

            AlertAwareRouteSegment(
                id = "$id-$index",
                sourceRoutePatternId = sourceRoutePatternId,
                sourceRouteId = sourceRouteId,
                stopIds = segmentStops,
                isFirst = index == 0,
                isLast = index == alertingSegments.lastIndex,
                alertState = alertState,
                otherPatternsByStopId = otherPatternsByStopId.filter { stopIdSet.contains(it.key) },
            )
        }
    }

    internal data class StopAlertState(val hasSuspension: Boolean, val hasShuttle: Boolean) {
        companion object {
            val Normal = StopAlertState(hasSuspension = false, hasShuttle = false)
        }
    }

    private fun otherRoutesByStopId(): Map<String, Set<Route.Id>> =
        otherPatternsByStopId.mapValues { (_, patterns) ->
            patterns.map { it.routeId }.toSet()
        }

    /**
     * Checks if each stop ID has a service alert relevant to this route segment, tracked separately
     * for each route. A route is relevant at a stop if it is the `sourceRouteId` for the segment or
     * is included in the `otherPatternsByStopId` for that stop.
     *
     * Only contains keys for stops with an alert, and for each stop only the routes with an alert.
     */
    internal fun alertStateByStopId(
        alertsByStop: Map<String, AlertAssociatedStop>
    ): Map<String, Map<Route.Id, StopAlertState>> {
        val otherRoutesByStopId = otherRoutesByStopId()
        return stopIds
            .associateWith { stopId ->
                val alertsForStop = alertsByStop[stopId] ?: return@associateWith emptyMap()
                val routes = otherRoutesByStopId[stopId].orEmpty() + sourceRouteId

                val allServiceAlerts =
                    (alertsForStop.serviceAlerts +
                            alertsForStop.childAlerts.values.flatMap { it.serviceAlerts })
                        .distinct()

                routes
                    .associateWith { route ->
                        val serviceAlerts = allServiceAlerts.filter { alert ->
                            alert.anyInformedEntity { it.route == route }
                        }
                        StopAlertState(
                            hasSuspension =
                                serviceAlerts.any { it.effect == Alert.Effect.Suspension },
                            hasShuttle = serviceAlerts.any { it.effect == Alert.Effect.Shuttle },
                        )
                    }
                    .filterValues { it.hasSuspension || it.hasShuttle }
            }
            .filterValues { it.isNotEmpty() }
    }

    internal companion object {
        /**
         * Split the list of stops into segments based on whether they are alerting. Stops on
         * boundaries are included in both segments.
         *
         * By default, a stop is treated as alerting if any route serving it is disrupted.
         *
         * If [requireAllRoutesDisrupted] is set, a pair of adjacent stops is only alerting if every
         * route serving both stops is disrupted between them. A stretch of track shared by multiple
         * routes isn't shown as disrupted while other routes still provide service along it. If the
         * routes have a mix of suspensions and shuttles, the stretch is shown as suspended.
         */
        internal fun alertingSegments(
            stopIds: List<String>,
            sourceRouteId: Route.Id,
            otherRoutesByStopId: Map<String, Set<Route.Id>>,
            stopsAlertState: Map<String, Map<Route.Id, StopAlertState>>,
            requireAllRoutesDisrupted: Boolean = false,
        ): List<Pair<SegmentAlertState, List<String>>> {

            if (stopIds.isEmpty()) {
                return listOf()
            }

            fun pairState(first: StopAlertState, second: StopAlertState): SegmentAlertState =
                if (first.hasSuspension && second.hasSuspension) {
                    SegmentAlertState.Suspension
                } else if (first.hasShuttle && second.hasShuttle) {
                    SegmentAlertState.Shuttle
                } else {
                    SegmentAlertState.Normal
                }

            fun anyRouteState(stopId: String): StopAlertState {
                val routeStates = stopsAlertState[stopId]?.values.orEmpty()
                return StopAlertState(
                    hasSuspension = routeStates.any { it.hasSuspension },
                    hasShuttle = routeStates.any { it.hasShuttle },
                )
            }

            val stopPairSegments =
                stopIds.windowed(size = 2, step = 1) { (firstStopId, secondStopId) ->
                    if (!requireAllRoutesDisrupted) {
                        return@windowed Pair(
                            pairState(anyRouteState(firstStopId), anyRouteState(secondStopId)),
                            listOf(firstStopId, secondStopId),
                        )
                    }
                    val routes =
                        (otherRoutesByStopId[firstStopId].orEmpty() intersect
                            otherRoutesByStopId[secondStopId].orEmpty()) + sourceRouteId
                    val routeStates = routes.map { route ->
                        pairState(
                            stopsAlertState[firstStopId]?.get(route) ?: StopAlertState.Normal,
                            stopsAlertState[secondStopId]?.get(route) ?: StopAlertState.Normal,
                        )
                    }
                    val segmentState =
                        when {
                            routeStates.contains(SegmentAlertState.Normal) ->
                                SegmentAlertState.Normal
                            routeStates.contains(SegmentAlertState.Suspension) ->
                                SegmentAlertState.Suspension
                            else -> SegmentAlertState.Shuttle
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
    override val sourceRouteId: Route.Id,
    override val stopIds: List<String>,
    override val otherPatternsByStopId: Map<String, List<RoutePatternKey>>,
    val isFirst: Boolean = false,
    val isLast: Boolean = false,
    val alertState: SegmentAlertState,
) : IRouteSegment

public enum class SegmentAlertState {
    Suspension,
    Shuttle,
    Normal,
}
