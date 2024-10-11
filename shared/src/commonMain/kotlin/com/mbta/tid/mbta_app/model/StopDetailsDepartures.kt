package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
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
            val allStopIds =
                if (patternIdsByStop.containsKey(stop.id)) {
                    listOf(stop.id)
                } else {
                    stop.childStopIds.filter { global.stops.containsKey(it) }
                }

            val staticData = NearbyStaticData(global, NearbyResponse(allStopIds))

            staticData
                .withRealtimeInfo(
                    global,
                    null,
                    schedules,
                    predictions,
                    alerts,
                    filterAtTime,
                    showAllPatternsWhileLoading = true,
                    hideNonTypicalPatternsBeyondNext = null,
                    filterCancellations = false,
                    pinnedRoutes
                )
                .flatMap { it.patternsByStop }
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
}
