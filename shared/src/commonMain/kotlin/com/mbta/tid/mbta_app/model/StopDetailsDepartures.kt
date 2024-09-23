package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import kotlinx.datetime.Instant

data class StopDetailsDepartures(val routes: List<PatternsByStop>) {
    constructor(
        stop: Stop,
        global: GlobalResponse,
        schedules: ScheduleResponse?,
        predictions: PredictionsStreamDataResponse?,
        alerts: AlertsStreamDataResponse?,
        pinnedRoutes: Set<String>,
        filterAtTime: Instant
    ) : this(
        global.run {
            val loading = schedules == null || predictions == null
            val tripMapByHeadsign = tripMapByHeadsign(schedules, predictions, filterAtTime)

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

            val activeRelevantAlerts =
                alerts?.alerts?.values?.filter {
                    it.isActive(filterAtTime) && it.significance >= AlertSignificance.Minor
                }
            val hasSchedulesTodayByPattern = NearbyStaticData.getSchedulesTodayByPattern(schedules)

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
                            tripMapByHeadsignOrDirection(
                                tripMapByHeadsign,
                                schedules,
                                predictions,
                                filterAtTime
                            ),
                            allStopIds,
                            loading,
                            global,
                            activeRelevantAlerts,
                            hasSchedulesTodayByPattern
                        )
                    }

                    return@mapNotNull patternsByStopForRoute(
                        stop,
                        route,
                        routePatterns,
                        tripMapByHeadsign,
                        allStopIds,
                        loading,
                        global,
                        activeRelevantAlerts,
                        hasSchedulesTodayByPattern
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

    fun autoFilter(): StopDetailsFilter? {
        if (routes.size != 1) {
            return null
        }
        val route = routes.first()
        val directions = route.patterns.map { it.directionId() }.toSet()
        if (directions.size != 1) {
            return null
        }
        val direction = directions.first()
        return StopDetailsFilter(route.routeIdentifier, direction)
    }

    companion object {

        private fun tripMapByHeadsign(
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse?,
            filterAtTime: Instant
        ): Map<RealtimePatterns.UpcomingTripKey.ByRoutePattern, List<UpcomingTrip>>? {
            return UpcomingTrip.tripsMappedBy(
                schedules,
                predictions,
                scheduleKey = { schedule, scheduleData ->
                    val trip = scheduleData.trips.getValue(schedule.tripId)
                    RealtimePatterns.UpcomingTripKey.ByRoutePattern(
                        schedule.routeId,
                        trip.routePatternId,
                        schedule.stopId
                    )
                },
                predictionKey = { prediction, streamData ->
                    val trip = streamData.trips.getValue(prediction.tripId)
                    RealtimePatterns.UpcomingTripKey.ByRoutePattern(
                        prediction.routeId,
                        trip.routePatternId,
                        prediction.stopId
                    )
                },
                filterAtTime
            )
        }

        private fun tripMapByHeadsignOrDirection(
            tripMapByRoutePattern:
                Map<RealtimePatterns.UpcomingTripKey.ByRoutePattern, List<UpcomingTrip>>?,
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse?,
            filterAtTime: Instant
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
                    },
                    filterAtTime
                )

            return if (tripMapByRoutePattern != null || tripMapByDirection != null) {
                (tripMapByRoutePattern ?: emptyMap()) + (tripMapByDirection ?: emptyMap())
            } else {
                null
            }
        }

        private fun patternsByStopForRoute(
            stop: Stop,
            route: Route,
            routePatterns: List<RoutePattern>,
            tripMap: Map<RealtimePatterns.UpcomingTripKey.ByRoutePattern, List<UpcomingTrip>>?,
            allStopIds: Set<String>,
            loading: Boolean,
            global: GlobalResponse,
            alerts: Collection<Alert>?,
            hasSchedulesTodayByPattern: Map<String, Boolean>?,
        ): PatternsByStop {
            global.run {
                val patternsByHeadsign =
                    routePatterns.groupBy {
                        routePatterns.groupBy { trips.getValue(it.representativeTripId) }
                        val representativeTrip = trips.getValue(it.representativeTripId)
                        representativeTrip.headsign
                    }
                return PatternsByStop(
                    listOf(route),
                    null,
                    stop,
                    patternsByHeadsign
                        .map { (headsign, patterns) ->
                            val stopIdsOnPatterns =
                                NearbyStaticData.filterStopsByPatterns(patterns, global, allStopIds)
                            val upcomingTrips =
                                if (tripMap != null) {
                                    stopIdsOnPatterns
                                        .map { stopId ->
                                            patterns
                                                .mapNotNull { pattern ->
                                                    tripMap[
                                                        RealtimePatterns.UpcomingTripKey
                                                            .ByRoutePattern(
                                                                route.id,
                                                                pattern.id,
                                                                stopId
                                                            )]
                                                }
                                                .flatten()
                                        }
                                        .flatten()
                                        .sorted()
                                } else {
                                    null
                                }
                            RealtimePatterns.ByHeadsign(
                                route,
                                headsign,
                                null,
                                patterns,
                                upcomingTrips,
                                alerts?.let {
                                    RealtimePatterns.applicableAlerts(
                                        routes = listOf(route),
                                        stopIds = stopIdsOnPatterns,
                                        alerts = alerts
                                    )
                                },
                                RealtimePatterns.hasSchedulesToday(
                                    hasSchedulesTodayByPattern,
                                    patterns
                                )
                            )
                        }
                        .filter {
                            loading || ((it.isTypical() || it.isUpcoming()) && !it.isArrivalOnly())
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
            global: GlobalResponse,
            alerts: Collection<Alert>?,
            hasSchedulesTodayByPattern: Map<String, Boolean>?,
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
                                    RealtimePatterns.ByHeadsign(
                                        it,
                                        tripMap,
                                        alerts,
                                        hasSchedulesTodayByPattern
                                    )
                                is NearbyStaticData.StaticPatterns.ByDirection ->
                                    RealtimePatterns.ByDirection(
                                        it,
                                        tripMap,
                                        alerts,
                                        hasSchedulesTodayByPattern
                                    )
                            }
                        }
                        .filter {
                            loading || ((it.isTypical() || it.isUpcoming()) && !it.isArrivalOnly())
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
