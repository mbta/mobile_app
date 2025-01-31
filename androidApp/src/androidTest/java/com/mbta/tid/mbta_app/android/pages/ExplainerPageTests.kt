package com.mbta.tid.mbta_app.android.pages

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.stopDetails.TripRouteAccents
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.stopDetailsPage.ExplainerType
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class ExplainerPageTests {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testCloses() {
        var closed = false
        composeTestRule.setContent {
            ExplainerPage(
                ExplainerType.FinishingAnotherTrip,
                TripRouteAccents.default,
                goBack = { closed = true },
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Close").performClick()
        assertTrue(closed)
    }

    @Test
    fun testFinishingAnother() {
        val objects = ObjectCollectionBuilder()
        composeTestRule.setContent {
            ExplainerPage(
                ExplainerType.FinishingAnotherTrip,
                TripRouteAccents(objects.route { type = RouteType.LIGHT_RAIL }),
                goBack = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Finishing another trip").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "The train assigned to this route is currently serving another trip. We’ll show it on the route once it starts this trip."
            )
            .assertIsDisplayed()
    }

    @Test
    fun testNoPredictions() {
        composeTestRule.setContent {
            ExplainerPage(
                ExplainerType.NoPrediction,
                TripRouteAccents.default,
                goBack = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Prediction not available yet").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "We don’t have live predictions for this trip yet, but they will appear closer to the scheduled time. If the trip is delayed or cancelled, we’ll let you know here."
            )
            .assertIsDisplayed()
    }

    @Test
    fun testNoVehicle() {
        val objects = ObjectCollectionBuilder()
        composeTestRule.setContent {
            ExplainerPage(
                ExplainerType.NoVehicle,
                TripRouteAccents(objects.route { type = RouteType.FERRY }),
                goBack = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ferry location not available yet").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "The ferry location might not be available in advance if a vehicle hasn’t been assigned yet. Once the driver starts the trip, we’ll start showing the live location."
            )
            .assertIsDisplayed()
    }
}
