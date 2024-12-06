package com.mbta.tid.mbta_app.android.pages

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

        composeTestRule.onNodeWithText("MBTA Go").assertIsDisplayed()
    }

    @Test
    fun testSettings() {

        var hideMapToggleCalled = false

        val koinApplication = koinApplication {
            modules(
                module {
                    single<ISettingsRepository> {
                        MockSettingsRepository(
                            onSaveSettings = { settings ->
                                if (settings.size == 1 && settings.containsKey(Settings.HideMaps)) {
                                    hideMapToggleCalled = true
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

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hide Maps").performClick()

        assertTrue { hideMapToggleCalled }
    }
}
