package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.RouteStopsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RouteDetailsStopList(val directionId: Int, val segments: List<Segment>) {

    /** A subset of consecutive stops that are all typical or all non-typical. */
    data class Segment(val stops: List<Entry>, val hasRouteLine: Boolean) {
        val isTypical = stops.all { it.isTypical }
    }

    data class Entry(
        val stop: Stop,
        val patterns: List<RoutePattern>,
        val connectingRoutes: List<Route>,
    ) {
        val patternIds = patterns.map { it.id }.toSet()
        val isTypical = patterns.any { it.isTypical() }
    }

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

        suspend fun fromPieces(
            routeId: String,
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
                        Entry(stop, patterns, transferRoutes)
                    }

                val segments = splitIntoSegments(stops)

                RouteDetailsStopList(directionId, segments)
            }

        /**
         * Split the list of entries into segments based on whether the stop serves a typical route
         * pattern.
         */
        private fun splitIntoSegments(entries: List<Entry>): List<Segment> {
            val authoritativePatternId =
                entries
                    .flatMapTo(mutableSetOf()) { it.patterns.filter(RoutePattern::isTypical) }
                    .minOrNull()
                    ?.id

            val segments: MutableList<MutableList<Entry>> = mutableListOf()

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
                Segment(
                    it,
                    hasRouteLine =
                        authoritativePatternId == null ||
                            it.any { it.patternIds.contains(authoritativePatternId) },
                )
            }
        }
    }
}
