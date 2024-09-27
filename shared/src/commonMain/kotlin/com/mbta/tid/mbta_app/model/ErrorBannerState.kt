package com.mbta.tid.mbta_app.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

sealed class ErrorBannerState {
    /** What to do when the button in the error banner is pressed */
    abstract val action: () -> Unit

    data class StalePredictions(val lastUpdated: Instant, override val action: () -> Unit) :
        ErrorBannerState() {
        fun minutesAgo() = (Clock.System.now() - lastUpdated).inWholeMinutes
    }
}
