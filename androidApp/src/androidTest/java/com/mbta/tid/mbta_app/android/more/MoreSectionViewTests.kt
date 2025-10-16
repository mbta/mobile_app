package com.mbta.tid.mbta_app.android.more

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.model.morePage.MoreItem
import com.mbta.tid.mbta_app.model.morePage.MoreSection
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.SharedString
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
                settingsCache =
                    SettingsCache(
                        MockSettingsRepository(onSaveSettings = { toggleCallbackCalled = true })
                    ),
            )
        }

        composeTestRule.onNodeWithText("Feature Flags").assertIsDisplayed()
        composeTestRule.onNodeWithText("Map Display").performClick()
        composeTestRule.waitForIdle()

        assertTrue { toggleCallbackCalled }
    }
}
