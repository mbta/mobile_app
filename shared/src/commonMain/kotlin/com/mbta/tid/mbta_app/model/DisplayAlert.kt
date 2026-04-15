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
        tripId: String?,
    ): AlertCardSpec {

        val significanceNow = alert.significance(now)
        return if (isDownstream) {
            AlertCardSpec.Downstream
        } else if (
            significanceNow == AlertSignificance.Major && (isAllServiceDisrupted) ||
                // May be looking at a trip in the future, so use the intrinsic significance instead
                // of significance now.
                (tripId != null &&
                    alert.anyInformedEntitySatisfies { checkTripStrict(tripId) } &&
                    alert.significance(null) == AlertSignificance.Major)
        ) {
            AlertCardSpec.Takeover
        } else if (
            significanceNow == AlertSignificance.Minor && alert.effect == Alert.Effect.Delay
        ) {
            AlertCardSpec.Delay
        } else if (alert.effect == Alert.Effect.ElevatorClosure) {
            AlertCardSpec.Elevator
        } else {
            AlertCardSpec.Basic
        }
    }
}
