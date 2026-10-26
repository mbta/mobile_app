package com.mbta.tid.mbta_app.model

public sealed class TrackChange {

    /**
     * There is a track change alert for this stop. The new track number can't be determined from
     * the alert, so details are only available in the alert itself.
     */
    public data class Alert(val alert: com.mbta.tid.mbta_app.model.Alert) : TrackChange()

    public data class Scheduled(
        val platformCode: String,
        val platformName: String,
    ) : TrackChange()
}
