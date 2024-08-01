package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class BoldedTripStatusTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testBoldedTripStatus() {
        val text = "Hello World"

        composeTestRule.setContent { BoldedTripStatus(text = text) }

        composeTestRule.onNodeWithText("Hello").assertIsDisplayed()
        composeTestRule.onNodeWithText("World").assertIsDisplayed()
    }
}
