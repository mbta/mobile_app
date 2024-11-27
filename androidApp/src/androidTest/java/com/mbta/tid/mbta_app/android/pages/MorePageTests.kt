package com.mbta.tid.mbta_app.android.pages

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class MorePageTests {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testNearbyTransitPageDisplaysCorrectly() {
        composeTestRule.setContent { MorePage(bottomBar = {}) }

        composeTestRule.onNodeWithText("MBTA Go").assertIsDisplayed()
    }
}
