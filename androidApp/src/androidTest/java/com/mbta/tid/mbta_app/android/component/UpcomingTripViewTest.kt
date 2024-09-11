package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
    }

    @Test
    fun testUpcomingTripViewWithSomeHidden() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Hidden))
        }
        composeTestRule.onNodeWithText("Test").assertDoesNotExist()
    }

    @Test
    fun testUpcomingTripViewWithSomeSkipped() {
        composeTestRule.setContent {
            UpcomingTripView(
                UpcomingTripViewState.Some(TripInstantDisplay.Skipped(Clock.System.now()))
            )
        }
        composeTestRule.onNodeWithText("Test").assertDoesNotExist()
    }

    @Test
    fun testUpcomingTripViewWithSomeNow() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Now))
        }
        composeTestRule.onNodeWithText("Now").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeBoarding() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Boarding))
        }
        composeTestRule.onNodeWithText("BRD").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeApproaching() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Approaching))
        }
        composeTestRule.onNodeWithText("1 min").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeArriving() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Arriving))
        }
        composeTestRule.onNodeWithText("ARR").assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeAsTime() {
        val instant = Instant.fromEpochSeconds(1722535384)
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.AsTime(instant)))
        }
        composeTestRule.onNodeWithText(formatTime(instant)).assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeSchedule() {
        val instant = Instant.fromEpochSeconds(1722535384)
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Schedule(instant)))
        }
        composeTestRule.onNodeWithText(formatTime(instant)).assertIsDisplayed()
    }

    @Test
    fun testUpcomingTripViewWithSomeMinutes() {
        composeTestRule.setContent {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Minutes(5)))
        }
        composeTestRule.onNodeWithText("5 min").assertIsDisplayed()
    }
}
