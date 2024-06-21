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
            data class UpcomingTripKey(val routeId: String, val headsign: String?)

            val loading = schedules == null || predictions == null
            val upcomingTripsByHeadsignAndStop =
                UpcomingTrip.tripsMappedBy(
                    schedules,
                    predictions,
                    scheduleKey = { schedule, scheduleData ->
                        val trip = scheduleData.trips.getValue(schedule.tripId)
                        UpcomingTripKey(schedule.routeId, trip.headsign)
                    },
                    predictionKey = { prediction, streamData ->
                        val trip = streamData.trips.getValue(prediction.tripId)
                        UpcomingTripKey(prediction.routeId, trip.headsign)
                    }
                )
            val cutoffTime = filterAtTime.plus(90.minutes)

            val allStopIds =
                if (patternIdsByStop.containsKey(stop.id)) {
                    listOf(stop.id)
                } else {
                    stop.childStopIds
                }

            val patternsByRoute =
                allStopIds
                    .flatMap { patternIdsByStop[it] ?: emptyList() }
                    .map { patternId -> routePatterns.getValue(patternId) }
                    .groupBy { routes.getValue(it.routeId) }

            patternsByRoute
                .map { (route, routePatterns) ->
                    val patternsByHeadsign =
                        routePatterns.groupBy {
                            val representativeTrip = trips.getValue(it.representativeTripId)
                            representativeTrip.headsign
                        }

                    PatternsByStop(
                        listOf(route),
                        null,
                        stop,
                        patternsByHeadsign
                            .map { (headsign, patterns) ->
                                val upcomingTrips =
                                    if (upcomingTripsByHeadsignAndStop != null) {
                                        val tripKey = UpcomingTripKey(route.id, headsign)
                                        upcomingTripsByHeadsignAndStop[tripKey] ?: emptyList()
                                    } else {
                                        null
                                    }
                                Patterns.ByHeadsign(route, headsign, patterns, upcomingTrips)
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
                .filterNot { it.patterns.isEmpty() }
                .sortedWith(compareBy(Route.relevanceComparator(pinnedRoutes)) { it.routes.min() })
        }
    )
}
