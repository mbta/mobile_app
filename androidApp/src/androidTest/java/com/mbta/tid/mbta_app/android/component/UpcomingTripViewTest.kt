package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test

class UpcomingTripViewTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testUpcomingTripViewWithSomeOverridden() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Overridden("Test")))
        }
        composeTestRule.onNodeWithText("Test").assertIsDisplayed()
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
                UpcomingTripViewState.Some(TripInstantDisplay.Skipped(Clock.System.now()))
            )
        }
        composeTestRule.onNodeWithText("Test").assertDoesNotExist()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertDoesNotExist()
    }

    @Test
    fun testUpcomingTripViewWithCancelled() {
        val instant = Instant.fromEpochSeconds(1722535384)
        val shortTime = formatTime(instant)
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Cancelled(instant)))
        }
        composeTestRule
            .onNodeWithText("Cancelled", substring = true)
            .onParent()
            .assertExists()
            .assertContentDescriptionContains("arriving at $shortTime cancelled", substring = true)
    }

    @Test
    fun testUpcomingTripViewWithSomeNow() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Now))
        }
        composeTestRule
            .onNodeWithText("Now")
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving now", substring = true)
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeBoarding() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Boarding))
        }
        composeTestRule
            .onNodeWithText("BRD")
            .assertIsDisplayed()
            .assertContentDescriptionContains("boarding now", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeApproaching() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Approaching))
        }
        composeTestRule
            .onNodeWithText("1 min")
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving in 1 min", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeArriving() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Arriving))
        }
        composeTestRule
            .onNodeWithText("ARR")
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving now", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeAsTime() {
        val instant = Instant.fromEpochSeconds(1722535384)
        val shortTime = formatTime(instant)
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Time(instant)))
        }
        composeTestRule
            .onNodeWithText(formatTime(instant))
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving at $shortTime", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeAsTimeWithStatus() {
        val instant = Instant.fromEpochSeconds(1722535384)
        val shortTime = formatTime(instant)
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(TripInstantDisplay.TimeWithStatus(instant, "All aboard"))
            )
        }
        composeTestRule
            .onNodeWithText(formatTime(instant))
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving at $shortTime", substring = true)
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("All aboard").assertDoesNotExist()
    }

    @Test
    fun testUpcomingTripViewWithSomeScheduleTime() {
        val instant = Instant.fromEpochSeconds(1722535384)
        val shortTime = formatTime(instant)

        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.ScheduleTime(instant)))
        }
        composeTestRule
            .onNodeWithText(formatTime(instant))
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving at $shortTime scheduled", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertDoesNotExist()
    }

    @Test
    fun testUpcomingTripViewWithSomeMinutes() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Minutes(5)))
        }
        composeTestRule
            .onNodeWithText("5 min")
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving in 5 min", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeHoursAndMinutes() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Minutes(65)))
        }
        composeTestRule.onNodeWithText("1 hr 5 min").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeHours() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Minutes(60)))
        }
        composeTestRule.onNodeWithText("1 hr").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeScheduleMinutes() {
        val instant = Instant.fromEpochSeconds(1722535384)
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.ScheduleMinutes(5)))
        }
        composeTestRule
            .onNodeWithText("5 min")
            .assertIsDisplayed()
            .assertContentDescriptionContains("arriving in 5 min scheduled", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("realtimeIndicator").assertDoesNotExist()
    }

    @Test
    fun testUpcomingTripViewWithSomeMinutesOtherBus() {
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(TripInstantDisplay.Minutes(5)),
                routeType = RouteType.BUS,
                isFirst = false
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
    fun testUpcomingTripViewWithSomeMinutesFirstBus() {
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(TripInstantDisplay.Minutes(5)),
                routeType = RouteType.BUS,
                isFirst = true,
                isOnly = false
            )
        }
        composeTestRule
            .onNodeWithText("5 min")
            .assertIsDisplayed()
            .assertContentDescriptionContains("buses arriving in 5 min", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithLoading() {
        composeTestRule.setContent { UpcomingTripView(UpcomingTripViewState.Loading) }
        composeTestRule.onNodeWithContentDescription("Loading...").assertIsDisplayed()
    }
}
