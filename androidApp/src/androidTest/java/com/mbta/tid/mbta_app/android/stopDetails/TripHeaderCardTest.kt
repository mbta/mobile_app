package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.hasTextMatching
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripHeaderSpec
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Month
import org.junit.Rule
import org.junit.Test

class TripHeaderCardTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testDisplaysStopName() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "Stop Name" }
        val trip = objects.trip()
        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.InTransitTo
                tripId = trip.id
            }
        val route = objects.route {}

        composeTestRule.setContent {
            TripHeaderCard(
                trip,
                TripHeaderSpec.VehicleOnTrip(vehicle, stop, null, false),
                "",
                route,
                TripRouteAccents(route),
                now,
            )
        }

        composeTestRule.onNodeWithText(stop.name, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun testDisplayFollowButtonWhenFollowAction() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "Stop Name" }
        val trip = objects.trip()
        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.InTransitTo
                tripId = trip.id
            }
        val route = objects.route {}
        var followTripClicked = false

        composeTestRule.setContent {
            TripHeaderCard(
                trip,
                TripHeaderSpec.VehicleOnTrip(vehicle, stop, null, false),
                "",
                route,
                TripRouteAccents(route),
                now,
                onFollowTrip = { followTripClicked = true },
            )
        }

        composeTestRule.onNodeWithText("Follow").performClick()
        assertTrue(followTripClicked)
    }

    @Test
    fun testDisplaysStatusDescription() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val trip = objects.trip()

        val vehicleState =
            mutableStateOf<Vehicle>(
                objects.vehicle {
                    currentStatus = Vehicle.CurrentStatus.InTransitTo
                    tripId = trip.id
                }
            )
        val route = objects.route {}

        composeTestRule.setContent {
            val vehicle: Vehicle by vehicleState
            TripHeaderCard(
                trip,
                TripHeaderSpec.VehicleOnTrip(vehicle, stop, null, false),
                "",
                route,
                TripRouteAccents(route),
                now,
            )
        }

        composeTestRule.onNodeWithText("Next stop", useUnmergedTree = true).assertIsDisplayed()

        val incomingVehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.IncomingAt
                tripId = trip.id
            }

        vehicleState.value = incomingVehicle
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Approaching", useUnmergedTree = true).assertIsDisplayed()

        val stoppedVehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                tripId = trip.id
            }

        vehicleState.value = stoppedVehicle

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Now at", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun testDisplaysBusCrowding() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val trip = objects.trip()

        val vehicleState =
            mutableStateOf<Vehicle>(
                objects.vehicle {
                    currentStatus = Vehicle.CurrentStatus.InTransitTo
                    tripId = trip.id
                    occupancyStatus = Vehicle.OccupancyStatus.ManySeatsAvailable
                }
            )
        val route = objects.route { type = RouteType.BUS }

        composeTestRule.setContent {
            val vehicle: Vehicle by vehicleState
            TripHeaderCard(
                trip,
                TripHeaderSpec.VehicleOnTrip(vehicle, stop, null, false),
                "",
                route,
                TripRouteAccents(route),
                now,
            )
        }

        composeTestRule.onNodeWithText("Not crowded", useUnmergedTree = true).assertIsDisplayed()

        vehicleState.value =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.InTransitTo
                tripId = trip.id
                occupancyStatus = Vehicle.OccupancyStatus.FewSeatsAvailable
            }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Some crowding", useUnmergedTree = true).assertIsDisplayed()

        vehicleState.value =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.InTransitTo
                tripId = trip.id
                occupancyStatus = Vehicle.OccupancyStatus.CrushedStandingRoomOnly
            }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Crowded", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun testDifferentTrip() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "Stop Name" }
        val route = objects.route {}
        val trip = objects.trip()

        composeTestRule.setContent {
            TripHeaderCard(
                trip,
                TripHeaderSpec.FinishingAnotherTrip,
                "",
                route,
                TripRouteAccents(route),
                now,
            )
        }

        composeTestRule.onNodeWithText("Finishing another trip").assertIsDisplayed()
        composeTestRule.onNodeWithText(stop.name, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun testNoVehicle() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "Stop Name" }
        val route = objects.route {}
        val trip = objects.trip()

        composeTestRule.setContent {
            TripHeaderCard(trip, TripHeaderSpec.NoVehicle, "", route, TripRouteAccents(route), now)
        }
        composeTestRule
            .onNodeWithText("Location not available yet", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stop.name, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun testAtTarget() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val trip = objects.trip()

        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                tripId = trip.id
                stopId = stop.id
            }

        val route = objects.route {}

        composeTestRule.setContent {
            TripHeaderCard(
                trip,
                TripHeaderSpec.VehicleOnTrip(vehicle, stop, null, false),
                stop.id,
                route,
                TripRouteAccents(route),
                now,
            )
        }

        composeTestRule
            .onNodeWithTag("stop_pin_indicator", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun testAtTerminal() {
        val now = EasternTimeInstant(2024, Month.AUGUST, 19, 16, 44, 8)
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route = objects.route {}
        val trip = objects.trip()

        val predictionDeparture = now.plus(5.minutes)

        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt

                tripId = trip.id
                stopId = stop.id
                currentStopSequence = 0
            }
        val prediction = objects.prediction { departureTime = predictionDeparture }

        composeTestRule.setContent {
            TripHeaderCard(
                trip,
                TripHeaderSpec.VehicleOnTrip(
                    vehicle,
                    stop,
                    TripDetailsStopList.Entry(
                        stop = stop,
                        stopSequence = 0,
                        disruption = null,
                        schedule = null,
                        prediction = prediction,
                        vehicle = vehicle,
                        routes = listOf(),
                    ),
                    true,
                ),
                stop.id,
                route,
                TripRouteAccents(route),
                now,
            )
        }

        composeTestRule
            .onNodeWithText("Waiting to depart", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("stop_pin_indicator", useUnmergedTree = true)
            .assertIsDisplayed()

        composeTestRule
            .onNode(hasTextMatching(Regex("4:49\\sPM")), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun testTrackNumber() {
        val now = EasternTimeInstant(2024, Month.AUGUST, 19, 16, 44, 8)
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { id = "place-north" }
        val platformStop =
            objects.stop {
                platformCode = "5"
                vehicleType = RouteType.COMMUTER_RAIL
                parentStationId = stop.id
            }
        val route = objects.route { type = RouteType.COMMUTER_RAIL }
        val trip = objects.trip()

        val predictionDeparture = now.plus(5.minutes)

        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt

                tripId = trip.id
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
                trip,
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
                        routes = listOf(),
                    ),
                    false,
                ),
                stop.id,
                route,
                TripRouteAccents(route),
                now,
            )
        }

        composeTestRule.onNodeWithText("Track 5", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun testScheduled() {
        val now = EasternTimeInstant(2025, Month.JULY, 29, 9, 43)
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "Stop Name" }
        val trip = objects.trip()

        val scheduledTime = now.plus(5.minutes)
        val route = objects.route { type = RouteType.COMMUTER_RAIL }

        val schedule =
            objects.schedule {
                this.trip = trip
                departureTime = scheduledTime
            }

        composeTestRule.setContent {
            TripHeaderCard(
                trip,
                TripHeaderSpec.Scheduled(
                    stop,
                    TripDetailsStopList.Entry(
                        stop = stop,
                        stopSequence = 0,
                        disruption = null,
                        schedule = schedule,
                        prediction = null,
                        vehicle = null,
                        routes = listOf(),
                    ),
                ),
                stop.id,
                route,
                TripRouteAccents(route),
                now,
            )
        }

        composeTestRule
            .onNodeWithText("Scheduled to depart", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stop.name, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule
            .onNode(hasTextMatching(Regex("9:48\\sAM")), useUnmergedTree = true)
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
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "stop" }
        val trip = objects.trip()
        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.IncomingAt
                tripId = trip.id
            }
        val route = objects.route { type = RouteType.BUS }

        composeTestRule.setContent {
            TripHeaderCard(
                trip,
                TripHeaderSpec.VehicleOnTrip(vehicle, stop, null, false),
                stop.id,
                route,
                TripRouteAccents(route),
                now,
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Selected bus Approaching stop, selected stop",
                useUnmergedTree = true,
            )
            .assertIsDisplayed()
    }

    @Test
    fun testAccessibilityVehicleDescriptionTrackNumber() {
        val now = EasternTimeInstant.now()
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
        val trip = objects.trip()
        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                tripId = trip.id
            }
        val route = objects.route { type = RouteType.COMMUTER_RAIL }
        val prediction =
            objects.prediction {
                departureTime = now.plus(5.minutes)
                stopId = platformStop.id
            }

        composeTestRule.setContent {
            TripHeaderCard(
                trip,
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
                        routes = listOf(),
                    ),
                    false,
                ),
                stop.id,
                route,
                TripRouteAccents(route),
                now,
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Selected train Now at Ruggles, selected stop",
                useUnmergedTree = true,
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Boarding on track 3", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun testAccessibilityVehicleDescriptionOtherStop() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "stop" }
        val trip = objects.trip()
        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.IncomingAt
                tripId = trip.id
            }
        val route = objects.route { type = RouteType.BUS }

        val otherStop = objects.stop { name = "other stop" }

        composeTestRule.setContent {
            TripHeaderCard(
                trip,
                TripHeaderSpec.VehicleOnTrip(vehicle, otherStop, null, false),
                stop.id,
                route,
                TripRouteAccents(route),
                now,
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Selected bus Approaching other stop",
                useUnmergedTree = true,
            )
            .assertIsDisplayed()
    }

    @Test
    fun testAccessibilityVehicleDescriptionScheduledToDepartSelected() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "stop" }
        val trip = objects.trip()
        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.IncomingAt
                tripId = trip.id
            }
        val route = objects.route { type = RouteType.BUS }
        val schedule = objects.schedule { departureTime = now.plus(5.minutes) }

        composeTestRule.setContent {
            TripHeaderCard(
                trip,
                TripHeaderSpec.Scheduled(
                    stop,
                    TripDetailsStopList.Entry(
                        stop = stop,
                        stopSequence = 0,
                        disruption = null,
                        schedule = schedule,
                        prediction = null,
                        vehicle = vehicle,
                        routes = listOf(),
                    ),
                ),
                stop.id,
                route,
                TripRouteAccents(route),
                now,
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Selected bus scheduled to depart stop, selected stop",
                useUnmergedTree = true,
            )
            .assertIsDisplayed()
    }

    @Test
    fun testAccessibilityVehicleDescriptionScheduledToDepartOther() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "stop" }
        val other = objects.stop { name = "other stop" }

        val trip = objects.trip()
        val vehicle =
            objects.vehicle {
                currentStatus = Vehicle.CurrentStatus.IncomingAt
                tripId = trip.id
            }
        val route = objects.route { type = RouteType.BUS }
        val schedule = objects.schedule { departureTime = now.plus(5.minutes) }

        composeTestRule.setContent {
            TripHeaderCard(
                trip,
                TripHeaderSpec.Scheduled(
                    other,
                    TripDetailsStopList.Entry(
                        stop = other,
                        stopSequence = 0,
                        disruption = null,
                        schedule = schedule,
                        prediction = null,
                        vehicle = vehicle,
                        routes = listOf(),
                    ),
                ),
                stop.id,
                route,
                TripRouteAccents(route),
                now,
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Selected bus scheduled to depart other stop",
                useUnmergedTree = true,
            )
            .assertIsDisplayed()
    }
}
