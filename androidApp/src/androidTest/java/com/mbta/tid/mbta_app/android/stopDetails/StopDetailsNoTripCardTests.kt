package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.RouteType
import org.junit.Rule
import org.junit.Test

class StopDetailsNoTripCardTests {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testPredictionsUnavailable() {
        composeTestRule.setContent {
            StopDetailsNoTripCard(
                status = RealtimePatterns.NoTripsFormat.PredictionsUnavailable,
                accentColor = Color.Black,
                routeType = RouteType.BUS,
                hideMaps = false
            )
        }

        composeTestRule.onNodeWithTag("live_data_slash").assertIsDisplayed()
        composeTestRule.onNodeWithText("Predictions unavailable").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "Service is running, but predicted arrival times aren’t available." +
                    " The map shows where buses on this route currently are."
            )
            .assertIsDisplayed()
    }

    @Test
    fun testPredictionsUnavailableHideMaps() {
        composeTestRule.setContent {
            StopDetailsNoTripCard(
                status = RealtimePatterns.NoTripsFormat.PredictionsUnavailable,
                accentColor = Color.Black,
                routeType = RouteType.BUS,
                hideMaps = true
            )
        }

        composeTestRule
            .onNodeWithText(
                "The map shows where buses on this route currently are.",
                substring = true
            )
            .assertDoesNotExist()
    }

    @Test
    fun testServiceEnded() {
        composeTestRule.setContent {
            StopDetailsNoTripCard(
                status = RealtimePatterns.NoTripsFormat.ServiceEndedToday,
                accentColor = Color.Black,
                routeType = RouteType.FERRY,
                hideMaps = false
            )
        }
        composeTestRule.onNodeWithTag("route_slash_icon").assertIsDisplayed()
        composeTestRule.onNodeWithText("Service ended").assertIsDisplayed()
    }

    @Test
    fun testNoSchedulesToday() {
        composeTestRule.setContent {
            StopDetailsNoTripCard(
                status = RealtimePatterns.NoTripsFormat.NoSchedulesToday,
                accentColor = Color.Black,
                routeType = RouteType.COMMUTER_RAIL,
                hideMaps = false
            )
        }
        composeTestRule.onNodeWithTag("route_slash_icon").assertIsDisplayed()
        composeTestRule.onNodeWithText("No service today").assertIsDisplayed()
    }
}
