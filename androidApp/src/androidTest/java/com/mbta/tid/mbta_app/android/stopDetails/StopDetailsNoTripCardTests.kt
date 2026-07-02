package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.android.testUtils.assertCanBeDisplayed
import com.mbta.tid.mbta_app.android.testUtils.hasTextMatching
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.response.NextScheduleResponse
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.plus
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
                now = EasternTimeInstant.now(),
                nextScheduleResponse = null,
            )
        }

        composeTestRule.onNodeWithTag("sunrise").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Good morning!").assertCanBeDisplayed()
        composeTestRule
            .onNode(
                hasTextMatching(
                    Regex(
                        "^The first Forest Hills train is scheduled to arrive at 9:44\\sAM. We don’t have predictions to show you yet, but they’ll appear here closer to the scheduled time.$",
                        RegexOption.IGNORE_CASE,
                    )
                )
            )
            .assertCanBeDisplayed()
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
                now = EasternTimeInstant.now(),
                nextScheduleResponse = null,
            )
        }

        composeTestRule.onNodeWithTag("live_data_slash").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Predictions unavailable").assertCanBeDisplayed()
        composeTestRule
            .onNodeWithText(
                "Service is running, but predicted arrival times aren’t available." +
                    " Check the map to see where buses are right now."
            )
            .assertCanBeDisplayed()
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
                now = EasternTimeInstant.now(),
                nextScheduleResponse = null,
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
        val objects = ObjectCollectionBuilder()
        val now = EasternTimeInstant.now()
        val schedule =
            objects.schedule {
                departureTime =
                    EasternTimeInstant(
                        now.local.date.plus(DatePeriod(days = 1)),
                        time = LocalTime(hour = 9, minute = 15),
                    )
            }
        loadKoinMocks()
        composeTestRule.setContent {
            StopDetailsNoTripCard(
                status = UpcomingFormat.NoTripsFormat.ServiceEndedToday,
                accentColor = Color.Black,
                directionLabel = "Winthrop",
                routeType = RouteType.FERRY,
                now = now,
                nextScheduleResponse = NextScheduleResponse(schedule),
            )
        }
        composeTestRule.onNodeWithTag("route_slash_icon").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Service ended").assertCanBeDisplayed()
        composeTestRule
            .onNode(
                hasTextMatching(Regex("^Next trip at 9:15\\sAM tomorrow$", RegexOption.IGNORE_CASE))
            )
            .assertCanBeDisplayed()
    }

    @Test
    fun testNoSchedulesToday() {
        val objects = ObjectCollectionBuilder()
        val now = EasternTimeInstant.now()
        val schedule =
            objects.schedule {
                departureTime =
                    EasternTimeInstant(now.local.date, time = LocalTime(hour = 9, minute = 15))
            }
        loadKoinMocks()
        composeTestRule.setContent {
            StopDetailsNoTripCard(
                status = UpcomingFormat.NoTripsFormat.NoSchedulesToday,
                accentColor = Color.Black,
                directionLabel = "Fitchburg",
                routeType = RouteType.COMMUTER_RAIL,
                now = now,
                nextScheduleResponse = NextScheduleResponse(schedule),
            )
        }
        composeTestRule.onNodeWithTag("route_slash_icon").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("No service today").assertCanBeDisplayed()
        composeTestRule
            .onNode(hasTextMatching(Regex("^Next trip at 9:15\\sAM$", RegexOption.IGNORE_CASE)))
            .assertCanBeDisplayed()
    }
}
