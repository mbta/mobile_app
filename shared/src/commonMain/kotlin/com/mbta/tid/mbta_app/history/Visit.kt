package com.mbta.tid.mbta_app.history

import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.serialization.Serializable

@Serializable
public sealed class Visit {
    public val timestamp: EasternTimeInstant = EasternTimeInstant.now()

    @Serializable public data class StopVisit(val stopId: String) : Visit()
}
