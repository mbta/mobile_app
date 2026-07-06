package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.testUtils.assertCanBeDisplayed
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
                warningAlert = null,
            ),
        )

        composeTestRule.onNodeWithText("Headsign").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("2 min").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("10 min").assertCanBeDisplayed()
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
                warningAlert = null,
            ),
        )

        composeTestRule.onNodeWithText("A Place").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("BRD").assertCanBeDisplayed()
    }

    @Test
    fun showsOnePredictionWithWarningAlert() {
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
                warningAlert = UpcomingFormat.WarningAlert("alert-large-green-issue"),
            ),
        )

        composeTestRule.onNodeWithText("A Place").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("BRD").assertCanBeDisplayed()
        composeTestRule.onNodeWithContentDescription("Alert").assertCanBeDisplayed()
    }

    @Test
    fun showsNoPredictions() {
        init(
            "Somewhere",
            UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.PredictionsUnavailable),
        )

        composeTestRule.onNodeWithText("Somewhere").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Predictions unavailable").assertCanBeDisplayed()
    }

    @Test
    fun showsNoPredictionsWithWarningAlert() {
        init(
            "Somewhere",
            UpcomingFormat.NoTrips(
                noTripsFormat = UpcomingFormat.NoTripsFormat.PredictionsUnavailable,
                warningAlert = UpcomingFormat.WarningAlert("alert-large-bus-issue"),
            ),
        )

        composeTestRule.onNodeWithText("Somewhere").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Predictions unavailable").assertCanBeDisplayed()
        composeTestRule.onNodeWithContentDescription("Alert").assertCanBeDisplayed()
    }

    @Test
    fun showsAlert() {
        init(
            "Headsign",
            UpcomingFormat.Disruption(alert { effect = Alert.Effect.Shuttle }, mapStopRoute = null),
        )

        composeTestRule.onNodeWithText("Shuttle Bus").assertCanBeDisplayed()
    }

    @Test
    fun showsLoading() {
        init("Headsign", UpcomingFormat.Loading)

        composeTestRule.onNodeWithContentDescription("Loading...").assertCanBeDisplayed()
    }
}
