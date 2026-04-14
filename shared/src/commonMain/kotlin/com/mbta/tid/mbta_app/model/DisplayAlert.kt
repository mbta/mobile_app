package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.utils.EasternTimeInstant

public enum class AlertCardSpec {
    Delay,
    Downstream,
    Elevator,
    Takeover,
    Basic,
}

public data class DisplayAlert(val alert: Alert, val isDownstream: Boolean = false) {

    val id: String = alert.id

    public fun cardSpec(
        now: EasternTimeInstant,
        isAllServiceDisrupted: Boolean,
        tripId: String? = null,
    ): AlertCardSpec {

        val significance = alert.significance(now, tripId = tripId)
        return if (isDownstream) {
            AlertCardSpec.Downstream
        } else if (
            significance == AlertSignificance.Major &&
                (isAllServiceDisrupted || alert.anyInformedEntitySatisfies { checkTrip(tripId) })
        ) {
            AlertCardSpec.Takeover
        } else if (significance == AlertSignificance.Minor && alert.effect == Alert.Effect.Delay) {
            AlertCardSpec.Delay
        } else if (alert.effect == Alert.Effect.ElevatorClosure) {
            AlertCardSpec.Elevator
        } else {
            AlertCardSpec.Basic
        }
    }
}
