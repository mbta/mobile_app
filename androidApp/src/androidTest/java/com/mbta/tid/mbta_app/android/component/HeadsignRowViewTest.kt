package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.alert
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.RouteType
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
                    RealtimePatterns.Format.Some.FormatWithId(
                        "a",
                        RouteType.LIGHT_RAIL,
                        TripInstantDisplay.Minutes(2)
                    ),
                    RealtimePatterns.Format.Some.FormatWithId(
                        "b",
                        RouteType.LIGHT_RAIL,
                        TripInstantDisplay.Minutes(10)
                    ),
                ),
                secondaryAlert = null
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
            RealtimePatterns.Format.Some(
                listOf(
                    RealtimePatterns.Format.Some.FormatWithId(
                        "a",
                        RouteType.LIGHT_RAIL,
                        TripInstantDisplay.Boarding
                    )
                ),
                secondaryAlert = null
            )
        )

        composeTestRule.onNodeWithText("A Place").assertIsDisplayed()
        composeTestRule.onNodeWithText("BRD").assertIsDisplayed()
    }

    @Test
    fun showsOnePredictionWithSecondaryAlert() {
        init(
            "A Place",
            RealtimePatterns.Format.Some(
                listOf(
                    RealtimePatterns.Format.Some.FormatWithId(
                        "a",
                        RouteType.LIGHT_RAIL,
                        TripInstantDisplay.Boarding
                    )
                ),
                secondaryAlert = RealtimePatterns.Format.SecondaryAlert("alert-large-green-issue")
            )
        )

        composeTestRule.onNodeWithText("A Place").assertIsDisplayed()
        composeTestRule.onNodeWithText("BRD").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Alert").assertIsDisplayed()
    }

    @Test
    fun showsNoPredictions() {
        init(
            "Somewhere",
            RealtimePatterns.Format.NoTrips(RealtimePatterns.NoTripsFormat.PredictionsUnavailable)
        )

        composeTestRule.onNodeWithText("Somewhere").assertIsDisplayed()
        composeTestRule.onNodeWithText("Predictions unavailable").assertIsDisplayed()
    }

    @Test
    fun showsNoPredictionsWithSecondaryAlert() {
        init(
            "Somewhere",
            RealtimePatterns.Format.NoTrips(
                noTripsFormat = RealtimePatterns.NoTripsFormat.PredictionsUnavailable,
                secondaryAlert = RealtimePatterns.Format.SecondaryAlert("alert-large-bus-issue")
            )
        )

        composeTestRule.onNodeWithText("Somewhere").assertIsDisplayed()
        composeTestRule.onNodeWithText("Predictions unavailable").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Alert").assertIsDisplayed()
    }

    @Test
    fun showsAlert() {
        init(
            "Headsign",
            RealtimePatterns.Format.Disruption(
                alert { effect = Alert.Effect.Shuttle },
                mapStopRoute = null
            )
        )

        composeTestRule.onNodeWithText("Shuttle Bus").assertIsDisplayed()
    }

    @Test
    fun showsLoading() {
        init("Headsign", RealtimePatterns.Format.Loading)

        composeTestRule.onNodeWithContentDescription("Loading...").assertIsDisplayed()
    }
}
