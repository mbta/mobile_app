package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Instant

data class StopDetailsDepartures(val routes: List<PatternsByStop>) {
    constructor(
        stop: Stop,
        global: GlobalResponse,
        schedules: ScheduleResponse?,
        predictions: PredictionsStreamDataResponse?,
        pinnedRoutes: Set<String>,
        filterAtTime: Instant
    ) : this(
        global.run {
            val loading = schedules == null || predictions == null
            val tripMapByHeadsign = tripMapByHeadsign(schedules, predictions)
            val cutoffTime = filterAtTime.plus(90.minutes)

            val allStopIds =
                if (patternIdsByStop.containsKey(stop.id)) {
                    setOf(stop.id)
                } else {
                    stop.childStopIds.toSet()
                }

            val patternsByRoute =
                allStopIds
                    .flatMap { patternIdsByStop[it] ?: emptyList() }
                    .map { patternId -> routePatterns.getValue(patternId) }
                    .groupBy { routes.getValue(it.routeId) }

            val touchedLines: MutableSet<String> = mutableSetOf()

            patternsByRoute
                .mapNotNull { (route, routePatterns) ->
                    if (touchedLines.contains(route.lineId)) {
                        return@mapNotNull null
                    } else if (NearbyStaticData.groupedLines.contains(route.lineId)) {
                        val line = global.lines[route.lineId] ?: return@mapNotNull null
                        touchedLines.add(line.id)

                        return@mapNotNull patternsByStopForLine(
                            stop,
                            line,
                            patternsByRoute,
                            tripMapByHeadsignOrDirection(tripMapByHeadsign, schedules, predictions),
                            allStopIds,
                            loading,
                            cutoffTime,
                            global
                        )
                    }

                    return@mapNotNull patternsByStopForRoute(
                        stop,
                        route,
                        routePatterns,
                        tripMapByHeadsign,
                        allStopIds,
                        loading,
                        cutoffTime,
                        global
                    )
                }
                .filterNot { it.patterns.isEmpty() }
                .sortedWith(
                    compareBy<PatternsByStop, Route>(Route.relevanceComparator(pinnedRoutes)) {
                            it.representativeRoute
                        }
                        .thenBy { it.representativeRoute }
                )
        }
    )

    companion object {

        private fun tripMapByHeadsign(
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse?,
        ): Map<RealtimePatterns.UpcomingTripKey.ByHeadsign, List<UpcomingTrip>>? {
            return UpcomingTrip.tripsMappedBy(
                schedules,
                predictions,
                scheduleKey = { schedule, scheduleData ->
                    val trip = scheduleData.trips.getValue(schedule.tripId)
                    RealtimePatterns.UpcomingTripKey.ByHeadsign(
                        schedule.routeId,
                        trip.headsign,
                        schedule.stopId
                    )
                },
                predictionKey = { prediction, streamData ->
                    val trip = streamData.trips.getValue(prediction.tripId)
                    RealtimePatterns.UpcomingTripKey.ByHeadsign(
                        prediction.routeId,
                        trip.headsign,
                        prediction.stopId
                    )
                }
            )
        }

        private fun tripMapByHeadsignOrDirection(
            tripMapByHeadsign:
                Map<RealtimePatterns.UpcomingTripKey.ByHeadsign, List<UpcomingTrip>>?,
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse?,
        ): Map<RealtimePatterns.UpcomingTripKey, List<UpcomingTrip>>? {
            val tripMapByDirection =
                UpcomingTrip.tripsMappedBy(
                    schedules,
                    predictions,
                    scheduleKey = { schedule, scheduleData ->
                        val trip = scheduleData.trips.getValue(schedule.tripId)
                        RealtimePatterns.UpcomingTripKey.ByDirection(
                            schedule.routeId,
                            trip.directionId,
                            schedule.stopId
                        )
                    },
                    predictionKey = { prediction, streamData ->
                        val trip = streamData.trips.getValue(prediction.tripId)
                        RealtimePatterns.UpcomingTripKey.ByDirection(
                            prediction.routeId,
                            trip.directionId,
                            prediction.stopId
                        )
                    }
                )

            return if (tripMapByHeadsign != null || tripMapByDirection != null) {
                (tripMapByHeadsign ?: emptyMap()) + (tripMapByDirection ?: emptyMap())
            } else {
                null
            }
        }

        private fun patternsByStopForRoute(
            stop: Stop,
            route: Route,
            routePatterns: List<RoutePattern>,
            tripMap: Map<RealtimePatterns.UpcomingTripKey.ByHeadsign, List<UpcomingTrip>>?,
            allStopIds: Set<String>,
            loading: Boolean,
            cutoffTime: Instant,
            global: GlobalResponse
        ): PatternsByStop {
            global.run {
                val patternsByHeadsign =
                    routePatterns.groupBy {
                        val representativeTrip = trips.getValue(it.representativeTripId)
                        representativeTrip.headsign
                    }

                return PatternsByStop(
                    listOf(route),
                    null,
                    stop,
                    patternsByHeadsign
                        .map { (headsign, patterns) ->
                            val upcomingTrips =
                                if (tripMap != null) {
                                    allStopIds
                                        .map {
                                            RealtimePatterns.UpcomingTripKey.ByHeadsign(
                                                route.id,
                                                headsign,
                                                it
                                            )
                                        }
                                        .flatMap { tripMap[it] ?: emptyList() }
                                        .sorted()
                                } else {
                                    null
                                }
                            RealtimePatterns.ByHeadsign(
                                route,
                                headsign,
                                null,
                                patterns,
                                upcomingTrips
                            )
                        }
                        .filter {
                            loading ||
                                ((it.isTypical() || it.isUpcomingBefore(cutoffTime)) &&
                                    !it.isArrivalOnly())
                        }
                        .sorted(),
                    Direction.getDirections(global, stop, route, routePatterns)
                )
            }
        }

        private fun patternsByStopForLine(
            stop: Stop,
            line: Line,
            patternsByRoute: Map<Route, List<RoutePattern>>,
            tripMap: Map<RealtimePatterns.UpcomingTripKey, List<UpcomingTrip>>?,
            allStopIds: Set<String>,
            loading: Boolean,
            cutoffTime: Instant,
            global: GlobalResponse
        ): PatternsByStop {
            global.run {
                val groupedPatternsByRoute = patternsByRoute.filter { it.key.lineId == line.id }

                val staticPatterns =
                    NearbyStaticData.buildStopPatternsForLine(
                        stop,
                        groupedPatternsByRoute,
                        line,
                        allStopIds,
                        global
                    )

                val realtimePatterns =
                    staticPatterns.patterns
                        .map {
                            when (it) {
                                is NearbyStaticData.StaticPatterns.ByHeadsign ->
                                    RealtimePatterns.ByHeadsign(it, tripMap, allStopIds, null)
                                is NearbyStaticData.StaticPatterns.ByDirection ->
                                    RealtimePatterns.ByDirection(it, tripMap, allStopIds, null)
                            }
                        }
                        .filter {
                            loading ||
                                ((it.isTypical() || it.isUpcomingBefore(cutoffTime)) &&
                                    !it.isArrivalOnly())
                        }
                        .sorted()

                return PatternsByStop(
                    groupedPatternsByRoute.map { it.key },
                    line,
                    stop,
                    realtimePatterns,
                    Direction.getDirectionsForLine(
                        global,
                        stop,
                        realtimePatterns.flatMap { it.patterns }
                    )
                )
            }
        }
    }
}
