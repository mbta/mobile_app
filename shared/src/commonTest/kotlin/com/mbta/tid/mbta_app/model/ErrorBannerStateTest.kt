package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class ErrorBannerStateTest {
    @Test
    fun `StalePredictions calculates minutes ago`() {
        val error = ErrorBannerState.StalePredictions(EasternTimeInstant.now() - 3.minutes) {}
        assertEquals(3, error.minutesAgo())
    }
}
