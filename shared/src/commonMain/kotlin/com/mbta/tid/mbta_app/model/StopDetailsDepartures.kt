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
        filterAtTime: Instant
    ) : this(
        global.run {
            data class UpcomingTripKey(val routeId: String, val headsign: String?)

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
                    // TODO use stop children directly
                    stops.filterValues { it.parentStationId == stop.id }.keys
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
                        route,
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

                                PatternsByHeadsign(route, headsign, patterns, upcomingTrips)
                            }
                            .filter {
                                (it.isTypical() || it.isUpcomingBefore(cutoffTime)) &&
                                    !it.isArrivalOnly()
                            }
                            .sorted()
                    )
                }
                .filterNot { it.patternsByHeadsign.isEmpty() }
                .sortedWith(
                    (compareBy<PatternsByStop, Route>(Route.subwayFirstComparator) { it.route })
                        .then(compareBy { it.route })
                )
        }
    )
}
