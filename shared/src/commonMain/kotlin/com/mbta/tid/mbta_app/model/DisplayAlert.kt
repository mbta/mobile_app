package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.utils.EasternTimeInstant

public enum class AlertCardSpec {
    Delay,
    Downstream,
    Elevator,
    Takeover,
    Regular,
}

public data class DisplayAlert(val alert: Alert, val isDownstream: Boolean = false) {

    public fun cardSpec(now: EasternTimeInstant, isAllServiceDisrupted: Boolean): AlertCardSpec {

        val significance = alert.significance(now)
        if (isDownstream) {
            return AlertCardSpec.Downstream
        } else if (significance == AlertSignificance.Major && isAllServiceDisrupted) {
            return AlertCardSpec.Takeover
        }
        if (significance == AlertSignificance.Minor && alert.effect == Alert.Effect.Delay) {
            return AlertCardSpec.Delay
        } else if (alert.effect == Alert.Effect.ElevatorClosure) {
            return AlertCardSpec.Elevator
        } else {
            return AlertCardSpec.Regular
        }
    }
}
