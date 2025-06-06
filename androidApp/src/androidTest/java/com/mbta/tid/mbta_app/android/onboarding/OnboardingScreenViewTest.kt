package com.mbta.tid.mbta_app.android.onboarding

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.mbta.tid.mbta_app.android.hasRole
import com.mbta.tid.mbta_app.android.location.MockLocationDataManager
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.model.OnboardingScreen
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.viewModel.SettingsViewModel
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
        val locationDataManager = MockLocationDataManager()
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
                substring = true,
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
                settings = mapOf(Settings.StationAccessibility to false),
                onSaveSettings = { settings ->
                    assertTrue(settings.getValue(Settings.StationAccessibility))
                    savedSetting = true
                },
            )
        val settingsCache = SettingsCache(SettingsViewModel(settingsRepo))
        var advanced = false
        composeTestRule.setContent {
            OnboardingScreenView(
                screen = OnboardingScreen.StationAccessibility,
                advance = { advanced = true },
                locationDataManager = MockLocationDataManager(),
                settingsCache = settingsCache,
            )
        }

        composeTestRule
            .onNodeWithText("Set station accessibility info preference")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                " we can show you which stations are inaccessible or have elevator closures.",
                substring = true,
            )
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").assertIsDisplayed()
        composeTestRule
            .onNode(hasText("Station Accessibility Info") and hasRole(Role.Switch))
            .performClick()

        composeTestRule.waitForIdle()
        assertTrue(savedSetting)
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.waitForIdle()

        assertTrue(advanced)
    }

    @Test
    fun testMapDisplayFlow() {
        var savedSetting = false
        val settingsRepo =
            MockSettingsRepository(
                settings = mapOf(Settings.HideMaps to false),
                onSaveSettings = {
                    assertEquals(mapOf(Settings.HideMaps to false), it)
                    savedSetting = true
                },
            )
        val settingsCache = SettingsCache(SettingsViewModel(settingsRepo))
        var advanced = false
        composeTestRule.setContent {
            OnboardingScreenView(
                screen = OnboardingScreen.HideMaps,
                advance = { advanced = true },
                locationDataManager = MockLocationDataManager(),
                settingsCache = settingsCache,
            )
        }
        composeTestRule
            .onNodeWithText(
                "When using TalkBack, we can hide maps to make the app easier to navigate."
            )
            .assertIsDisplayed()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Map Display")
            .assertIsDisplayed()
            .assertIsOff()
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Map Display").assertIsOn()

        composeTestRule.onNodeWithText("Continue").assertIsDisplayed().performClick()
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
                locationDataManager = MockLocationDataManager(),
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
