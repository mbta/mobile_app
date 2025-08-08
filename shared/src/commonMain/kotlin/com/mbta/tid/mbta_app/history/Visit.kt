package com.mbta.tid.mbta_app.history

import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.serialization.Serializable

@Serializable
public sealed class Visit {
    internal val timestamp: EasternTimeInstant = EasternTimeInstant.now()

    @Serializable public data class StopVisit(internal val stopId: String) : Visit()
}
