package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.testUtils.assertCanBeDisplayed
import com.mbta.tid.mbta_app.model.TrackChange
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test

class TrackNumberTest {
    @get:Rule val composeTestRule = createComposeRule(effectContext = UnconfinedTestDispatcher())

    @Test
    fun testScheduledTrackChangeShowsPill() {
        composeTestRule.setContent {
            TrackNumber(
                TripDetailsStopList.Track.TrackChange(TrackChange.Scheduled("2", "Track 2")),
            )
        }
        composeTestRule.onNodeWithText("Track 2", useUnmergedTree = true).assertCanBeDisplayed()
    }
}
