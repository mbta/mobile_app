package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.RouteStopsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

public data class RouteDetailsStopList(val directionId: Int, val segments: List<Segment>) {

    /** A subset of consecutive stops that are all typical or all non-typical. */
    public data class Segment(val stops: List<Entry>, val isTypical: Boolean) {
        /**
         * Assuming that this segment will be collapsed, returns the connections that should be
         * drawn in the toggle, and whether or not they should be twisted because they contain
         * stops.
         */
        public fun twistedConnections(): List<Pair<RouteBranchSegment.StickConnection, Boolean>>? {
            val lanesWithStops = stops.map { it.stopLane }.toSet()
            val connectionsBefore =
                stops.first().stickConnections.filter { it.fromVPos == RouteBranchSegment.VPos.Top }
            val connectionsAfter =
                stops.last().stickConnections.filter { it.toVPos == RouteBranchSegment.VPos.Bottom }
            val connectionsTransitive =
                connectionsBefore.mapNotNull { first ->
                    val second =
                        connectionsAfter.find { second ->
                            first.toLane == second.fromLane && first.toVPos == second.fromVPos
                        }
                    if (second != null) {
                        Triple(
                            first,
                            second,
                            RouteBranchSegment.StickConnection(
                                fromStop = first.fromStop,
                                toStop = second.toStop,
                                fromLane = first.fromLane,
                                toLane = second.toLane,
                                fromVPos = first.fromVPos,
                                toVPos = second.toVPos,
                            ),
                        )
                    } else {
                        null
                    }
                }
            val connections =
                (connectionsBefore.filter { first ->
                        !connectionsTransitive.any { it.first == first }
                    } +
                        connectionsAfter.filter { second ->
                            !connectionsTransitive.any { it.second == second }
                        } +
                        connectionsTransitive.map { it.third })
                    .distinct()
            val stickConnections =
                connections.map {
                    it to
                        (lanesWithStops.contains(it.fromLane) && lanesWithStops.contains(it.toLane))
                }
            val terminatedTwist =
                stickConnections.any { (connection, twisted) ->
                    twisted &&
                        (connection.fromVPos != RouteBranchSegment.VPos.Top ||
                            connection.toVPos != RouteBranchSegment.VPos.Bottom)
                }
            return if (terminatedTwist) null else stickConnections
        }
    }

    public data class Entry(
        val stop: Stop,
        val stopLane: RouteBranchSegment.Lane,
        val stickConnections: List<RouteBranchSegment.StickConnection>,
        val connectingRoutes: List<Route>,
    )

    public data class RouteParameters(
        val availableDirections: List<Int>,
        val directions: List<Direction>,
    ) {
        public constructor(
            lineOrRoute: LineOrRoute,
            globalData: GlobalResponse,
        ) : this(
            globalData.routePatterns.values
                .asSequence<RoutePattern>()
                .filter<RoutePattern> { it.routeId in lineOrRoute.allRoutes.map { it.id } }
                .map { it.directionId }
                .distinct()
                .sorted()
                .toList(),
            listOf(0, 1).map { directionId ->
                val name =
                    lineOrRoute.allRoutes
                        .map { it.directionNames[directionId] }
                        .distinct()
                        .singleOrNull()
                val destination =
                    lineOrRoute.allRoutes
                        .map { it.directionDestinations[directionId] }
                        .distinct()
                        .singleOrNull()
                Direction(name, destination, directionId)
            },
        )
    }

    public companion object {

        public suspend fun fromPieces(
            routeId: Route.Id,
            directionId: Int,
            routeStops: RouteStopsResult?,
            globalData: GlobalResponse,
        ): RouteDetailsStopList? =
            withContext(Dispatchers.Default) {
                if (
                    routeStops == null ||
                        routeStops.routeId != routeId ||
                        routeStops.directionId != directionId
                )
                    return@withContext null

                val segments =
                    routeStops.segments
                        .mapNotNull { segment ->
                            Segment(
                                    segment.stops.mapNotNull { branchStop ->
                                        val stopId = branchStop.stopId
                                        val stop =
                                            globalData
                                                .getStop(branchStop.stopId)
                                                ?.resolveParent(globalData)
                                                ?: return@mapNotNull null
                                        val transferRoutes =
                                            TripDetailsStopList.getTransferRoutes(
                                                stopId,
                                                routeId,
                                                globalData,
                                            )

                                        Entry(
                                            stop,
                                            branchStop.stopLane,
                                            branchStop.connections,
                                            transferRoutes,
                                        )
                                    },
                                    segment.isTypical,
                                )
                                .takeUnless { it.stops.isEmpty() }
                        }
                        .fold(mutableListOf<Segment>()) { acc, segment ->
                            if (acc.lastOrNull()?.isTypical == false && !segment.isTypical) {
                                // removeLast was added to the Android stdlib in API 35 and so will
                                // fail when compiled against API 35 but run on API <35
                                val priorSegment = acc.removeAt(acc.lastIndex)
                                acc.add(
                                    priorSegment.copy(stops = priorSegment.stops + segment.stops)
                                )
                            } else {
                                acc.add(segment)
                            }
                            acc
                        }

                RouteDetailsStopList(directionId, segments)
            }
    }
}
