package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext

class StopDetailsNoTripCardTests {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testPredictionsUnavailable() {
        composeTestRule.setContent {
            StopDetailsNoTripCard(
                status = UpcomingFormat.NoTripsFormat.PredictionsUnavailable,
                accentColor = Color.Black,
                routeType = RouteType.BUS,
            )
        }

        composeTestRule.onNodeWithTag("live_data_slash").assertIsDisplayed()
        composeTestRule.onNodeWithText("Predictions unavailable").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "Service is running, but predicted arrival times aren’t available." +
                    " Check the map to see where buses are right now."
            )
            .assertIsDisplayed()
    }

    @Test
    fun testPredictionsUnavailableHideMaps() {
        val koin = testKoinApplication {
            settings = MockSettingsRepository(mapOf(Settings.HideMaps to true))
        }

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                StopDetailsNoTripCard(
                    status = UpcomingFormat.NoTripsFormat.PredictionsUnavailable,
                    accentColor = Color.Black,
                    routeType = RouteType.BUS,
                )
            }
        }

        composeTestRule
            .onNodeWithText(
                "The map shows where buses on this route currently are.",
                substring = true,
            )
            .assertDoesNotExist()
    }

    @Test
    fun testServiceEnded() {
        composeTestRule.setContent {
            StopDetailsNoTripCard(
                status = UpcomingFormat.NoTripsFormat.ServiceEndedToday,
                accentColor = Color.Black,
                routeType = RouteType.FERRY,
            )
        }
        composeTestRule.onNodeWithTag("route_slash_icon").assertIsDisplayed()
        composeTestRule.onNodeWithText("Service ended").assertIsDisplayed()
    }

    @Test
    fun testNoSchedulesToday() {
        composeTestRule.setContent {
            StopDetailsNoTripCard(
                status = UpcomingFormat.NoTripsFormat.NoSchedulesToday,
                accentColor = Color.Black,
                routeType = RouteType.COMMUTER_RAIL,
            )
        }
        composeTestRule.onNodeWithTag("route_slash_icon").assertIsDisplayed()
        composeTestRule.onNodeWithText("No service today").assertIsDisplayed()
    }
}
