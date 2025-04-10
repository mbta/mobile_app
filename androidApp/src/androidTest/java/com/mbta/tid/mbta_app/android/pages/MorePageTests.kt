package com.mbta.tid.mbta_app.android.pages

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.mbta.tid.mbta_app.model.Dependency
import com.mbta.tid.mbta_app.model.getAllDependencies
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.KoinTest

class MorePageTests : KoinTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testNearbyTransitPageDisplaysCorrectly() {
        composeTestRule.setContent { MorePage(bottomBar = {}) }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MBTA Go").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testSettings() {
        var hideMapValue = false

        val koinApplication = koinApplication {
            modules(
                module {
                    single<ISettingsRepository> {
                        MockSettingsRepository(
                            onSaveSettings = { settings ->
                                if (settings.size == 1 && settings.containsKey(Settings.HideMaps)) {
                                    hideMapValue = settings.getValue(Settings.HideMaps)
                                }
                            }
                        )
                    }
                }
            )
        }
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) { MorePage(bottomBar = {}) }
        }

        composeTestRule.waitForIdle()
        composeTestRule.waitUntilExactlyOneExists(hasText("Settings"))
        composeTestRule.onNodeWithText("Settings").performScrollTo()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Map Display").assertIsOn().performClick()

        composeTestRule.waitForIdle()
        composeTestRule.waitUntil { hideMapValue }

        assertTrue { hideMapValue }
        composeTestRule.onNodeWithText("Map Display").assertIsOff()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLinksExist() {
        val koinApplication = koinApplication {
            modules(
                module {
                    single<ISettingsRepository> { MockSettingsRepository(onSaveSettings = {}) }
                }
            )
        }
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) { MorePage(bottomBar = {}) }
        }

        composeTestRule.waitForIdle()
        composeTestRule.waitUntilExactlyOneExists(hasText("Send app feedback"))
        composeTestRule.onNodeWithText("Send app feedback").assertIsDisplayed()
        composeTestRule.onNodeWithText("Trip Planner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fare Information").assertIsDisplayed()
        composeTestRule.onNodeWithText("Commuter Rail and Ferry tickets").performScrollTo()
        composeTestRule.onNodeWithText("Commuter Rail and Ferry tickets").assertIsDisplayed()
        composeTestRule.onNodeWithText("Terms of Use").performScrollTo()
        composeTestRule.onNodeWithText("Terms of Use").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy Policy").performScrollTo()
        composeTestRule.onNodeWithText("Privacy Policy").assertIsDisplayed()
        composeTestRule.onNode(hasText("View source on GitHub")).performScrollTo()
        composeTestRule.onNodeWithText("View source on GitHub").assertIsDisplayed()
        composeTestRule.onNodeWithText("Software Licenses").performScrollTo()
        composeTestRule.onNodeWithText("Software Licenses").assertIsDisplayed()
        composeTestRule.onNode(hasText("617-222-3200")).performScrollTo()
        composeTestRule.onNodeWithText("617-222-3200").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testSoftwareLicenses() {
        val koinApplication = koinApplication {
            modules(
                module {
                    single<ISettingsRepository> { MockSettingsRepository(onSaveSettings = {}) }
                }
            )
        }
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) { MorePage(bottomBar = {}) }
        }

        val dependencies = Dependency.getAllDependencies()
        val dependency = dependencies.first()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilExactlyOneExists(hasText("Send app feedback"))
        composeTestRule.onNodeWithText("Software Licenses").performScrollTo()
        composeTestRule.onNodeWithText("Software Licenses").performClick()
        composeTestRule.waitUntilExactlyOneExists(hasText(dependency.name))
        composeTestRule.onNodeWithText(dependency.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(dependency.name).performClick()
        composeTestRule.waitUntilExactlyOneExists(hasText(dependency.licenseText))
        composeTestRule.onNodeWithText(dependency.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(dependency.licenseText).assertIsDisplayed()
    }
}
