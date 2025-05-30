package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.R
import org.junit.Rule
import org.junit.Test

class BottomNavIconButtonTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testBottomNavIconButton() {
        val icon = R.drawable.map_pin
        val label = "Hello World"
        val active = false

        composeTestRule.setContent {
            BottomNavIconButton(
                modifier = Modifier,
                onClick = {},
                icon = icon,
                label = label,
                active = active,
            )
        }

        composeTestRule.onNodeWithText("Hello World").assertExists()
    }
}
