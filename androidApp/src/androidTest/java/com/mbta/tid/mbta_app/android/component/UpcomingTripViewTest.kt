package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.onRoot
import com.mbta.tid.mbta_app.android.assertContentDescriptionMatches
import com.mbta.tid.mbta_app.android.assertHasColor
import com.mbta.tid.mbta_app.android.hasContentDescriptionMatching
import com.mbta.tid.mbta_app.android.hasTextMatching
import com.mbta.tid.mbta_app.android.util.FormattedAlert
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.datetime.Month
import org.junit.Rule
import org.junit.Test

class UpcomingTripViewTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testUpcomingTripViewWithSomeOverridden() {
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(TripInstantDisplay.Overridden("Test", false))
            )
        }
        composeTestRule
            .onNodeWithText("Test")
            .assertIsDisplayed()
            .assertContentDescriptionContains("Test", substring = true)
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeOverriddenLast() {
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(TripInstantDisplay.Overridden("Test", true))
            )
        }
        composeTestRule.onNodeWithText("Last").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Test")
            .assertIsDisplayed()
            .assertContentDescriptionContains("Test, Last trip", substring = true)
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeHidden() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Hidden))
        }
        composeTestRule.onNodeWithText("Test").assertDoesNotExist()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertDoesNotExist()
    }

    @Test
    fun testUpcomingTripViewWithSomeSkipped() {
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(TripInstantDisplay.Skipped(EasternTimeInstant.now()))
            )
        }
        composeTestRule.onNodeWithText("Test").assertDoesNotExist()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertDoesNotExist()
    }

    @Test
    fun testUpcomingTripViewWithCancelled() {
        val instant = EasternTimeInstant(2024, Month.AUGUST, 1, 14, 3, 4)
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Cancelled(instant)))
        }
        composeTestRule
            .onNodeWithText("Cancelled", substring = true)
            .onParent()
            .assertExists()
            .assertContentDescriptionMatches(Regex(" arriving at 2:03\\sPM cancelled"))
    }

    @Test
    fun testUpcomingTripViewWithSomeNow() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Now(false)))
        }
        composeTestRule
            .onNodeWithText("Now")
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving now", substring = true)
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeNowLast() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Now(true)))
        }
        composeTestRule.onNodeWithText("Last").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Now")
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving now, Last trip", substring = true)
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeBoarding() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Boarding(false)))
        }
        composeTestRule
            .onNodeWithText("BRD")
            .assertIsDisplayed()
            .assertContentDescriptionContains("boarding now", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeBoardingLast() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Boarding(true)))
        }
        composeTestRule.onNodeWithText("Last").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("BRD")
            .assertIsDisplayed()
            .assertContentDescriptionContains("boarding now, Last trip", substring = true)
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeApproaching() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Approaching(false)))
        }
        composeTestRule
            .onNodeWithText("1 min")
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving in 1 min", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeApproachingLast() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Approaching(true)))
        }
        composeTestRule.onNodeWithText("Last").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("1 min")
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving in 1 min, Last trip", substring = true)
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeArriving() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Arriving(false)))
        }
        composeTestRule
            .onNodeWithText("ARR")
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving now", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeArrivingLast() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Arriving(true)))
        }
        composeTestRule.onNodeWithText("Last").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("ARR")
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving now, Last trip", substring = true)
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeAsTime() {
        val instant = EasternTimeInstant(2024, Month.AUGUST, 1, 14, 3, 4)
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Time(instant, false)))
        }
        composeTestRule
            .onNode(hasTextMatching(Regex("2:03\\sPM")))
            .assertIsDisplayed()
            .assertContentDescriptionMatches(Regex(" arriving at 2:03\\sPM"))
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeAsTimeLast() {
        val instant = EasternTimeInstant(2024, Month.AUGUST, 1, 14, 3, 4)
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Time(instant, true)))
        }
        composeTestRule.onNodeWithText("Last").assertIsDisplayed()
        composeTestRule
            .onNode(hasTextMatching(Regex("2:03\\sPM")))
            .assertIsDisplayed()
            .assertContentDescriptionMatches(Regex(" arriving at 2:03\\sPM, Last trip"))
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeAsTimeWithStatus() {
        val instant = EasternTimeInstant(2024, Month.AUGUST, 1, 14, 3, 4)
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.TimeWithStatus(instant, "All aboard", false)
                ),
                routeType = RouteType.COMMUTER_RAIL,
            )
        }

        composeTestRule
            .onNode(hasTextMatching(Regex("2:03\\sPM")), useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNode(hasContentDescriptionMatching(Regex("train arriving at 2:03\\sPM, All aboard")))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("realtimeIndicator", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("All aboard", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeAsTimeWithStatusLast() {
        val instant = EasternTimeInstant(2024, Month.AUGUST, 1, 14, 3, 4)
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.TimeWithStatus(instant, "All aboard", true)
                ),
                routeType = RouteType.COMMUTER_RAIL,
            )
        }
        composeTestRule.onNodeWithText("Last", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule
            .onNode(hasTextMatching(Regex("2:03\\sPM")), useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNode(
                hasContentDescriptionMatching(
                    Regex("train arriving at 2:03\\sPM, All aboard, Last trip")
                )
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("realtimeIndicator", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeAsTimeWithStatusDelay() {
        val instant = EasternTimeInstant(2024, Month.AUGUST, 1, 14, 3, 4)
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.TimeWithStatus(instant, "Delay", false)
                ),
                routeType = RouteType.COMMUTER_RAIL,
            )
        }
        composeTestRule
            .onNode(hasTextMatching(Regex("2:03\\sPM")), useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNode(hasContentDescriptionMatching(Regex("2:03\\sPM train delayed")))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("realtimeIndicator", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeAsTimeWithStatusLate() {
        val instant = EasternTimeInstant(2024, Month.AUGUST, 1, 14, 3, 4)
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.TimeWithStatus(instant, "Late", false)
                ),
                routeType = RouteType.COMMUTER_RAIL,
            )
        }
        composeTestRule
            .onNode(hasTextMatching(Regex("2:03\\sPM")), useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNode(hasContentDescriptionMatching(Regex("2:03\\sPM train delayed")))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("realtimeIndicator", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeAsTimeWithScheduleDelay() {
        val predictionInstant = EasternTimeInstant(2024, Month.AUGUST, 1, 14, 9, 44)
        val scheduleInstant = EasternTimeInstant(2024, Month.AUGUST, 1, 14, 3, 4)
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.TimeWithSchedule(predictionInstant, scheduleInstant, false)
                ),
                routeType = RouteType.COMMUTER_RAIL,
            )
        }
        composeTestRule
            .onNode(hasTextMatching(Regex("2:09\\sPM")), useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNode(
                hasContentDescriptionMatching(
                    Regex("2:03\\sPM train delayed, arriving at 2:09\\sPM")
                )
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("realtimeIndicator", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNode(hasTextMatching(Regex("2:03\\sPM")), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeAsTimeWithScheduleEarly() {
        val predictionInstant = EasternTimeInstant(2024, Month.AUGUST, 1, 14, 3, 4)
        val scheduleInstant = EasternTimeInstant(2024, Month.AUGUST, 1, 14, 9, 44)
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.TimeWithSchedule(predictionInstant, scheduleInstant, false)
                ),
                routeType = RouteType.COMMUTER_RAIL,
            )
        }
        composeTestRule
            .onNode(hasTextMatching(Regex("2:03\\sPM")), useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNode(
                hasContentDescriptionMatching(Regex("2:09\\sPM train early, arriving at 2:03\\sPM"))
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("realtimeIndicator", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNode(hasTextMatching(Regex("2:09\\sPM")), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeAsTimeWithScheduleDelayOther() {
        val predictionInstant = EasternTimeInstant(2024, Month.AUGUST, 1, 14, 9, 44)
        val scheduleInstant = EasternTimeInstant(2024, Month.AUGUST, 1, 14, 3, 4)
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.TimeWithSchedule(predictionInstant, scheduleInstant, false)
                ),
                routeType = RouteType.COMMUTER_RAIL,
                isFirst = false,
            )
        }

        composeTestRule
            .onNode(
                hasContentDescriptionMatching(
                    Regex("and 2:03\\sPM train delayed, arriving at 2:09\\sPM")
                )
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("realtimeIndicator", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNode(hasTextMatching(Regex("2:09\\sPM")), useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNode(hasTextMatching(Regex("2:03\\sPM")), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeAsTimeWithScheduleEarlyOther() {
        val predictionInstant = EasternTimeInstant(2024, Month.AUGUST, 1, 14, 3, 4)
        val scheduleInstant = EasternTimeInstant(2024, Month.AUGUST, 1, 14, 9, 44)
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.TimeWithSchedule(predictionInstant, scheduleInstant, false)
                ),
                routeType = RouteType.COMMUTER_RAIL,
                isFirst = false,
            )
        }
        composeTestRule
            .onNode(hasTextMatching(Regex("2:03\\sPM")), useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNode(
                hasContentDescriptionMatching(
                    Regex("and 2:09\\sPM train early, arriving at 2:03\\sPM")
                )
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("realtimeIndicator", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNode(hasTextMatching(Regex("2:09\\sPM")), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeScheduleTime() {
        val instant = EasternTimeInstant(2024, Month.AUGUST, 1, 14, 3, 4)

        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(TripInstantDisplay.ScheduleTime(instant, false))
            )
        }
        composeTestRule
            .onNode(hasTextMatching(Regex("2:03\\sPM")))
            .assertIsDisplayed()
            .assertContentDescriptionMatches(Regex(" arriving at 2:03\\sPM scheduled"))
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertDoesNotExist()
    }

    @Test
    fun testUpcomingTripViewWithSomeScheduleTimeLast() {
        val instant = EasternTimeInstant(2024, Month.AUGUST, 1, 14, 3, 4)
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(TripInstantDisplay.ScheduleTime(instant, true))
            )
        }
        composeTestRule.onNodeWithText("Last").assertIsDisplayed()
        composeTestRule
            .onNode(hasTextMatching(Regex("2:03\\sPM")))
            .assertIsDisplayed()
            .assertContentDescriptionMatches(Regex(" arriving at 2:03\\sPM scheduled, Last trip"))
        composeTestRule.onNodeWithTag("lastScheduleIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeScheduleTimeWithStatusColumn() {
        val instant = EasternTimeInstant(2025, Month.AUGUST, 5, 14, 14)
        val status = "Delayed"

        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.ScheduleTimeWithStatusColumn(instant, status, false)
                ),
                routeType = RouteType.COMMUTER_RAIL,
            )
        }
        composeTestRule
            .onNode(hasTextMatching(Regex("2:14\\sPM")), useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Delayed", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule
            .onNode(hasContentDescriptionMatching(Regex("2:14\\sPM train delayed")))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("realtimeIndicator", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun testUpcomingTripViewWithSomeScheduleTimeWithStatusColumnLast() {
        val instant = EasternTimeInstant(2025, Month.AUGUST, 5, 14, 14)
        val status = "Delayed"
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.ScheduleTimeWithStatusColumn(instant, status, true)
                ),
                routeType = RouteType.COMMUTER_RAIL,
            )
        }
        composeTestRule.onNodeWithText("Last", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule
            .onNode(hasTextMatching(Regex("2:14\\sPM")), useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNode(hasContentDescriptionMatching(Regex("2:14\\sPM train delayed, Last trip")))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("lastScheduleIndicator", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeScheduleTimeWithStatusRow() {
        val instant = EasternTimeInstant(2025, Month.AUGUST, 5, 14, 14)
        val status = "Anomalous"

        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.ScheduleTimeWithStatusRow(instant, status)
                ),
                routeType = RouteType.COMMUTER_RAIL,
            )
        }
        composeTestRule
            .onNode(hasTextMatching(Regex("2:14\\sPM")), useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Anomalous", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule
            .onNode(
                hasContentDescriptionMatching(
                    Regex("train arriving at 2:14\\sPM scheduled, Anomalous")
                )
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("realtimeIndicator", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun testUpcomingTripViewWithSomeMinutes() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Minutes(5, false)))
        }
        composeTestRule
            .onNodeWithText("5 min")
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving in 5 min", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeMinutesLast() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Minutes(5, true)))
        }
        composeTestRule.onNodeWithText("Last").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("5 min")
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving in 5 min, Last trip", substring = true)
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeHoursAndMinutes() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Minutes(65, false)))
        }
        composeTestRule
            .onNodeWithText("1 hr 5 min")
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving in 1 hr 5 min", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeHours() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Minutes(60, false)))
        }
        composeTestRule
            .onNodeWithText("1 hr")
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving in 1 hr", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeScheduleMinutes() {
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(TripInstantDisplay.ScheduleMinutes(5, false))
            )
        }
        composeTestRule
            .onNodeWithText("5 min")
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving in 5 min scheduled", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertDoesNotExist()
    }

    @Test
    fun testUpcomingTripViewWithSomeScheduleMinutesLast() {
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(TripInstantDisplay.ScheduleMinutes(5, true))
            )
        }
        composeTestRule.onNodeWithText("Last").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("5 min")
            .assertIsDisplayed()
            .assertContentDescriptionContains(
                "arriving in 5 min scheduled, Last trip",
                substring = true,
            )
        composeTestRule.onNodeWithTag("lastScheduleIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeScheduleMinutesOver60() {
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(TripInstantDisplay.ScheduleMinutes(75, false))
            )
        }
        composeTestRule
            .onNodeWithText("1 hr 15 min")
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving in 1 hr 15 min scheduled", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertDoesNotExist()
    }

    @Test
    fun testUpcomingTripViewWithSomeMinutesOtherBus() {
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(TripInstantDisplay.Minutes(5, false)),
                routeType = RouteType.BUS,
                isFirst = false,
            )
        }
        composeTestRule
            .onNodeWithText("5 min")
            .assertIsDisplayed()
            .assertContentDescriptionContains("and in 5 min", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeHoursAndMinutesOtherBus() {
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(TripInstantDisplay.Minutes(65, false)),
                routeType = RouteType.BUS,
                isFirst = false,
            )
        }
        composeTestRule
            .onNodeWithText("1 hr 5 min")
            .assertIsDisplayed()
            .assertContentDescriptionContains("and in 1 hr 5 min", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeHoursOtherBus() {
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(TripInstantDisplay.Minutes(60, false)),
                routeType = RouteType.BUS,
                isFirst = false,
            )
        }
        composeTestRule
            .onNodeWithText("1 hr")
            .assertIsDisplayed()
            .assertContentDescriptionContains("and in 1 hr", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeMinutesFirstBus() {
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(TripInstantDisplay.Minutes(5, false)),
                routeType = RouteType.BUS,
                isFirst = true,
                isOnly = false,
            )
        }
        composeTestRule
            .onNodeWithText("5 min")
            .assertIsDisplayed()
            .assertContentDescriptionContains("buses arriving in 5 min", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithNoTripsSubwayEarlyMorning() {
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.NoTrips(
                    UpcomingFormat.NoTripsFormat.SubwayEarlyMorning(
                        EasternTimeInstant(2025, Month.NOVEMBER, 17, 9, 41)
                    )
                )
            )
        }
        composeTestRule.onNode(hasTextMatching(Regex("^First 9:41\\sAM"))).assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithLoading() {
        composeTestRule.setContent { UpcomingTripView(UpcomingTripViewState.Loading) }
        composeTestRule.onNodeWithContentDescription("Loading...").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithDisruption() {
        val alert = ObjectCollectionBuilder.Single.alert { effect = Alert.Effect.Suspension }
        val disruption = UpcomingFormat.Disruption(alert, mapStopRoute = MapStopRoute.FERRY)
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Disruption(FormattedAlert(alert), disruption.iconName)
            )
        }
        composeTestRule
            .onNodeWithText("Suspension")
            .assert(hasContentDescription("Service suspended"))
        composeTestRule.onRoot().assertHasColor(Color.fromHex("008eaa"))
    }
}
