package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.repositories.IAccessibilityStatusRepository
import com.mbta.tid.mbta_app.repositories.MockAccessibilityStatusRepository
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingScreenTest {
    private val undefinedAccessibilityStatus =
        object : IAccessibilityStatusRepository {
            override fun isScreenReaderEnabled(): Boolean {
                throw NotImplementedError()
            }
        }

    @Test
    fun `Location always applies`() {
        assertTrue(OnboardingScreen.Location.applies(undefinedAccessibilityStatus))
    }

    @Test
    fun `HideMaps checks accessibility status`() {
        assertTrue(
            OnboardingScreen.MapDisplay.applies(
                MockAccessibilityStatusRepository(isScreenReaderEnabled = true)
            )
        )
        assertFalse(
            OnboardingScreen.MapDisplay.applies(
                MockAccessibilityStatusRepository(isScreenReaderEnabled = false)
            )
        )
    }

    @Test
    fun `Feedback always applies`() {
        assertTrue(OnboardingScreen.Feedback.applies(undefinedAccessibilityStatus))
    }
}
