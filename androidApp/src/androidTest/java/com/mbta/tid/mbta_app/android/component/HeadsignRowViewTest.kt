package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.alert
import com.mbta.tid.mbta_app.model.PatternsByHeadsign
import com.mbta.tid.mbta_app.model.UpcomingTrip
import org.junit.Rule
import org.junit.Test

class HeadsignRowViewTest {
    @get:Rule val composeTestRule = createComposeRule()

    private fun init(headsign: String, predictions: PatternsByHeadsign.Format) {
        composeTestRule.setContent { MyApplicationTheme { HeadsignRowView(headsign, predictions) } }
    }

    @Test
    fun showsTwoPredictions() {
        init(
            "Headsign",
            PatternsByHeadsign.Format.Some(
                listOf(
                    PatternsByHeadsign.Format.Some.FormatWithId(
                        "a",
                        UpcomingTrip.Format.Minutes(2)
                    ),
                    PatternsByHeadsign.Format.Some.FormatWithId(
                        "b",
                        UpcomingTrip.Format.Minutes(10)
                    ),
                )
            )
        )

        composeTestRule.onNodeWithText("Headsign").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("10 min").assertIsDisplayed()
    }

    @Test
    fun showsOnePrediction() {
        init(
            "A Place",
            PatternsByHeadsign.Format.Some(
                listOf(
                    PatternsByHeadsign.Format.Some.FormatWithId("a", UpcomingTrip.Format.Boarding)
                )
            )
        )

        composeTestRule.onNodeWithText("A Place").assertIsDisplayed()
        composeTestRule.onNodeWithText("BRD").assertIsDisplayed()
    }

    @Test
    fun showsNoPredictions() {
        init("Somewhere", PatternsByHeadsign.Format.None)

        composeTestRule.onNodeWithText("Somewhere").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Predictions").assertIsDisplayed()
    }

    @Test
    fun showsAlert() {
        init(
            "Headsign",
            PatternsByHeadsign.Format.NoService(alert { effect = Alert.Effect.Shuttle })
        )

        composeTestRule.onNodeWithText("Shuttle", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun showsLoading() {
        init("Headsign", PatternsByHeadsign.Format.Loading)

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }
}
