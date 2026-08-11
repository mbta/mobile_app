package com.mbta.tid.mbta_app.android.more

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.testUtils.assertCanBeDisplayed
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.model.morePage.MoreItem
import com.mbta.tid.mbta_app.model.morePage.MoreSection
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.SharedString
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class MoreSectionViewTests {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testToggleItem() {
        var toggleCallbackCalled = false
        composeTestRule.setContent {
            MoreSectionView(
                section =
                    MoreSection(
                        MoreSection.Category.Settings,
                        SharedString.FeatureFlagsSection,
                        listOf(MoreItem.Toggle(SharedString.MapDisplay, Settings.HideMaps)),
                    ),
                highlighted = false,
                settingsCache =
                    SettingsCache(
                        MockSettingsRepository(onSaveSettings = { toggleCallbackCalled = true })
                    ),
            )
        }

        composeTestRule.onNodeWithText("Feature Flags").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Map Display").performClick()
        composeTestRule.waitForIdle()

        assertTrue { toggleCallbackCalled }
    }

    @Test
    fun testOnChangeSetting() {
        var wasWritten = false
        var onChangeArgs: Pair<Settings, Boolean>? = null
        composeTestRule.setContent {
            MoreSectionView(
                section =
                    MoreSection(
                        MoreSection.Category.PublicBetas,
                        SharedString.BetaSection,
                        listOf(MoreItem.Toggle(SharedString.Notifications, Settings.Notifications)),
                    ),
                highlighted = false,
                settingsCache =
                    SettingsCache(
                        MockSettingsRepository(
                            onSaveSettings = {
                                assertNull(onChangeArgs)
                                wasWritten = true
                            }
                        )
                    ),
                onChangeSetting = { setting, newValue ->
                    assertTrue(wasWritten)
                    onChangeArgs = Pair(setting, newValue)
                },
            )
        }

        composeTestRule.onNodeWithText("Test New Features").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Disruption Notifications").performClick()
        composeTestRule.waitForIdle()

        assertTrue(wasWritten)
        assertEquals(Pair(Settings.Notifications, true), onChangeArgs)
    }
}
