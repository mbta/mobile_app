package com.mbta.tid.mbta_app.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class ErrorBannerStateTest {
    @Test
    fun `StalePredictions calculates minutes ago`() {
        val error = ErrorBannerState.StalePredictions(Clock.System.now() - 3.minutes) {}
        assertEquals(3, error.minutesAgo())
    }
}
