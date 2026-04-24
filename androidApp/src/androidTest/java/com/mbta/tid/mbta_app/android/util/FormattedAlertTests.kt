package com.mbta.tid.mbta_app.android.util

import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripShuttleAlertSummary
import com.mbta.tid.mbta_app.model.TripSpecificAlertSummary
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Month
import org.junit.Rule
import org.junit.Test

class FormattedAlertTests {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testTripSpecificShuttle() = runTest {
        val now = EasternTimeInstant(2026, Month.APRIL, 10, 15, 0, 0)
        val summary =
            TripShuttleAlertSummary(
                TripShuttleAlertSummary.SingleTrip(
                    now.plus(1.hours),
                    RouteType.COMMUTER_RAIL,
                    null,
                ),
                "Porter",
                "North Station",
            )
        val format = FormattedAlert(null, summary)
        val pattern = "Shuttle buses replace the 4:00\\sPM train from Porter to North Station"
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assert(Regex(pattern).matches(summaryString))
        }
    }

    @Test
    fun testTripSpecificDownstreamShuttle() = runTest {
        val now = EasternTimeInstant(2026, Month.APRIL, 10, 15, 0, 0)
        val summary =
            TripShuttleAlertSummary(
                TripShuttleAlertSummary.SingleTrip(
                    now.plus(1.hours),
                    RouteType.COMMUTER_RAIL,
                    "Concord",
                ),
                "Porter",
                "North Station",
            )
        val format = FormattedAlert(null, summary)
        val pattern =
            "4:00\\sPM train from Concord is replaced by shuttle buses from Porter to North Station"
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assert(Regex(pattern).matches(summaryString))
        }
    }

    @Test
    fun testThisTripSpecificShuttle() = runTest {
        val summary =
            TripShuttleAlertSummary(
                TripShuttleAlertSummary.ThisTrip(RouteType.COMMUTER_RAIL),
                "Porter",
                "North Station",
            )
        val format = FormattedAlert(null, summary)
        val expected = "Shuttle buses replace this train from Porter to North Station"
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assertEquals(expected, summaryString)
        }
    }

    @Test
    fun testTripSpecificSuspension() = runTest {
        val now = EasternTimeInstant(2026, Month.APRIL, 10, 12, 13, 0)
        val summary =
            TripSpecificAlertSummary(
                TripSpecificAlertSummary.TripFrom(now, RouteType.COMMUTER_RAIL, "Ruggles"),
                Alert.Effect.Suspension,
                null,
                true,
                Alert.Cause.Weather,
            )
        val format = FormattedAlert(null, summary)
        val pattern = "12:13\\sPM train from Ruggles is suspended today due to weather"
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assert(Regex(pattern).matches(summaryString))
        }
    }

    @Test
    fun testTripSpecificDownstreamSuspension() = runTest {
        val now = EasternTimeInstant(2026, Month.APRIL, 10, 11, 15, 0)
        val summary =
            TripSpecificAlertSummary(
                TripSpecificAlertSummary.TripFrom(now, RouteType.COMMUTER_RAIL, "Concord"),
                Alert.Effect.Suspension,
                listOf("Porter"),
                true,
                Alert.Cause.Weather,
            )
        val format = FormattedAlert(null, summary)
        val pattern = "11:15\\sAM train from Concord will terminate at Porter today due to weather"
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assert(Regex(pattern).matches(summaryString))
        }
    }

    @Test
    fun testThisTripSpecificSuspension() = runTest {
        val summary =
            TripSpecificAlertSummary(
                TripSpecificAlertSummary.ThisTrip(RouteType.COMMUTER_RAIL),
                Alert.Effect.Suspension,
                null,
                true,
                Alert.Cause.Weather,
            )
        val format = FormattedAlert(null, summary)
        val expected = "This train is suspended today due to weather"
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assertEquals(expected, summaryString)
        }
    }

    @Test
    fun testThisTripSpecificDownstreamSuspension() = runTest {
        val summary =
            TripSpecificAlertSummary(
                TripSpecificAlertSummary.ThisTrip(RouteType.COMMUTER_RAIL),
                Alert.Effect.Suspension,
                listOf("Porter"),
                true,
                Alert.Cause.Weather,
            )
        val format = FormattedAlert(null, summary)
        val expected = "This train will terminate at Porter today due to weather"
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assertEquals(expected, summaryString)
        }
    }

    @Test
    fun testTripSpecificDownstreamStopClosure() = runTest {
        val now = EasternTimeInstant(2026, Month.APRIL, 10, 12, 13, 0)
        val summary =
            TripSpecificAlertSummary(
                TripSpecificAlertSummary.TripTo(now, RouteType.COMMUTER_RAIL, "Stoughton"),
                Alert.Effect.StopClosure,
                listOf("Back Bay", "Ruggles"),
                true,
                Alert.Cause.Weather,
            )
        val format = FormattedAlert(null, summary)
        val pattern =
            "12:13\\sPM train to Stoughton will not stop at Back Bay and Ruggles today due to weather"
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assert(Regex(pattern).matches(summaryString))
        }
    }

    @Test
    fun testThisTripSpecificStopClosure() = runTest {
        val summary =
            TripSpecificAlertSummary(
                TripSpecificAlertSummary.ThisTrip(RouteType.COMMUTER_RAIL),
                Alert.Effect.StationClosure,
                listOf("Porter"),
                true,
                Alert.Cause.Weather,
            )
        val format = FormattedAlert(null, summary)
        val expected = "This train will not stop at Porter today due to weather"
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assertEquals(expected, summaryString)
        }
    }
}
