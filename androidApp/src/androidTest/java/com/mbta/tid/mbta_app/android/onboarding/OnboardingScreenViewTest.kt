package com.mbta.tid.mbta_app.android.onboarding

import android.location.Location
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.mbta.tid.mbta_app.android.location.MockLocationDataManager
import com.mbta.tid.mbta_app.model.OnboardingScreen
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingScreenViewTest {
    @get:Rule val composeTestRule = createComposeRule()

    // We can't easily mock the permission request, so we grant the permission eagerly.
    @get:Rule
    val runtimePermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)

    @Test
    fun testLocationFlow() {
        var advanced = false
        val locationDataManager = MockLocationDataManager(Location("mock"))
        composeTestRule.setContent {
            OnboardingScreenView(
                screen = OnboardingScreen.Location,
                advance = { advanced = true },
                locationDataManager = locationDataManager,
            )
        }

        composeTestRule
            .onNodeWithText(
                "We use your location to show you nearby transit options.",
                substring = true
            )
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").performClick()

        composeTestRule.waitForIdle()
        assertTrue(advanced)
    }

    @Test
    fun testStationAccessibilityFlow() {
        var savedSetting = false
        val settingsRepo =
            MockSettingsRepository(
                settings = mapOf(Settings.ElevatorAccessibility to false),
                onSaveSettings = {
                    assertEquals(mapOf(Settings.ElevatorAccessibility to true), it)
                    savedSetting = true
                }
            )
        var advanced = false
        composeTestRule.setContent {
            OnboardingScreenView(
                screen = OnboardingScreen.StationAccessibility,
                advance = { advanced = true },
                locationDataManager = MockLocationDataManager(Location("mock")),
                settingsRepository = settingsRepo
            )
        }

        composeTestRule.onNodeWithText("Know about elevator closures").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("We can tell you when elevators are closed at a station.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip").assertIsDisplayed()
        composeTestRule.onNodeWithText("Show elevator closures").performClick()

        composeTestRule.waitForIdle()
        assertTrue(savedSetting)
        assertTrue(advanced)
    }

    @Test
    fun testHideMapsFlow() {
        var savedSetting = false
        val settingsRepo =
            MockSettingsRepository(
                settings = mapOf(Settings.HideMaps to false),
                onSaveSettings = {
                    assertEquals(mapOf(Settings.HideMaps to true), it)
                    savedSetting = true
                }
            )
        var advanced = false
        composeTestRule.setContent {
            OnboardingScreenView(
                screen = OnboardingScreen.HideMaps,
                advance = { advanced = true },
                locationDataManager = MockLocationDataManager(Location("mock")),
                settingsRepository = settingsRepo
            )
        }
        composeTestRule
            .onNodeWithText(
                "When using TalkBack, we can skip reading out maps to keep you focused on transit information."
            )
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Show maps").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hide maps").performClick()

        composeTestRule.waitForIdle()
        assertTrue(savedSetting)
        assertTrue(advanced)
    }

    @Test
    fun testFeedbackFlow() {
        var advanced = false
        composeTestRule.setContent {
            OnboardingScreenView(
                screen = OnboardingScreen.Feedback,
                advance = { advanced = true },
                locationDataManager = MockLocationDataManager(Location("mock")),
            )
        }
        composeTestRule
            .onNodeWithText(
                "MBTA Go is in the early stages! We want your feedback" +
                    " as we continue making improvements and adding new features."
            )
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Get started").performClick()

        composeTestRule.waitForIdle()
        assertTrue(advanced)
    }
}
