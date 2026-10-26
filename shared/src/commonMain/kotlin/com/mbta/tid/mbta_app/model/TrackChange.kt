package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse

public sealed class TrackChange {

    /**
     * There is a track change alert for this stop. The new track number can't be determined from
     * the alert, so details are only available in the alert itself.
     */
    public data class Alert(val alert: com.mbta.tid.mbta_app.model.Alert) : TrackChange()

    public data class Scheduled(
        val platformCode: String,
        val platformName: String?,
    ) : TrackChange()

    public companion object {
        public fun calculate(
            alerts: Map<String, com.mbta.tid.mbta_app.model.Alert>,
            schedule: Schedule?,
            prediction: Prediction?,
            trip: Trip,
            stopIds: Set<String>,
            globalData: GlobalResponse,
        ): TrackChange? {
            val trackChangeAlert =
                com.mbta.tid.mbta_app.model.Alert.trackChangeAlert(
                    alerts.values,
                    schedule?.departureBasedStopTime ?: prediction?.departureBasedStopTime,
                    trip.directionId,
                    trip.routeId,
                    stopIds,
                    trip.id,
                )

            if (trackChangeAlert != null) {
                return Alert(trackChangeAlert)
            }

            if (schedule != null && schedule.isScheduledTrackChange(globalData)) {
                val stop = globalData.stops[schedule.stopId]
                return stop?.platformCode?.let { TrackChange.Scheduled(it, stop.platformName) }
            } else {
                return null
            }
        }
    }
}
