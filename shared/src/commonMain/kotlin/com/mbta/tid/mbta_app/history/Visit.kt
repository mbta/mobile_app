package com.mbta.tid.mbta_app.history

import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.serialization.Serializable

@Serializable
sealed class Visit {
    val timestamp: EasternTimeInstant = EasternTimeInstant.now()

    @Serializable data class StopVisit(val stopId: String) : Visit()
}
