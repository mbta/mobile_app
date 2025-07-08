package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Prediction
import com.mbta.tid.mbta_app.model.Schedule
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripHeaderSpec
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import org.junit.Rule
import org.junit.Test

class TripStopsTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testDisplaysSplitStops() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val pattern = objects.routePattern(route)
        val trip = objects.trip(pattern)

        val stop1 = objects.stop { name = "Stop A" }
        val stop2 = objects.stop { name = "Stop B" }
        val stop3Target = objects.stop { name = "Stop C" }
        val stop4 = objects.stop { name = "Stop D" }
        val stop5 = objects.stop { name = "Stop E" }

        val vehicle =
            objects.vehicle {
                tripId = trip.id
                routeId = route.id
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                stopId = stop1.id
            }

        fun makeSchedule(stop: Stop) =
            objects.schedule {
                routeId = route.id
                stopId = stop.id
                this.trip = trip
            }

        var predictionTime = now
        fun makePrediction(schedule: Schedule): Prediction {
            predictionTime += 5.seconds
            return objects.prediction(schedule) {
                departureTime = predictionTime
                vehicleId = vehicle.id
            }
        }

        val schedule1 = makeSchedule(stop1)
        val prediction1 = makePrediction(schedule1)
        val schedule2 = makeSchedule(stop2)
        val prediction2 = makePrediction(schedule2)
        val schedule3 = makeSchedule(stop3Target)
        val prediction3 = makePrediction(schedule3)
        val schedule4 = makeSchedule(stop4)
        val prediction4 = makePrediction(schedule4)
        val schedule5 = makeSchedule(stop5)
        val prediction5 = makePrediction(schedule5)

        val stops =
            TripDetailsStopList(
                trip,
                listOf(
                    TripDetailsStopList.Entry(
                        stop1,
                        stopSequence = 1,
                        disruption = null,
                        schedule1,
                        prediction1,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                    TripDetailsStopList.Entry(
                        stop2,
                        stopSequence = 2,
                        disruption = null,
                        schedule2,
                        prediction2,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                    TripDetailsStopList.Entry(
                        stop3Target,
                        stopSequence = 3,
                        disruption = null,
                        schedule3,
                        prediction3,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                    TripDetailsStopList.Entry(
                        stop4,
                        stopSequence = 4,
                        disruption = null,
                        schedule4,
                        prediction4,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                    TripDetailsStopList.Entry(
                        stop5,
                        stopSequence = 5,
                        disruption = null,
                        schedule5,
                        prediction5,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                ),
            )

        composeTestRule.setContent {
            TripStops(
                targetId = stop3Target.id,
                stops,
                stopSequence = 1,
                TripHeaderSpec.VehicleOnTrip(vehicle, stop1, null, false),
                now,
                onTapLink = {},
                onOpenAlertDetails = {},
                routeAccents = TripRouteAccents(route),
                alertSummaries = emptyMap(),
                global = GlobalResponse(objects),
            )
        }

        composeTestRule.onNodeWithText("2 stops away", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop3Target.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop5.name).assertIsDisplayed()
    }

    @Test
    fun testDisplaysUnsplitStops() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val pattern = objects.routePattern(route)
        val trip = objects.trip(pattern)

        val stopTarget = objects.stop()
        val stop1 = objects.stop { name = "Stop A" }
        val stop2 = objects.stop { name = "Stop B" }
        val stop3 = objects.stop { name = "Stop C" }

        val vehicle =
            objects.vehicle {
                tripId = trip.id
                routeId = route.id
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                stopId = stop1.id
            }

        fun makeSchedule(stop: Stop) =
            objects.schedule {
                routeId = route.id
                stopId = stop.id
                this.trip = trip
            }

        var predictionTime = now
        fun makePrediction(schedule: Schedule): Prediction {
            predictionTime += 5.seconds
            return objects.prediction(schedule) {
                departureTime = predictionTime
                vehicleId = vehicle.id
            }
        }

        val schedule1 = makeSchedule(stop1)
        val prediction1 = makePrediction(schedule1)
        val schedule2 = makeSchedule(stop2)
        val prediction2 = makePrediction(schedule2)
        val schedule3 = makeSchedule(stop3)
        val prediction3 = makePrediction(schedule3)

        val stops =
            TripDetailsStopList(
                trip,
                listOf(
                    TripDetailsStopList.Entry(
                        stop1,
                        stopSequence = 1,
                        disruption = null,
                        schedule1,
                        prediction1,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                    TripDetailsStopList.Entry(
                        stop2,
                        stopSequence = 2,
                        disruption = null,
                        schedule2,
                        prediction2,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                    TripDetailsStopList.Entry(
                        stop3,
                        stopSequence = 3,
                        disruption = null,
                        schedule3,
                        prediction3,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                ),
            )

        composeTestRule.setContent {
            TripStops(
                targetId = stopTarget.id,
                stops,
                stopSequence = 0,
                TripHeaderSpec.VehicleOnTrip(vehicle, stop1, null, false),
                now,
                onTapLink = {},
                onOpenAlertDetails = {},
                routeAccents = TripRouteAccents(route),
                alertSummaries = emptyMap(),
                global = GlobalResponse(objects),
            )
        }

        composeTestRule.onNodeWithText(stop1.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop2.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop3.name).assertIsDisplayed()
    }

    @Test
    fun testTargetHidden() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val pattern = objects.routePattern(route)
        val trip = objects.trip(pattern)

        val stop1 = objects.stop { name = "Stop A" }
        val stop2 = objects.stop { name = "Stop B" }

        val vehicle =
            objects.vehicle {
                tripId = trip.id
                routeId = route.id
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                stopId = stop1.id
            }

        fun makeSchedule(stop: Stop) =
            objects.schedule {
                routeId = route.id
                stopId = stop.id
                this.trip = trip
            }

        var predictionTime = now
        fun makePrediction(schedule: Schedule): Prediction {
            predictionTime += 5.seconds
            return objects.prediction(schedule) {
                departureTime = predictionTime
                vehicleId = vehicle.id
            }
        }

        val schedule1 = makeSchedule(stop1)
        val prediction1 = makePrediction(schedule1)
        val schedule2 = makeSchedule(stop2)
        val prediction2 = makePrediction(schedule2)

        val firstStop: TripDetailsStopList.Entry =
            TripDetailsStopList.Entry(
                stop1,
                stopSequence = 1,
                disruption = null,
                schedule1,
                prediction1,
                vehicle = vehicle,
                routes = listOf(route),
            )
        val stops =
            TripDetailsStopList(
                trip,
                listOf(
                    firstStop,
                    TripDetailsStopList.Entry(
                        stop2,
                        stopSequence = 2,
                        disruption = null,
                        schedule2,
                        prediction2,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                ),
                startTerminalEntry = firstStop,
            )

        composeTestRule.setContent {
            TripStops(
                targetId = stop1.id,
                stops,
                stopSequence = 1,
                TripHeaderSpec.Scheduled(stop1, firstStop),
                now,
                onTapLink = {},
                onOpenAlertDetails = {},
                routeAccents = TripRouteAccents(route),
                alertSummaries = emptyMap(),
                global = GlobalResponse(objects),
            )
        }

        composeTestRule.onNodeWithText(stop1.name).assertDoesNotExist()
    }

    @Test
    fun testFirstStopSeparated() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val pattern = objects.routePattern(route)
        val trip = objects.trip(pattern)

        val stop1 = objects.stop { name = "Stop A" }
        val stop2 = objects.stop { name = "Stop B" }
        val stop3 = objects.stop { name = "Stop C" }

        val vehicle =
            objects.vehicle {
                tripId = "different"
                routeId = route.id
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                stopId = stop1.id
            }

        fun makeSchedule(stop: Stop) =
            objects.schedule {
                routeId = route.id
                stopId = stop.id
                this.trip = trip
            }

        var predictionTime = now
        fun makePrediction(schedule: Schedule): Prediction {
            predictionTime += 5.seconds
            return objects.prediction(schedule) {
                departureTime = predictionTime
                vehicleId = vehicle.id
            }
        }

        val schedule1 = makeSchedule(stop1)
        val prediction1 = makePrediction(schedule1)
        val schedule2 = makeSchedule(stop2)
        val prediction2 = makePrediction(schedule2)
        val schedule3 = makeSchedule(stop3)
        val prediction3 = makePrediction(schedule3)

        val firstStop: TripDetailsStopList.Entry =
            TripDetailsStopList.Entry(
                stop1,
                stopSequence = 1,
                disruption = null,
                schedule1,
                prediction1,
                vehicle = vehicle,
                routes = listOf(route),
            )
        val stops =
            TripDetailsStopList(
                trip,
                listOf(
                    firstStop,
                    TripDetailsStopList.Entry(
                        stop2,
                        stopSequence = 2,
                        disruption = null,
                        schedule2,
                        prediction2,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                    TripDetailsStopList.Entry(
                        stop3,
                        stopSequence = 3,
                        disruption = null,
                        schedule3,
                        prediction3,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                ),
                startTerminalEntry = firstStop,
            )

        composeTestRule.setContent {
            TripStops(
                targetId = stop3.id,
                stops,
                stopSequence = 3,
                TripHeaderSpec.FinishingAnotherTrip,
                now,
                onTapLink = {},
                onOpenAlertDetails = {},
                routeAccents = TripRouteAccents(route),
                alertSummaries = emptyMap(),
                global = GlobalResponse(objects),
            )
        }

        composeTestRule.onNodeWithText(stop1.name).assertIsDisplayed()
        composeTestRule.onNodeWithText("1 stop away", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun testDownstreamShuttleAlert() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val pattern = objects.routePattern(route)
        val trip = objects.trip(pattern)

        val stop1 = objects.stop { name = "Stop A" }
        val stop2Target = objects.stop { name = "Stop B" }
        val stop3 = objects.stop { name = "Stop C" }

        val alert = objects.alert { effect = Alert.Effect.Shuttle }

        val vehicle =
            objects.vehicle {
                tripId = trip.id
                routeId = route.id
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                stopId = stop1.id
            }

        fun makeSchedule(stop: Stop) =
            objects.schedule {
                routeId = route.id
                stopId = stop.id
                this.trip = trip
            }

        var predictionTime = now
        fun makePrediction(schedule: Schedule): Prediction {
            predictionTime += 5.seconds
            return objects.prediction(schedule) {
                departureTime = predictionTime
                vehicleId = vehicle.id
            }
        }

        val schedule1 = makeSchedule(stop1)
        val prediction1 = makePrediction(schedule1)
        val schedule2 = makeSchedule(stop2Target)
        val prediction2 = makePrediction(schedule2)
        val schedule3 = makeSchedule(stop3)
        val prediction3 = makePrediction(schedule3)

        val stops =
            TripDetailsStopList(
                trip,
                listOf(
                    TripDetailsStopList.Entry(
                        stop1,
                        stopSequence = 1,
                        disruption = null,
                        schedule1,
                        prediction1,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                    TripDetailsStopList.Entry(
                        stop2Target,
                        stopSequence = 2,
                        disruption = null,
                        schedule2,
                        prediction2,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                    TripDetailsStopList.Entry(
                        stop3,
                        stopSequence = 3,
                        disruption = UpcomingFormat.Disruption(alert, null),
                        schedule3,
                        prediction3,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                ),
            )

        composeTestRule.setContent {
            TripStops(
                targetId = stop2Target.id,
                stops,
                stopSequence = 1,
                TripHeaderSpec.VehicleOnTrip(vehicle, stop1, null, false),
                now,
                onTapLink = {},
                onOpenAlertDetails = {},
                routeAccents = TripRouteAccents(route),
                alertSummaries =
                    mapOf(
                        alert.id to
                            AlertSummary(
                                alert.effect,
                                AlertSummary.Location.SingleStop(stop3.name),
                                AlertSummary.Timeframe.EndOfService,
                            )
                    ),
                global = GlobalResponse(objects),
            )
        }

        composeTestRule
            .onNodeWithText("Shuttle buses at Stop C through end of service")
            .assertIsDisplayed()
    }

    @Test
    fun testDownstreamClosureAlert() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val pattern = objects.routePattern(route)
        val trip = objects.trip(pattern)

        val stop1 = objects.stop { name = "Stop A" }
        val stop2Target = objects.stop { name = "Stop B" }
        val stop3 = objects.stop { name = "Stop C" }

        val alert = objects.alert { effect = Alert.Effect.StopClosure }

        val vehicle =
            objects.vehicle {
                tripId = trip.id
                routeId = route.id
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                stopId = stop1.id
            }

        fun makeSchedule(stop: Stop) =
            objects.schedule {
                routeId = route.id
                stopId = stop.id
                this.trip = trip
            }

        var predictionTime = now
        fun makePrediction(schedule: Schedule): Prediction {
            predictionTime += 5.seconds
            return objects.prediction(schedule) {
                departureTime = predictionTime
                vehicleId = vehicle.id
            }
        }

        val schedule1 = makeSchedule(stop1)
        val prediction1 = makePrediction(schedule1)
        val schedule2 = makeSchedule(stop2Target)
        val prediction2 = makePrediction(schedule2)
        val schedule3 = makeSchedule(stop3)
        val prediction3 = makePrediction(schedule3)

        val stops =
            TripDetailsStopList(
                trip,
                listOf(
                    TripDetailsStopList.Entry(
                        stop1,
                        stopSequence = 1,
                        disruption = null,
                        schedule1,
                        prediction1,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                    TripDetailsStopList.Entry(
                        stop2Target,
                        stopSequence = 2,
                        disruption = null,
                        schedule2,
                        prediction2,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                    TripDetailsStopList.Entry(
                        stop3,
                        stopSequence = 3,
                        disruption = UpcomingFormat.Disruption(alert, null),
                        schedule3,
                        prediction3,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                ),
            )

        composeTestRule.setContent {
            TripStops(
                targetId = stop2Target.id,
                stops,
                stopSequence = 1,
                TripHeaderSpec.VehicleOnTrip(vehicle, stop1, null, false),
                now,
                onTapLink = {},
                onOpenAlertDetails = {},
                routeAccents = TripRouteAccents(route),
                alertSummaries =
                    mapOf(
                        alert.id to
                            AlertSummary(
                                alert.effect,
                                AlertSummary.Location.SingleStop(stop3.name),
                                AlertSummary.Timeframe.EndOfService,
                            )
                    ),
                global = GlobalResponse(objects),
            )
        }

        composeTestRule
            .onNodeWithText("Stop closed at Stop C through end of service")
            .assertIsNotDisplayed()
    }

    @Test
    fun testUpstreamAlert() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val pattern = objects.routePattern(route)
        val trip = objects.trip(pattern)

        val stop1 = objects.stop { name = "Stop A" }
        val stop2Target = objects.stop { name = "Stop B" }
        val stop3 = objects.stop { name = "Stop C" }

        val alert = objects.alert { effect = Alert.Effect.StopClosure }

        val vehicle =
            objects.vehicle {
                tripId = trip.id
                routeId = route.id
                currentStatus = Vehicle.CurrentStatus.StoppedAt
                stopId = stop1.id
            }

        fun makeSchedule(stop: Stop) =
            objects.schedule {
                routeId = route.id
                stopId = stop.id
                this.trip = trip
            }

        var predictionTime = now
        fun makePrediction(schedule: Schedule): Prediction {
            predictionTime += 5.seconds
            return objects.prediction(schedule) {
                departureTime = predictionTime
                vehicleId = vehicle.id
            }
        }

        val schedule1 = makeSchedule(stop1)
        val prediction1 = makePrediction(schedule1)
        val schedule2 = makeSchedule(stop2Target)
        val prediction2 = makePrediction(schedule2)
        val schedule3 = makeSchedule(stop3)
        val prediction3 = makePrediction(schedule3)

        val stops =
            TripDetailsStopList(
                trip,
                listOf(
                    TripDetailsStopList.Entry(
                        stop1,
                        stopSequence = 1,
                        disruption = UpcomingFormat.Disruption(alert, null),
                        schedule1,
                        prediction1,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                    TripDetailsStopList.Entry(
                        stop2Target,
                        stopSequence = 2,
                        disruption = null,
                        schedule2,
                        prediction2,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                    TripDetailsStopList.Entry(
                        stop3,
                        stopSequence = 3,
                        disruption = null,
                        schedule3,
                        prediction3,
                        vehicle = vehicle,
                        routes = listOf(route),
                    ),
                ),
            )

        composeTestRule.setContent {
            TripStops(
                targetId = stop2Target.id,
                stops,
                stopSequence = 1,
                TripHeaderSpec.VehicleOnTrip(vehicle, stop1, null, false),
                now,
                onTapLink = {},
                onOpenAlertDetails = {},
                routeAccents = TripRouteAccents(route),
                alertSummaries =
                    mapOf(
                        alert.id to
                            AlertSummary(
                                alert.effect,
                                AlertSummary.Location.SingleStop(stop1.name),
                                AlertSummary.Timeframe.EndOfService,
                            )
                    ),
                global = GlobalResponse(objects),
            )
        }

        composeTestRule.onNodeWithText("Stop closed", substring = true).assertDoesNotExist()
    }
}
