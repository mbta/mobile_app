package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ScrollSeparatorLazyColumnTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testSeparatorVisibility(): Unit = runBlocking {
        composeTestRule.setContent {
            ScrollSeparatorLazyColumn(Modifier.testTag("column")) {
                items((1..100).toList()) { n -> Text("Content $n", Modifier.testTag("$n")) }
            }
        }
        composeTestRule.onNodeWithTag("separator").assertIsNotDisplayed()
        composeTestRule.onNodeWithTag("99").assertIsNotDisplayed()

        composeTestRule.onNodeWithTag("column").performScrollToNode(hasTestTag("99"))
        composeTestRule.awaitIdle()

        composeTestRule.onNodeWithTag("separator").assertIsDisplayed()
        composeTestRule.onNodeWithTag("99").assertIsDisplayed()
    }
}
