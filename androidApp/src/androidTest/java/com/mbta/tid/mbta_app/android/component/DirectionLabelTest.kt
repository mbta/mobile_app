package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.testUtils.assertCanBeDisplayed
import com.mbta.tid.mbta_app.model.Direction
import org.junit.Rule
import org.junit.Test

class DirectionLabelTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testBasicDirection() {
        composeTestRule.setContent {
            DirectionLabel(Direction(name = "Inbound", destination = "South Station", id = 1))
        }
        composeTestRule.onNodeWithText("Inbound to").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("South Station").assertCanBeDisplayed()
    }

    @Test
    fun testDirectionReformatting() {
        composeTestRule.setContent {
            DirectionLabel(Direction(name = "East", destination = "Park St & North", id = 1))
        }
        composeTestRule.onNodeWithText("Eastbound to").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Park St & North").assertCanBeDisplayed()
    }
}
