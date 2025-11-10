package com.mbta.tid.mbta_app.android.pages

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.TripResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import com.mbta.tid.mbta_app.model.response.VehicleStreamDataResponse
import com.mbta.tid.mbta_app.repositories.MockTripRepository
import com.mbta.tid.mbta_app.repositories.MockVehicleRepository
import com.mbta.tid.mbta_app.utils.NavigationCallbacks
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class TripDetailsPageTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testHasCloseButton() {
        var onCloseCalled = false
        composeTestRule.setContent {
            TripDetailsPage(
                filter =
                    TripDetailsPageFilter(
                        "tripId",
                        "vehicleId",
                        Route.Id("routeId"),
                        0,
                        "stopId",
                        null,
                    ),
                allAlerts = AlertsStreamDataResponse(emptyMap()),
                openModal = {},
                openSheetRoute = {},
                navCallbacks =
                    NavigationCallbacks(
                        onBack = null,
                        onClose = { onCloseCalled = true },
                        backButtonPresentation = NavigationCallbacks.BackButtonPresentation.Floating,
                    ),
            )
        }

        composeTestRule.onNodeWithContentDescription("Close").performClick()
        assertTrue(onCloseCalled)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testHasContent() {
        val objects = TestData.clone()
        val routePattern = objects.getRoutePattern("Orange-3-0")
        val trip =
            objects.trip(routePattern) {
                stopIds = objects.getTrip(routePattern.representativeTripId).stopIds
            }
        val targetStop = objects.getStop("place-dwnxg")
        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.IncomingAt
                stopId = targetStop.id
                tripId = trip.id
            }

        loadKoinMocks(objects) {
            this.trip =
                MockTripRepository(
                    tripSchedulesResponse =
                        TripSchedulesResponse.StopIds(checkNotNull(trip.stopIds)),
                    tripResponse = TripResponse(trip),
                )
            this.vehicle =
                MockVehicleRepository(outcome = ApiResult.Ok(VehicleStreamDataResponse(vehicle)))
        }
        composeTestRule.setContent {
            TripDetailsPage(
                filter =
                    TripDetailsPageFilter(
                        tripId = trip.id,
                        vehicleId = vehicle.id,
                        routeId = trip.routeId,
                        directionId = trip.directionId,
                        stopId = targetStop.id,
                        stopSequence = null,
                    ),
                allAlerts = AlertsStreamDataResponse(objects),
                openModal = {},
                openSheetRoute = {},
                navCallbacks = NavigationCallbacks.empty,
            )
        }

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasText("Southbound to") and hasText("Forest Hills")
        )
        composeTestRule.onNodeWithText("OL").assertIsDisplayed()
        composeTestRule.onNodeWithText("Southbound to").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Forest Hills").assertCountEquals(2)
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText(targetStop.name))
    }
}
