package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.NewRouteStopsResult
import com.mbta.tid.mbta_app.repositories.OldRouteStopsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RouteDetailsStopList(
    val directionId: Int,
    val oldSegments: List<OldSegment>?,
    val newSegments: List<NewSegment>?,
) {

    /** A subset of consecutive stops that are all typical or all non-typical. */
    data class OldSegment(val stops: List<OldEntry>, val hasRouteLine: Boolean) {
        val isTypical = stops.all { it.isTypical }
    }

    /** A subset of consecutive stops that are all typical or all non-typical. */
    data class NewSegment(val stops: List<NewEntry>, val isTypical: Boolean) {
        /**
         * Assuming that this segment will be collapsed, returns the connections that should be
         * drawn in the toggle, and whether or not they should be twisted because they contain
         * stops.
         */
        fun twistedConnections(): List<Pair<RouteBranchSegment.StickConnection, Boolean>> {
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
            return connections.map {
                it to (lanesWithStops.contains(it.fromLane) && lanesWithStops.contains(it.toLane))
            }
        }
    }

    data class OldEntry(
        val stop: Stop,
        val patterns: List<RoutePattern>,
        val connectingRoutes: List<Route>,
    ) {
        val patternIds = patterns.map { it.id }.toSet()
        val isTypical = patterns.any { it.isTypical() }
    }

    data class NewEntry(
        val stop: Stop,
        val stopLane: RouteBranchSegment.Lane,
        val stickConnections: List<RouteBranchSegment.StickConnection>,
        val connectingRoutes: List<Route>,
    )

    data class RouteParameters(
        val availableDirections: List<Int>,
        val directions: List<Direction>,
    ) {
        constructor(
            lineOrRoute: RouteCardData.LineOrRoute,
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

    companion object {
        fun getLineOrRoute(
            selectionId: String,
            globalData: GlobalResponse,
        ): RouteCardData.LineOrRoute? {
            val route = globalData.getRoute(selectionId)
            val line = globalData.getLine(selectionId) ?: globalData.getLine(route?.lineId)
            return when {
                line != null && line.isGrouped ->
                    RouteCardData.LineOrRoute.Line(
                        line,
                        globalData.routesByLineId[line.id].orEmpty().toSet(),
                    )

                route != null -> RouteCardData.LineOrRoute.Route(route)
                else -> null
            }
        }

        suspend fun fromOldPieces(
            routeId: String,
            directionId: Int,
            routeStops: OldRouteStopsResult?,
            globalData: GlobalResponse,
        ): RouteDetailsStopList? =
            withContext(Dispatchers.Default) {
                if (
                    routeStops == null ||
                        routeStops.routeId != routeId ||
                        routeStops.directionId != directionId
                )
                    return@withContext null

                val stops =
                    routeStops.stopIds.mapNotNull { stopId ->
                        val stop =
                            globalData.getStop(stopId)?.resolveParent(globalData)
                                ?: return@mapNotNull null
                        val patterns =
                            globalData.getPatternsFor(stopId, routeId).filter {
                                it.directionId == directionId
                            }
                        val transferRoutes =
                            TripDetailsStopList.getTransferRoutes(stopId, routeId, globalData)
                        OldEntry(stop, patterns, transferRoutes)
                    }

                val segments = splitIntoOldSegments(stops)

                RouteDetailsStopList(directionId, segments, null)
            }

        suspend fun fromNewPieces(
            routeId: String,
            directionId: Int,
            routeStops: NewRouteStopsResult?,
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
                            NewSegment(
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

                                        NewEntry(
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
                        .fold(mutableListOf<NewSegment>()) { acc, segment ->
                            if (acc.lastOrNull()?.isTypical == false && !segment.isTypical) {
                                val priorSegment = acc.removeLast()
                                acc.add(
                                    priorSegment.copy(stops = priorSegment.stops + segment.stops)
                                )
                            } else {
                                acc.add(segment)
                            }
                            acc
                        }

                RouteDetailsStopList(directionId, null, segments)
            }

        /**
         * Split the list of entries into segments based on whether the stop serves a typical route
         * pattern.
         */
        private fun splitIntoOldSegments(entries: List<OldEntry>): List<OldSegment> {
            val authoritativePatternId =
                entries
                    .flatMapTo(mutableSetOf()) { it.patterns.filter(RoutePattern::isTypical) }
                    .minOrNull()
                    ?.id

            val segments: MutableList<MutableList<OldEntry>> = mutableListOf()

            entries.forEach { entry ->
                if (segments.isEmpty()) {
                    segments.add(mutableListOf(entry))
                } else {
                    val wipSegment = segments.last()
                    val lastEntry = wipSegment.last()
                    val isAuthoritativePatternBoundary =
                        authoritativePatternId != null &&
                            (entry.patternIds.contains(authoritativePatternId) !=
                                lastEntry.patternIds.contains(authoritativePatternId))
                    if (entry.isTypical == lastEntry.isTypical && !isAuthoritativePatternBoundary) {
                        wipSegment.add(entry)
                    } else {
                        segments.add(mutableListOf(entry))
                    }
                }
            }

            return segments.map {
                OldSegment(
                    it,
                    hasRouteLine =
                        authoritativePatternId == null ||
                            it.any { it.patternIds.contains(authoritativePatternId) },
                )
            }
        }
    }
}
