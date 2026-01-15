package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.trip
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.UpcomingTrip
import org.junit.Rule
import org.junit.Test

class HeadsignRowViewTest {
    @get:Rule val composeTestRule = createComposeRule()

    private fun init(headsign: String, predictions: UpcomingFormat) {
        composeTestRule.setContent { MyApplicationTheme { HeadsignRowView(headsign, predictions) } }
    }

    @Test
    fun showsTwoPredictions() {
        init(
            "Headsign",
            UpcomingFormat.Some(
                listOf(
                    UpcomingFormat.Some.FormattedTrip(
                        UpcomingTrip(trip { id = "a" }),
                        RouteType.LIGHT_RAIL,
                        TripInstantDisplay.Minutes(2, true),
                        true,
                    ),
                    UpcomingFormat.Some.FormattedTrip(
                        UpcomingTrip(trip { id = "b" }),
                        RouteType.LIGHT_RAIL,
                        TripInstantDisplay.Minutes(10, true),
                        true,
                    ),
                ),
                secondaryAlert = null,
            ),
        )

        composeTestRule.onNodeWithText("Headsign").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("10 min").assertIsDisplayed()
    }

    @Test
    fun showsOnePrediction() {
        init(
            "A Place",
            UpcomingFormat.Some(
                listOf(
                    UpcomingFormat.Some.FormattedTrip(
                        UpcomingTrip(trip { id = "a" }),
                        RouteType.LIGHT_RAIL,
                        TripInstantDisplay.Boarding(true),
                        true,
                    )
                ),
                secondaryAlert = null,
            ),
        )

        composeTestRule.onNodeWithText("A Place").assertIsDisplayed()
        composeTestRule.onNodeWithText("BRD").assertIsDisplayed()
    }

    @Test
    fun showsOnePredictionWithSecondaryAlert() {
        init(
            "A Place",
            UpcomingFormat.Some(
                listOf(
                    UpcomingFormat.Some.FormattedTrip(
                        UpcomingTrip(trip { id = "a" }),
                        RouteType.LIGHT_RAIL,
                        TripInstantDisplay.Boarding(true),
                        true,
                    )
                ),
                secondaryAlert = UpcomingFormat.SecondaryAlert("alert-large-green-issue"),
            ),
        )

        composeTestRule.onNodeWithText("A Place").assertIsDisplayed()
        composeTestRule.onNodeWithText("BRD").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Alert").assertIsDisplayed()
    }

    @Test
    fun showsNoPredictions() {
        init(
            "Somewhere",
            UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.PredictionsUnavailable),
        )

        composeTestRule.onNodeWithText("Somewhere").assertIsDisplayed()
        composeTestRule.onNodeWithText("Predictions unavailable").assertIsDisplayed()
    }

    @Test
    fun showsNoPredictionsWithSecondaryAlert() {
        init(
            "Somewhere",
            UpcomingFormat.NoTrips(
                noTripsFormat = UpcomingFormat.NoTripsFormat.PredictionsUnavailable,
                secondaryAlert = UpcomingFormat.SecondaryAlert("alert-large-bus-issue"),
            ),
        )

        composeTestRule.onNodeWithText("Somewhere").assertIsDisplayed()
        composeTestRule.onNodeWithText("Predictions unavailable").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Alert").assertIsDisplayed()
    }

    @Test
    fun showsAlert() {
        init(
            "Headsign",
            UpcomingFormat.Disruption(alert { effect = Alert.Effect.Shuttle }, mapStopRoute = null),
        )

        composeTestRule.onNodeWithText("Shuttle Bus").assertIsDisplayed()
    }

    @Test
    fun showsLoading() {
        init("Headsign", UpcomingFormat.Loading)

        composeTestRule.onNodeWithContentDescription("Loading...").assertIsDisplayed()
    }
}
