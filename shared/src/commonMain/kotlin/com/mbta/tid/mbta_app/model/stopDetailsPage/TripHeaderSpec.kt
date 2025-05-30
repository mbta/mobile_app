package com.mbta.tid.mbta_app.model.stopDetailsPage

import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.Vehicle

sealed class TripHeaderSpec {
    data object FinishingAnotherTrip : TripHeaderSpec()

    data object NoVehicle : TripHeaderSpec()

    data class Scheduled(val stop: Stop, val entry: TripDetailsStopList.Entry) : TripHeaderSpec()

    data class VehicleOnTrip(
        val vehicle: Vehicle,
        val stop: Stop,
        val entry: TripDetailsStopList.Entry?,
        val atTerminal: Boolean,
    ) : TripHeaderSpec()

    companion object {
        fun getSpec(
            tripId: String,
            stops: TripDetailsStopList,
            terminalStop: Stop?,
            vehicle: Vehicle?,
            vehicleStop: Stop?,
        ): TripHeaderSpec? {
            return if (vehicle != null && vehicleStop != null) {
                val atTerminal =
                    terminalStop != null &&
                        terminalStop.id == vehicleStop.id &&
                        vehicle.currentStatus == Vehicle.CurrentStatus.StoppedAt
                if (vehicle.tripId == tripId) {
                    VehicleOnTrip(
                        vehicle,
                        vehicleStop,
                        if (atTerminal) stops.startTerminalEntry
                        else stops.stops.firstOrNull { it.stop.id == vehicleStop.id },
                        atTerminal,
                    )
                } else {
                    FinishingAnotherTrip
                }
            } else if (stops.stops.any { it.prediction != null }) {
                NoVehicle
            } else if (terminalStop != null && stops.startTerminalEntry != null) {
                Scheduled(terminalStop, stops.startTerminalEntry)
            } else {
                null
            }
        }
    }
}
