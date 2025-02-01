package com.mbta.tid.mbta_app.android.util

import androidx.annotation.StringRes
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.Alert

data class FormattedAlert(@StringRes val effect: Int) {
    companion object {
        fun format(alert: Alert): FormattedAlert? {
            // TODO: Add all possible alert effects if/when we start displaying non-disruption
            // alerts here
            val effect =
                when (alert.effect) {
                    Alert.Effect.Detour -> R.string.detour
                    Alert.Effect.DockClosure -> R.string.dock_closure
                    Alert.Effect.ElevatorClosure -> R.string.elevator_closure
                    Alert.Effect.ServiceChange -> R.string.service_change
                    Alert.Effect.Shuttle -> R.string.shuttle
                    Alert.Effect.StationClosure -> R.string.station_closure
                    Alert.Effect.StopClosure -> R.string.stop_closure
                    Alert.Effect.StopMove,
                    Alert.Effect.StopMoved -> R.string.station_moved
                    Alert.Effect.Suspension -> R.string.suspension
                    else -> return null
                }
            return FormattedAlert(effect)
        }
    }
}
