package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.component.formatTime
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripHeaderSpec
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test

class TripHeaderCardTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testDisplaysStopName() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "Stop Name" }
        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.InTransitTo
                tripId = ""
            }
        val route = objects.route {}

        composeTestRule.setContent {
            TripHeaderCard(
                "",
                TripHeaderSpec.VehicleOnTrip(vehicle, stop, null, false),
                "",
                TripRouteAccents(route),
                now
            )
        }

        composeTestRule.onNodeWithText(stop.name, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun testDisplaysStatusDescription() = runTest {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}

        val vehicleState =
            mutableStateOf<Vehicle>(
                objects.vehicle {
                    currentStatus = Vehicle.CurrentStatus.InTransitTo
                    tripId = ""
                }
            )
        val route = objects.route {}

        composeTestRule.setContent {
            val vehicle: Vehicle by vehicleState
            TripHeaderCard(
                "",
                TripHeaderSpec.VehicleOnTrip(vehicle, stop, null, false),
                "",
                TripRouteAccents(route),
                now
            )
        }

        composeTestRule.onNodeWithText("Next stop", useUnmergedTree = true).assertIsDisplayed()

        val incomingVehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.IncomingAt
                tripId = ""
            }

        vehicleState.value = incomingVehicle
        composeTestRule.awaitIdle()
        composeTestRule.onNodeWithText("Approaching", useUnmergedTree = true).assertIsDisplayed()

        val stoppedVehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                tripId = ""
            }

        vehicleState.value = stoppedVehicle

        composeTestRule.awaitIdle()
        composeTestRule.onNodeWithText("Now at", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun testDifferentTrip() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "Stop Name" }
        val route = objects.route {}

        composeTestRule.setContent {
            TripHeaderCard(
                "selected",
                TripHeaderSpec.FinishingAnotherTrip,
                "",
                TripRouteAccents(route),
                now
            )
        }

        composeTestRule.onNodeWithText("Finishing another trip").assertIsDisplayed()
        composeTestRule.onNodeWithText(stop.name, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun testNoVehicle() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "Stop Name" }
        val route = objects.route {}

        composeTestRule.setContent {
            TripHeaderCard("selected", TripHeaderSpec.NoVehicle, "", TripRouteAccents(route), now)
        }
        composeTestRule
            .onNodeWithText("Location not available yet", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stop.name, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun testAtTarget() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}

        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                tripId = ""
                stopId = stop.id
            }

        val route = objects.route {}

        composeTestRule.setContent {
            TripHeaderCard(
                "",
                TripHeaderSpec.VehicleOnTrip(vehicle, stop, null, false),
                stop.id,
                TripRouteAccents(route),
                now
            )
        }

        composeTestRule
            .onNodeWithTag("stop_pin_indicator", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun testAtTerminal() {
        val now = Instant.parse("2024-08-19T16:44:08-04:00")
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route = objects.route {}

        val predictionDeparture = now.plus(5.minutes)

        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt

                tripId = ""
                stopId = stop.id
                currentStopSequence = 0
            }
        val prediction = objects.prediction { departureTime = predictionDeparture }

        composeTestRule.setContent {
            TripHeaderCard(
                "",
                TripHeaderSpec.VehicleOnTrip(
                    vehicle,
                    stop,
                    TripDetailsStopList.Entry(
                        stop = stop,
                        stopSequence = 0,
                        disruption = null,
                        schedule = null,
                        prediction = prediction,
                        predictionStop = stop,
                        vehicle = vehicle,
                        routes = listOf()
                    ),
                    true
                ),
                stop.id,
                TripRouteAccents(route),
                now
            )
        }

        composeTestRule
            .onNodeWithText("Waiting to depart", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("stop_pin_indicator", useUnmergedTree = true)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(formatTime(predictionDeparture), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun testTrackNumber() {
        val now = Instant.parse("2024-08-19T16:44:08-04:00")
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { id = "place-north" }
        val platformStop =
            objects.stop {
                platformCode = "5"
                vehicleType = RouteType.COMMUTER_RAIL
                parentStationId = stop.id
            }
        val route = objects.route { type = RouteType.COMMUTER_RAIL }

        val predictionDeparture = now.plus(5.minutes)

        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt

                tripId = ""
                stopId = stop.id
                currentStopSequence = 0
            }
        val prediction =
            objects.prediction {
                departureTime = predictionDeparture
                stopId = platformStop.id
            }

        composeTestRule.setContent {
            TripHeaderCard(
                "",
                TripHeaderSpec.VehicleOnTrip(
                    vehicle,
                    stop,
                    TripDetailsStopList.Entry(
                        stop = stop,
                        stopSequence = 0,
                        disruption = null,
                        schedule = null,
                        prediction = prediction,
                        predictionStop = platformStop,
                        vehicle = vehicle,
                        routes = listOf()
                    ),
                    false
                ),
                stop.id,
                TripRouteAccents(route),
                now
            )
        }

        composeTestRule.onNodeWithText("Track 5", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun testScheduled() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "Stop Name" }

        val scheduledTime = now.plus(5.minutes)
        val route = objects.route {}

        val schedule = objects.schedule { departureTime = scheduledTime }

        composeTestRule.setContent {
            TripHeaderCard(
                "",
                TripHeaderSpec.Scheduled(
                    stop,
                    TripDetailsStopList.Entry(
                        stop = stop,
                        stopSequence = 0,
                        disruption = null,
                        schedule = schedule,
                        prediction = null,
                        predictionStop = null,
                        vehicle = null,
                        routes = listOf()
                    )
                ),
                stop.id,
                TripRouteAccents(route),
                now
            )
        }

        composeTestRule
            .onNodeWithText("Scheduled to depart", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stop.name, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(formatTime(scheduledTime), useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(formatTime(scheduledTime), useUnmergedTree = true)
            .assertIsDisplayed()
    }
    /*
    @Test
    fun testScheduledTap()  {
        val now = Clock.System.now()
                val objects = ObjectCollectionBuilder()
        val stop = objects.stop { _ in }

        val schedule = objects.schedule { schedule in
                schedule.departureTime = now.addingTimeInterval(5 * 60).toKotlinInstant()
        }

        val tapExpectation = expectation(description: "card tapped")

        val sut = TripHeaderCard(
                spec: .scheduled(stop, .init(
        stop: stop,
        stopSequence: 0,
        alert: null,
        schedule: schedule,
        prediction: null,
        vehicle: null,
        routes: []
        )),
        tripId: "",
        targetId: stop.id,
        routeAccents: .init(),
        onTap: { tapExpectation.fulfill() },
        now: now
        )

        try sut.inspect().find(ViewType.ZStack.self).callOnTapGesture()
            wait(for: [tapExpectation], timeout: 1)
            try XCTAssertNotnull(sut.inspect().find(ViewType.Image.self, where: { image in
                    try image.actualImage().name() == "fa-circle-info"
                    }))
            }
            */

    @Test
    fun testAccessibilityVehicleDescriptionSelectedStop() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "stop" }
        val vehicle = objects.vehicle { currentStatus = Vehicle.CurrentStatus.IncomingAt }
        val route = objects.route { type = RouteType.BUS }

        composeTestRule.setContent {
            TripHeaderCard(
                "",
                TripHeaderSpec.VehicleOnTrip(vehicle, stop, null, false),
                stop.id,
                TripRouteAccents(route),
                now
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Selected bus Approaching stop, selected stop",
                useUnmergedTree = true
            )
            .assertIsDisplayed()
    }

    @Test
    fun testAccessibilityVehicleDescriptionTrackNumber() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val stop =
            objects.stop {
                id = "place-rugg"
                name = "Ruggles"
            }
        val platformStop =
            objects.stop {
                platformCode = "3"
                vehicleType = RouteType.COMMUTER_RAIL
                parentStationId = stop.id
            }
        val vehicle = objects.vehicle { currentStatus = Vehicle.CurrentStatus.StoppedAt }
        val route = objects.route { type = RouteType.COMMUTER_RAIL }
        val prediction =
            objects.prediction {
                departureTime = now.plus(5.minutes)
                stopId = platformStop.id
            }

        composeTestRule.setContent {
            TripHeaderCard(
                "",
                TripHeaderSpec.VehicleOnTrip(
                    vehicle,
                    stop,
                    TripDetailsStopList.Entry(
                        stop = stop,
                        stopSequence = 0,
                        disruption = null,
                        schedule = null,
                        prediction = prediction,
                        predictionStop = platformStop,
                        vehicle = vehicle,
                        routes = listOf()
                    ),
                    false
                ),
                stop.id,
                TripRouteAccents(route),
                now
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Selected train Now at Ruggles, selected stop",
                useUnmergedTree = true
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Boarding on track 3", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun testAccessibilityVehicleDescriptionOtherStop() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "stop" }
        val vehicle = objects.vehicle { currentStatus = Vehicle.CurrentStatus.IncomingAt }
        val route = objects.route { type = RouteType.BUS }

        val otherStop = objects.stop { name = "other stop" }

        composeTestRule.setContent {
            TripHeaderCard(
                "",
                TripHeaderSpec.VehicleOnTrip(vehicle, otherStop, null, false),
                stop.id,
                TripRouteAccents(route),
                now
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Selected bus Approaching other stop",
                useUnmergedTree = true
            )
            .assertIsDisplayed()
    }

    @Test
    fun testAccessibilityVehicleDescriptionScheduledToDepartSelected() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "stop" }
        val vehicle = objects.vehicle { currentStatus = Vehicle.CurrentStatus.IncomingAt }
        val route = objects.route { type = RouteType.BUS }
        val schedule = objects.schedule { departureTime = now.plus(5.minutes) }

        composeTestRule.setContent {
            TripHeaderCard(
                "",
                TripHeaderSpec.Scheduled(
                    stop,
                    TripDetailsStopList.Entry(
                        stop = stop,
                        stopSequence = 0,
                        disruption = null,
                        schedule = schedule,
                        prediction = null,
                        predictionStop = null,
                        vehicle = null,
                        routes = listOf()
                    )
                ),
                stop.id,
                TripRouteAccents(route),
                now
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Selected bus scheduled to depart stop, selected stop",
                useUnmergedTree = true
            )
            .assertIsDisplayed()
    }

    @Test
    fun testAccessibilityVehicleDescriptionScheduledToDepartOther() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "stop" }
        val other = objects.stop { name = "other stop" }

        val vehicle = objects.vehicle { currentStatus = Vehicle.CurrentStatus.IncomingAt }
        val route = objects.route { type = RouteType.BUS }
        val schedule = objects.schedule { departureTime = now.plus(5.minutes) }

        composeTestRule.setContent {
            TripHeaderCard(
                "",
                TripHeaderSpec.Scheduled(
                    other,
                    TripDetailsStopList.Entry(
                        stop = other,
                        stopSequence = 0,
                        disruption = null,
                        schedule = schedule,
                        prediction = null,
                        predictionStop = null,
                        vehicle = null,
                        routes = listOf()
                    )
                ),
                stop.id,
                TripRouteAccents(route),
                now
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Selected bus scheduled to depart other stop",
                useUnmergedTree = true
            )
            .assertIsDisplayed()
    }
}
