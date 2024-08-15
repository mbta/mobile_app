package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.alert
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import org.junit.Rule
import org.junit.Test

class HeadsignRowViewTest {
    @get:Rule val composeTestRule = createComposeRule()

    private fun init(headsign: String, predictions: RealtimePatterns.Format) {
        composeTestRule.setContent { MyApplicationTheme { HeadsignRowView(headsign, predictions) } }
    }

    @Test
    fun showsTwoPredictions() {
        init(
            "Headsign",
            RealtimePatterns.Format.Some(
                listOf(
                    RealtimePatterns.Format.Some.FormatWithId("a", TripInstantDisplay.Minutes(2)),
                    RealtimePatterns.Format.Some.FormatWithId("b", TripInstantDisplay.Minutes(10)),
                )
            )
        )

        composeTestRule.onNodeWithText("Headsign").assertIsDisplayed()
        composeTestRule.onNodeWithText("2").assertIsDisplayed()
        composeTestRule.onNodeWithText("10").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("min").assertCountEquals(2)
    }

    @Test
    fun showsOnePrediction() {
        init(
            "A Place",
            RealtimePatterns.Format.Some(
                listOf(RealtimePatterns.Format.Some.FormatWithId("a", TripInstantDisplay.Boarding))
            )
        )

        composeTestRule.onNodeWithText("A Place").assertIsDisplayed()
        composeTestRule.onNodeWithText("BRD").assertIsDisplayed()
    }

    @Test
    fun showsNoPredictions() {
        init("Somewhere", RealtimePatterns.Format.None)

        composeTestRule.onNodeWithText("Somewhere").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Predictions").assertIsDisplayed()
    }

    @Test
    fun showsAlert() {
        init("Headsign", RealtimePatterns.Format.NoService(alert { effect = Alert.Effect.Shuttle }))

        composeTestRule.onNodeWithText("Shuttle", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun showsLoading() {
        init("Headsign", RealtimePatterns.Format.Loading)

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }
}
