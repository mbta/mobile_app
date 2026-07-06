package com.mbta.tid.mbta_app.android.component

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import com.mbta.tid.mbta_app.android.testUtils.assertIsAlreadyOnScreen
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ScrollSeparatorColumnTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testSeparatorVisibility(): Unit = runBlocking {
        composeTestRule.setContent {
            ScrollSeparatorColumn { for (n in 1..100) Text("Content $n", Modifier.testTag("$n")) }
        }
        composeTestRule.onNodeWithTag("separator").assertIsNotDisplayed()
        composeTestRule.onNodeWithTag("99").assertIsNotDisplayed()

        composeTestRule.onNodeWithTag("99").performScrollTo()
        composeTestRule.awaitIdle()

        composeTestRule.onNodeWithTag("separator").assertIsAlreadyOnScreen()
        composeTestRule.onNodeWithTag("99").assertIsAlreadyOnScreen()
    }
}
