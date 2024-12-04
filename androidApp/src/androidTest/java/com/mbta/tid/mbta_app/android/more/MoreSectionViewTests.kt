package com.mbta.tid.mbta_app.android.more

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.model.morePage.MoreItem
import com.mbta.tid.mbta_app.model.morePage.MoreSection
import com.mbta.tid.mbta_app.repositories.Settings
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
                        listOf(MoreItem.Toggle("Toggle 1", Settings.HideMaps, true))
                    )
            ) {
                toggleCallbackCalled = true
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Toggle 1").performClick()

        assertTrue { toggleCallbackCalled }
    }
}
