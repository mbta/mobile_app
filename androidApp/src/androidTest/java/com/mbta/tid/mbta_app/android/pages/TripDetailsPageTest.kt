package com.mbta.tid.mbta_app.android.pages

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.TripResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import com.mbta.tid.mbta_app.model.response.VehicleStreamDataResponse
import com.mbta.tid.mbta_app.repositories.MockTripRepository
import com.mbta.tid.mbta_app.repositories.MockVehicleRepository
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
                filter = TripDetailsPageFilter("tripId", "vehicleId", "routeId", 0, "stopId", null),
                allAlerts = AlertsStreamDataResponse(emptyMap()),
                openModal = {},
                openSheetRoute = {},
                onClose = { onCloseCalled = true },
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
                onClose = {},
            )
        }

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Forest Hills"))
        composeTestRule.onNodeWithText("OL").assertIsDisplayed()
        composeTestRule.onNodeWithText("Southbound to").assertIsDisplayed()
        composeTestRule.onNodeWithText("Forest Hills").assertIsDisplayed()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText(targetStop.name))
    }
}
