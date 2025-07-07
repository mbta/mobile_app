package com.mbta.tid.mbta_app.model

import kotlin.time.Clock
import kotlin.time.Instant

sealed class ErrorBannerState {
    /** What to do when the button in the error banner is pressed */
    abstract val action: (() -> Unit)?

    data class StalePredictions(val lastUpdated: Instant, override val action: () -> Unit) :
        ErrorBannerState() {
        fun minutesAgo() = (Clock.System.now() - lastUpdated).inWholeMinutes
    }

    data class DataError(val messages: Set<String>, override val action: () -> Unit) :
        ErrorBannerState()

    data class NetworkError(override val action: (() -> Unit)?) : ErrorBannerState()
}
