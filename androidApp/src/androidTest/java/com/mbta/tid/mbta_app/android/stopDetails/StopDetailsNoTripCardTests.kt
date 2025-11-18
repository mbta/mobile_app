package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.hasTextMatching
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.datetime.Month
import org.junit.Rule
import org.junit.Test

class StopDetailsNoTripCardTests {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testSubwayEarlyMorning() {
        loadKoinMocks()
        composeTestRule.setContent {
            StopDetailsNoTripCard(
                status =
                    UpcomingFormat.NoTripsFormat.SubwayEarlyMorning(
                        EasternTimeInstant(2025, Month.NOVEMBER, 17, 9, 44)
                    ),
                accentColor = Color.Black,
                directionLabel = "Forest Hills",
                routeType = RouteType.HEAVY_RAIL,
            )
        }

        composeTestRule.onNodeWithTag("sunrise").assertIsDisplayed()
        composeTestRule.onNodeWithText("Good morning!").assertIsDisplayed()
        composeTestRule
            .onNode(
                hasTextMatching(
                    Regex(
                        "^The first Forest Hills train is scheduled to arrive at 9:44\\sAM. We don’t have predictions to show you yet, but they’ll appear here closer to the scheduled time.$"
                    )
                )
            )
            .assertIsDisplayed()
    }

    @Test
    fun testPredictionsUnavailable() {
        loadKoinMocks()
        composeTestRule.setContent {
            StopDetailsNoTripCard(
                status = UpcomingFormat.NoTripsFormat.PredictionsUnavailable,
                accentColor = Color.Black,
                directionLabel = "Forest Hills",
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
        loadKoinMocks { settings = MockSettingsRepository(mapOf(Settings.HideMaps to true)) }

        composeTestRule.setContent {
            StopDetailsNoTripCard(
                status = UpcomingFormat.NoTripsFormat.PredictionsUnavailable,
                accentColor = Color.Black,
                directionLabel = "Forest Hills",
                routeType = RouteType.BUS,
            )
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
        loadKoinMocks()
        composeTestRule.setContent {
            StopDetailsNoTripCard(
                status = UpcomingFormat.NoTripsFormat.ServiceEndedToday,
                accentColor = Color.Black,
                directionLabel = "Winthrop",
                routeType = RouteType.FERRY,
            )
        }
        composeTestRule.onNodeWithTag("route_slash_icon").assertIsDisplayed()
        composeTestRule.onNodeWithText("Service ended").assertIsDisplayed()
    }

    @Test
    fun testNoSchedulesToday() {
        loadKoinMocks()
        composeTestRule.setContent {
            StopDetailsNoTripCard(
                status = UpcomingFormat.NoTripsFormat.NoSchedulesToday,
                accentColor = Color.Black,
                directionLabel = "Fitchburg",
                routeType = RouteType.COMMUTER_RAIL,
            )
        }
        composeTestRule.onNodeWithTag("route_slash_icon").assertIsDisplayed()
        composeTestRule.onNodeWithText("No service today").assertIsDisplayed()
    }
}
