package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.testUtils.assertCanBeDisplayed
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test

class TrackChangePillTest {
    @get:Rule val composeTestRule = createComposeRule(effectContext = UnconfinedTestDispatcher())

    @Test
    fun testScheduledExpandedDislaysPlatformName() {
        composeTestRule.setContent {
            TrackChangePill(
                pillType = TrackChangePillType.Scheduled("2", "Track 2"),
                pillStyle = TrackChangePillStyle.Expanded,
            )
        }

        composeTestRule
            .onNodeWithTag("track_change_pill", useUnmergedTree = true)
            .assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Track 2", useUnmergedTree = true).assertCanBeDisplayed()
    }

    @Test
    fun testScheduledExpandedFallsBackToPlatformCode() {
        composeTestRule.setContent {
            TrackChangePill(
                pillType = TrackChangePillType.Scheduled("2", null),
                pillStyle = TrackChangePillStyle.Expanded,
            )
        }

        composeTestRule.onNodeWithText("2", useUnmergedTree = true).assertCanBeDisplayed()
    }
}
