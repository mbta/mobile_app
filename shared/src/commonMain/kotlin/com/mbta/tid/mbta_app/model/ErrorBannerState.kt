package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.utils.EasternTimeInstant

public sealed class ErrorBannerState {
    /** What to do when the button in the error banner is pressed */
    public abstract val action: (() -> Unit)?

    public data class StalePredictions(
        val lastUpdated: EasternTimeInstant,
        override val action: () -> Unit,
    ) : ErrorBannerState() {
        public fun minutesAgo(): Long = (EasternTimeInstant.now() - lastUpdated).inWholeMinutes
    }

    public data class DataError(
        val messages: Set<String>,
        internal val details: Set<String>,
        override val action: () -> Unit,
    ) : ErrorBannerState()

    public data class NetworkError(override val action: (() -> Unit)?) : ErrorBannerState()
}
