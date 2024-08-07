package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class BottomNavIconButtonTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testBottomNavIconButton() {
        val icon = 0
        val label = "Hello World"
        val active = false

        composeTestRule.setContent {
            BottomNavIconButton(onClick = {}, icon = icon, label = label, active = active)
        }

        composeTestRule.onNodeWithText("Hello World").assertExists()
    }
}
