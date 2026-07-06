package com.mbta.tid.mbta_app.android.util

import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.android.assertMatches
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.AlertSummaryEntity
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
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
        val alert = ObjectCollectionBuilder.Single.alert()
        val format =
            FormattedAlert(
                alert,
                summary,
                AlertSummaryEntity(
                    null,
                    null,
                    null,
                    null,
                    "Shuttle buses replace the **4:00\u202FPM** train from **Porter** to **North Station**",
                ),
            )
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assertMatches(
                Regex(
                    "Shuttle buses replace the 4:00\\sPM train from Porter to North Station",
                    RegexOption.IGNORE_CASE,
                ),
                summaryString,
            )
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
        val alert = ObjectCollectionBuilder.Single.alert()
        val format =
            FormattedAlert(
                alert,
                summary,
                AlertSummaryEntity(
                    null,
                    null,
                    null,
                    null,
                    "**4:00\u202FPM** train from **Concord** is replaced by shuttle buses from **Porter** to **North Station**",
                ),
            )
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assertMatches(
                Regex(
                    "4:00\\sPM train from Concord is replaced by shuttle buses from Porter to North Station",
                    RegexOption.IGNORE_CASE,
                ),
                summaryString,
            )
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
        val alert = ObjectCollectionBuilder.Single.alert()
        val format =
            FormattedAlert(
                alert,
                summary,
                AlertSummaryEntity(
                    null,
                    null,
                    null,
                    null,
                    "Shuttle buses replace this train from **Porter** to **North Station**",
                ),
            )
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assertEquals(
                "Shuttle buses replace this train from Porter to North Station",
                summaryString,
            )
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
        val alert = ObjectCollectionBuilder.Single.alert { cause = Alert.Cause.Weather }
        val format =
            FormattedAlert(
                alert,
                summary,
                AlertSummaryEntity(
                    null,
                    null,
                    null,
                    null,
                    "**12:13\u202FPM** train from **Ruggles** is suspended today due to weather",
                ),
            )
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assertMatches(
                Regex(
                    "12:13\\sPM train from Ruggles is suspended today due to weather",
                    RegexOption.IGNORE_CASE,
                ),
                summaryString,
            )
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
        val alert = ObjectCollectionBuilder.Single.alert { cause = Alert.Cause.Weather }
        val format =
            FormattedAlert(
                alert,
                summary,
                AlertSummaryEntity(
                    null,
                    null,
                    null,
                    null,
                    "**11:15\u202FAM** train from **Concord** will terminate at Porter today due to weather",
                ),
            )
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assertMatches(
                Regex(
                    "11:15\\s[Aa][Mm] train from Concord will terminate at Porter today due to weather"
                ),
                summaryString,
            )
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
        val alert = ObjectCollectionBuilder.Single.alert { cause = Alert.Cause.Weather }
        val format =
            FormattedAlert(
                alert,
                summary,
                AlertSummaryEntity(
                    null,
                    null,
                    null,
                    null,
                    "This train is suspended today due to weather",
                ),
            )
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assertEquals("This train is suspended today due to weather", summaryString)
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
        val alert = ObjectCollectionBuilder.Single.alert { cause = Alert.Cause.Weather }
        val format =
            FormattedAlert(
                alert,
                summary,
                AlertSummaryEntity(
                    null,
                    null,
                    null,
                    null,
                    "This train will terminate at Porter today due to weather",
                ),
            )
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assertEquals("This train will terminate at Porter today due to weather", summaryString)
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
        val alert = ObjectCollectionBuilder.Single.alert { cause = Alert.Cause.Weather }
        val format =
            FormattedAlert(
                alert,
                summary,
                AlertSummaryEntity(
                    null,
                    null,
                    null,
                    null,
                    "**12:13\u202FPM** train to **Stoughton** will not stop at **Back Bay** and **Ruggles** today due to weather",
                ),
            )
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assertMatches(
                Regex(
                    "12:13\\sPM train to Stoughton will not stop at Back Bay and Ruggles today due to weather",
                    RegexOption.IGNORE_CASE,
                ),
                summaryString,
            )
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
        val alert = ObjectCollectionBuilder.Single.alert { cause = Alert.Cause.Weather }
        val format =
            FormattedAlert(
                alert,
                summary,
                AlertSummaryEntity(
                    null,
                    null,
                    null,
                    null,
                    "This train will not stop at **Porter** today due to weather",
                ),
            )
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assertEquals("This train will not stop at Porter today due to weather", summaryString)
        }
    }

    @Test
    fun testOneStopSkipped() = runTest {
        val summary =
            AlertSummary.Standard(
                Alert.Effect.StationClosure,
                AlertSummary.Location.AffectedStops(listOf("Back Bay")),
                AlertSummary.Timeframe.UntilFurtherNotice,
            )
        val alert = ObjectCollectionBuilder.Single.alert()
        val format =
            FormattedAlert(
                alert,
                summary,
                AlertSummaryEntity(
                    null,
                    null,
                    null,
                    null,
                    "Trains will not stop at **Back Bay** until further notice",
                ),
            )
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assertMatches(
                Regex("Trains will not stop at Back Bay until further notice"),
                summaryString,
            )
        }
    }

    @Test
    fun testTwoStopsSkipped() = runTest {
        val summary =
            AlertSummary.Standard(
                Alert.Effect.StationClosure,
                AlertSummary.Location.AffectedStops(listOf("Back Bay", "Ruggles")),
                AlertSummary.Timeframe.UntilFurtherNotice,
            )
        val alert = ObjectCollectionBuilder.Single.alert()
        val format =
            FormattedAlert(
                alert,
                summary,
                AlertSummaryEntity(
                    null,
                    null,
                    null,
                    null,
                    "Trains will not stop at **Back Bay** and **Ruggles** until further notice",
                ),
            )
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assertMatches(
                Regex("Trains will not stop at Back Bay and Ruggles until further notice"),
                summaryString,
            )
        }
    }

    @Test
    fun testThreeStopsSkipped() = runTest {
        val summary =
            AlertSummary.Standard(
                Alert.Effect.StationClosure,
                AlertSummary.Location.AffectedStops(listOf("Back Bay", "Ruggles", "Hyde Park")),
                AlertSummary.Timeframe.UntilFurtherNotice,
            )
        val alert = ObjectCollectionBuilder.Single.alert()
        val format =
            FormattedAlert(
                alert,
                summary,
                AlertSummaryEntity(
                    null,
                    null,
                    null,
                    null,
                    "Trains will not stop at **Back Bay**, **Ruggles**, and **Hyde Park** until further notice",
                ),
            )
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assertMatches(
                Regex(
                    "Trains will not stop at Back Bay, Ruggles,? and Hyde Park until further notice"
                ),
                summaryString,
            )
        }
    }

    @Test
    fun testMultipleStopsSkipped() = runTest {
        val summary =
            AlertSummary.Standard(
                Alert.Effect.StationClosure,
                AlertSummary.Location.AffectedStops(
                    listOf("Back Bay", "Ruggles", "Hyde Park", "Readville")
                ),
                AlertSummary.Timeframe.UntilFurtherNotice,
            )
        val alert = ObjectCollectionBuilder.Single.alert()
        val format =
            FormattedAlert(
                alert,
                summary,
                AlertSummaryEntity(
                    null,
                    null,
                    null,
                    null,
                    "Trains will not stop at **multiple stops** until further notice",
                ),
            )
        composeTestRule.setContent {
            val summaryString = format.alertCardMajorBody(LocalResources.current).toString()
            assertMatches(
                Regex("Trains will not stop at multiple stops until further notice"),
                summaryString,
            )
        }
    }
}
