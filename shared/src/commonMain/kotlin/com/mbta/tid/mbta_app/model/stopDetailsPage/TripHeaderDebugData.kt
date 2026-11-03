package com.mbta.tid.mbta_app.model.stopDetailsPage

import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.utils.EasternTimeInstant

public data class TripHeaderDebugData(val lines: List<String>) {
    public companion object {
        public fun from(
            trip: Trip,
            vehicleId: String?,
            vehicle: Vehicle?,
            now: EasternTimeInstant,
        ): TripHeaderDebugData =
            TripHeaderDebugData(
                buildList {
                    add("trip id: ${trip.id}")
                    add("pattern id: ${trip.routePatternId}")
                    (vehicleId ?: vehicle?.id)?.let { add("vehicle id: $it") }
                    vehicle?.let {
                        val seconds = (now - it.updatedAt).absoluteValue.inWholeSeconds
                        val minutesText = (seconds / 60).toString().padStart(2, '0')
                        val secondsText = (seconds % 60).toString().padStart(2, '0')
                        add("vehicle updated ${minutesText}m ${secondsText}s ago")
                        it.currentStopSequence?.let { sequence ->
                            add("current stop sequence: $sequence")
                        }
                    }
                },
            )
    }
}
