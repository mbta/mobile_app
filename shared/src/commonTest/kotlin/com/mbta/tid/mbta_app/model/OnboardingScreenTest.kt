package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.repositories.IAccessibilityStatusRepository
import com.mbta.tid.mbta_app.repositories.MockAccessibilityStatusRepository
import com.mbta.tid.mbta_app.repositories.Settings
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
        assertTrue(OnboardingScreen.Location.applies(undefinedAccessibilityStatus, emptyMap()))
    }

    @Test
    fun `HideMaps checks accessibility status`() {
        assertTrue(
            OnboardingScreen.HideMaps.applies(
                MockAccessibilityStatusRepository(isScreenReaderEnabled = true),
                emptyMap(),
            )
        )
        assertFalse(
            OnboardingScreen.HideMaps.applies(
                MockAccessibilityStatusRepository(isScreenReaderEnabled = false),
                emptyMap(),
            )
        )
    }

    @Test
    fun `NotificationsBeta checks notifications beta setting`() {
        assertTrue(
            OnboardingScreen.NotificationsBeta.applies(
                undefinedAccessibilityStatus,
                mapOf(Settings.Notifications to true),
            )
        )
        assertFalse(
            OnboardingScreen.NotificationsBeta.applies(
                undefinedAccessibilityStatus,
                mapOf(Settings.Notifications to false),
            )
        )
        assertFalse(
            OnboardingScreen.NotificationsBeta.applies(undefinedAccessibilityStatus, emptyMap())
        )
    }

    @Test
    fun `Feedback always applies`() {
        assertTrue(OnboardingScreen.Feedback.applies(undefinedAccessibilityStatus, emptyMap()))
    }
}
