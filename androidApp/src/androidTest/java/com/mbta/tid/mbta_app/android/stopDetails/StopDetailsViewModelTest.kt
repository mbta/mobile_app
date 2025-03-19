package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.model.response.TripResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import com.mbta.tid.mbta_app.model.response.VehicleStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IdleScheduleRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockScheduleRepository
import com.mbta.tid.mbta_app.repositories.MockTripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockTripRepository
import com.mbta.tid.mbta_app.repositories.MockVehicleRepository
import junit.framework.TestCase.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test

class StopDetailsViewModelTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testLoadStopDetailsLoadsSchedulesAndPredictions() = runTest {
        val objects = ObjectCollectionBuilder()
        objects.prediction {}
        objects.trip {}
        objects.vehicle { currentStatus = Vehicle.CurrentStatus.InTransitTo }
        objects.schedule {}
        objects.trip {}

        val predictionsRepo =
            MockPredictionsRepository(
                connectV2Outcome = ApiResult.Ok(PredictionsByStopJoinResponse(objects))
            )

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val errorBannerRepo = MockErrorBannerStateRepository()

        val viewModel = StopDetailsViewModel.mocked(errorBannerRepo, predictionsRepo, schedulesRepo)

        composeTestRule.setContent { LaunchedEffect(Unit) { viewModel.loadStopDetails("stop") } }

        composeTestRule.waitUntil { viewModel.stopData?.value?.predictionsByStop != null }

        assertEquals("stop", viewModel.stopData.value?.stopId)
        assertNotNull(viewModel.stopData?.value?.predictionsByStop)
        assertNotNull(viewModel.stopData?.value?.schedules != null)
    }

    @Test
    fun testRejoinStopPredictionsWhenStopDataSet() = runTest {
        val objects = ObjectCollectionBuilder()
        objects.prediction {}
        objects.trip {}
        objects.vehicle { currentStatus = Vehicle.CurrentStatus.InTransitTo }
        objects.schedule {}
        objects.trip {}

        var connectCount = 0

        val predictionsRepo =
            MockPredictionsRepository(
                connectV2Outcome = ApiResult.Ok(PredictionsByStopJoinResponse(objects)),
                onConnectV2 = { connectCount += 1 }
            )

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val errorBannerRepo = MockErrorBannerStateRepository()

        val viewModel = StopDetailsViewModel.mocked(errorBannerRepo, predictionsRepo, schedulesRepo)

        composeTestRule.setContent { LaunchedEffect(Unit) { viewModel.loadStopDetails("stop") } }

        composeTestRule.waitUntil { connectCount == 1 }

        assertEquals(viewModel.stopData.value?.stopId, "stop")
        assertNotNull(viewModel.stopData?.value?.predictionsByStop)
        assertEquals(1, connectCount)
        viewModel.rejoinStopPredictions()
        composeTestRule.awaitIdle()

        composeTestRule.waitUntil { connectCount == 2 }
        assertEquals(2, connectCount)
    }

    @Test
    fun testRejoinStopPredictionsWhenNoStop() = runTest {
        val objects = ObjectCollectionBuilder()
        objects.prediction {}
        objects.trip {}
        objects.vehicle { currentStatus = Vehicle.CurrentStatus.InTransitTo }
        objects.schedule {}
        objects.trip {}

        var connectCount = 0

        val predictionsRepo =
            MockPredictionsRepository(
                connectV2Outcome = ApiResult.Ok(PredictionsByStopJoinResponse(objects)),
                onConnectV2 = { connectCount += 1 }
            )

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val errorBannerRepo = MockErrorBannerStateRepository()

        val viewModel = StopDetailsViewModel.mocked(errorBannerRepo, predictionsRepo, schedulesRepo)

        composeTestRule.setContent { LaunchedEffect(Unit) { viewModel.loadStopDetails("stop") } }

        composeTestRule.waitUntil { connectCount == 1 }

        assertEquals(viewModel.stopData.value?.stopId, "stop")
        assertNotNull(viewModel.stopData?.value?.predictionsByStop)
        assertEquals(1, connectCount)

        viewModel.handleStopChange(null)
        composeTestRule.awaitIdle()

        assertNull(viewModel.stopData?.value)
        viewModel.rejoinStopPredictions()
        // nothing to rejoin
        assertEquals(1, connectCount)
    }

    @Test
    fun testLeaveStopPredictions() = runTest {
        val objects = ObjectCollectionBuilder()
        objects.prediction {}
        objects.trip {}
        objects.vehicle { currentStatus = Vehicle.CurrentStatus.InTransitTo }
        objects.schedule {}
        objects.trip {}

        var connectCount = 0
        var disconnectCount = 0

        val predictionsRepo =
            MockPredictionsRepository(
                connectV2Outcome = ApiResult.Ok(PredictionsByStopJoinResponse(objects)),
                onConnectV2 = { connectCount += 1 },
                onDisconnect = { disconnectCount += 1 }
            )

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val errorBannerRepo = MockErrorBannerStateRepository()

        val viewModel = StopDetailsViewModel.mocked(errorBannerRepo, predictionsRepo, schedulesRepo)

        composeTestRule.setContent { LaunchedEffect(Unit) { viewModel.loadStopDetails("stop") } }

        composeTestRule.awaitIdle()

        assertEquals(viewModel.stopData.value?.stopId, "stop")
        assertNotNull(viewModel.stopData?.value?.predictionsByStop)
        assertEquals(1, connectCount)
        assertEquals(0, disconnectCount)

        viewModel.leaveStopPredictions()

        composeTestRule.waitUntil { disconnectCount == 1 }
        assertEquals(1, disconnectCount)
    }

    @Test
    fun testSetDepartures() {
        val predictionsRepo = MockPredictionsRepository()

        val schedulesRepo = MockScheduleRepository()

        val errorBannerRepo = MockErrorBannerStateRepository()

        val viewModel = StopDetailsViewModel.mocked(errorBannerRepo, predictionsRepo, schedulesRepo)

        assertNull(viewModel.stopDepartures.value)

        viewModel.setDepartures(StopDetailsDepartures(emptyList()))

        assertEquals(emptyList<PatternsByStop>(), viewModel.stopDepartures.value?.routes)
    }

    @Test
    fun testHandleStopChange() = runTest {
        val objects = ObjectCollectionBuilder()

        var connectCount = 0
        var disconnectCount = 0

        val predictionsRepo =
            MockPredictionsRepository(
                connectV2Outcome = ApiResult.Ok(PredictionsByStopJoinResponse(objects)),
                onConnectV2 = { connectCount += 1 },
                onDisconnect = { disconnectCount += 1 }
            )

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val errorBannerRepo = MockErrorBannerStateRepository()

        val viewModel = StopDetailsViewModel.mocked(errorBannerRepo, predictionsRepo, schedulesRepo)

        composeTestRule.setContent {
            LaunchedEffect(Unit) {
                viewModel.loadStopDetails("stop1")
                viewModel.handleStopChange("stop2")
            }
        }

        composeTestRule.waitUntil { connectCount == 2 }

        assertEquals(1, disconnectCount)
        assertEquals(2, connectCount)
        assertEquals("stop2", viewModel.stopData.value?.stopId)
        assertNotNull(viewModel.stopData?.value?.predictionsByStop)
        assertNotNull(viewModel.stopData?.value?.schedules)
    }

    @Test
    fun testLoadTripData() = runTest {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route = objects.route()

        val pattern =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip { headsign = "0" }
            }
        val trip1 = objects.trip(pattern) { headsign = "0" }
        val schedule =
            objects.schedule {
                trip = trip1
                routeId = route.id
                stopId = stop.id
            }
        val vehicle =
            objects.vehicle {
                tripId = trip1.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
                stopId = stop.id
                currentStopSequence = 0
            }

        val tripPredictions = PredictionsStreamDataResponse(objects)
        val tripSchedules = TripSchedulesResponse.Schedules(listOf(schedule))

        var tripPredictionsConnectedCount = 0
        var tripPredictionsDisconnectedCount = 0
        var vehicleConnectedCount = 0
        var vehicleDisconnectedCount = 0

        val viewModel =
            StopDetailsViewModel.mocked(
                tripPredictionsRepo =
                    MockTripPredictionsRepository(
                        { tripPredictionsConnectedCount += 1 },
                        { tripPredictionsDisconnectedCount += 1 },
                        tripPredictions
                    ),
                tripRepo =
                    MockTripRepository(
                        tripSchedulesResponse = tripSchedules,
                        tripResponse = TripResponse(trip1)
                    ),
                vehicleRepo =
                    MockVehicleRepository(
                        { vehicleConnectedCount += 1 },
                        { vehicleDisconnectedCount += 1 },
                        ApiResult.Ok(VehicleStreamDataResponse(vehicle))
                    )
            )

        val tripFilter = TripDetailsFilter(trip1.id, vehicle.id, 0, false)
        composeTestRule.setContent {
            LaunchedEffect(Unit) { viewModel.handleTripFilterChange(tripFilter) }
        }

        composeTestRule.waitUntil { viewModel.tripData.value != null }
        assertEquals(tripFilter, viewModel.tripData.value?.tripFilter)
        assertEquals(trip1, viewModel.tripData.value?.trip)

        composeTestRule.waitUntil { viewModel.tripData.value?.tripSchedules != null }
        assertEquals(tripSchedules, viewModel.tripData.value?.tripSchedules)
        composeTestRule.waitUntil { viewModel.tripData.value?.tripPredictions != null }
        assertEquals(tripPredictions, viewModel.tripData.value?.tripPredictions)
        composeTestRule.waitUntil { viewModel.tripData.value?.vehicle != null }
        assertEquals(vehicle, viewModel.tripData.value?.vehicle)

        assertEquals(true, viewModel.tripData.value?.tripPredictionsLoaded)
        assertEquals(2, tripPredictionsDisconnectedCount)
        assertEquals(1, tripPredictionsConnectedCount)
        assertEquals(2, vehicleDisconnectedCount)
        assertEquals(1, vehicleConnectedCount)

        viewModel.clearTripDetails()

        composeTestRule.waitUntil { viewModel.tripData.value == null }
        assertNull(viewModel.tripData.value)

        assertEquals(3, tripPredictionsDisconnectedCount)
        assertEquals(3, vehicleDisconnectedCount)
    }

    @Test
    fun testSkipLoadingTripData() = runTest {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route = objects.route()

        val pattern =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip { headsign = "0" }
            }
        val trip1 = objects.trip(pattern) { headsign = "0" }
        val schedule =
            objects.schedule {
                trip = trip1
                routeId = route.id
                stopId = stop.id
            }
        val vehicle =
            objects.vehicle {
                tripId = trip1.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
                stopId = stop.id
                currentStopSequence = 0
            }

        val tripPredictions = PredictionsStreamDataResponse(objects)
        val tripSchedules = TripSchedulesResponse.Schedules(listOf(schedule))

        var tripPredictionsConnectedCount = 0
        var tripPredictionsDisconnectedCount = 0
        var vehicleConnectedCount = 0
        var vehicleDisconnectedCount = 0

        val viewModel =
            StopDetailsViewModel.mocked(
                tripPredictionsRepo =
                    MockTripPredictionsRepository(
                        { tripPredictionsConnectedCount += 1 },
                        { tripPredictionsDisconnectedCount += 1 },
                        tripPredictions
                    ),
                tripRepo =
                    MockTripRepository(
                        tripSchedulesResponse = tripSchedules,
                        tripResponse = TripResponse(trip1)
                    ),
                vehicleRepo =
                    MockVehicleRepository(
                        { vehicleConnectedCount += 1 },
                        { vehicleDisconnectedCount += 1 },
                        ApiResult.Ok(VehicleStreamDataResponse(vehicle))
                    )
            )

        val tripFilter = TripDetailsFilter(trip1.id, vehicle.id, 0, false)
        composeTestRule.setContent {
            LaunchedEffect(Unit) { viewModel.handleTripFilterChange(tripFilter) }
        }

        composeTestRule.waitUntil { viewModel.tripData.value != null }
        assertEquals(tripFilter, viewModel.tripData.value?.tripFilter)
        assertEquals(trip1, viewModel.tripData.value?.trip)

        composeTestRule.waitUntil { viewModel.tripData.value?.tripSchedules != null }
        assertEquals(tripSchedules, viewModel.tripData.value?.tripSchedules)
        composeTestRule.waitUntil { viewModel.tripData.value?.tripPredictions != null }
        assertEquals(tripPredictions, viewModel.tripData.value?.tripPredictions)
        composeTestRule.waitUntil { viewModel.tripData.value?.vehicle != null }
        assertEquals(vehicle, viewModel.tripData.value?.vehicle)

        assertEquals(true, viewModel.tripData.value?.tripPredictionsLoaded)
        assertEquals(2, tripPredictionsDisconnectedCount)
        assertEquals(1, tripPredictionsConnectedCount)
        assertEquals(2, vehicleDisconnectedCount)
        assertEquals(1, vehicleConnectedCount)

        // Call handle change again - connection / disconnect counts should stay the same

        viewModel.handleTripFilterChange(tripFilter)

        assertEquals(2, tripPredictionsDisconnectedCount)
        assertEquals(1, tripPredictionsConnectedCount)
        assertEquals(2, vehicleDisconnectedCount)
        assertEquals(1, vehicleConnectedCount)

        assertEquals(tripFilter, viewModel.tripData.value?.tripFilter)
        assertEquals(trip1, viewModel.tripData.value?.trip)
        assertEquals(tripSchedules, viewModel.tripData.value?.tripSchedules)
        assertEquals(tripPredictions, viewModel.tripData.value?.tripPredictions)
        assertEquals(vehicle, viewModel.tripData.value?.vehicle)
    }

    @Test
    fun testSkipLoadingRedundantVehicleData() = runTest {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route = objects.route()

        val pattern =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip { headsign = "0" }
            }
        val trip0 = objects.trip(pattern) { headsign = "0" }
        val trip1 = objects.trip(pattern) { headsign = "0" }

        val schedule =
            objects.schedule {
                trip = trip0
                routeId = route.id
                stopId = stop.id
            }
        val vehicle =
            objects.vehicle {
                tripId = trip0.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
                stopId = stop.id
                currentStopSequence = 0
            }

        val tripPredictions = PredictionsStreamDataResponse(objects)
        val tripSchedules = TripSchedulesResponse.Schedules(listOf(schedule))

        var tripPredictionsConnectedCount = 0
        var tripPredictionsDisconnectedCount = 0
        var vehicleConnectedCount = 0
        var vehicleDisconnectedCount = 0

        val viewModel =
            StopDetailsViewModel.mocked(
                tripPredictionsRepo =
                    MockTripPredictionsRepository(
                        { tripPredictionsConnectedCount += 1 },
                        { tripPredictionsDisconnectedCount += 1 },
                        tripPredictions
                    ),
                tripRepo =
                    MockTripRepository(
                        tripSchedulesResponse = tripSchedules,
                        tripResponse = TripResponse(trip1)
                    ),
                vehicleRepo =
                    MockVehicleRepository(
                        { vehicleConnectedCount += 1 },
                        { vehicleDisconnectedCount += 1 },
                        ApiResult.Ok(VehicleStreamDataResponse(vehicle))
                    )
            )

        val tripFilter = TripDetailsFilter(trip0.id, vehicle.id, 0, false)
        composeTestRule.setContent {
            LaunchedEffect(Unit) { viewModel.handleTripFilterChange(tripFilter) }
        }

        composeTestRule.waitUntil {
            viewModel.tripData.value?.tripSchedules != null &&
                viewModel.tripData.value?.tripPredictions != null &&
                viewModel.tripData.value?.vehicle != null
        }
        assertEquals(2, tripPredictionsDisconnectedCount)
        assertEquals(1, tripPredictionsConnectedCount)
        assertEquals(2, vehicleDisconnectedCount)
        assertEquals(1, vehicleConnectedCount)

        // Call handle change again - only the tripId changed
        val newTripFilter = tripFilter.copy(tripId = trip1.id)

        viewModel.handleTripFilterChange(newTripFilter)

        composeTestRule.waitUntil {
            tripPredictionsDisconnectedCount == 4 && tripPredictionsConnectedCount == 2
        }

        assertEquals(4, tripPredictionsDisconnectedCount)
        assertEquals(2, tripPredictionsConnectedCount)
        assertEquals(2, vehicleDisconnectedCount)
        assertEquals(1, vehicleConnectedCount)

        assertEquals(newTripFilter, viewModel.tripData.value?.tripFilter)
        assertEquals(trip1, viewModel.tripData.value?.trip)
        assertEquals(tripSchedules, viewModel.tripData.value?.tripSchedules)
        assertEquals(tripPredictions, viewModel.tripData.value?.tripPredictions)
        assertEquals(vehicle, viewModel.tripData.value?.vehicle)
    }

    @Test
    fun testSkipLoadingRedundantTripData() = runTest {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route = objects.route()

        val pattern =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip { headsign = "0" }
            }
        val trip0 = objects.trip(pattern) { headsign = "0" }

        val schedule =
            objects.schedule {
                trip = trip0
                routeId = route.id
                stopId = stop.id
            }
        val vehicle0 =
            objects.vehicle {
                tripId = trip0.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
                stopId = stop.id
                currentStopSequence = 0
            }

        val vehicle1 =
            objects.vehicle {
                tripId = trip0.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
                stopId = stop.id
                currentStopSequence = 0
            }

        val tripPredictions = PredictionsStreamDataResponse(objects)
        val tripSchedules = TripSchedulesResponse.Schedules(listOf(schedule))

        var tripPredictionsConnectedCount = 0
        var tripPredictionsDisconnectedCount = 0
        var vehicleConnectedCount = 0
        var vehicleDisconnectedCount = 0

        val viewModel =
            StopDetailsViewModel.mocked(
                tripPredictionsRepo =
                    MockTripPredictionsRepository(
                        { tripPredictionsConnectedCount += 1 },
                        { tripPredictionsDisconnectedCount += 1 },
                        tripPredictions
                    ),
                tripRepo =
                    MockTripRepository(
                        tripSchedulesResponse = tripSchedules,
                        tripResponse = TripResponse(trip0)
                    ),
                vehicleRepo =
                    MockVehicleRepository(
                        { vehicleConnectedCount += 1 },
                        { vehicleDisconnectedCount += 1 },
                        ApiResult.Ok(VehicleStreamDataResponse(vehicle1))
                    )
            )

        val tripFilter = TripDetailsFilter(trip0.id, vehicle0.id, 0, false)
        composeTestRule.setContent {
            LaunchedEffect(Unit) { viewModel.handleTripFilterChange(tripFilter) }
        }

        composeTestRule.waitUntil {
            viewModel.tripData.value?.tripSchedules != null &&
                viewModel.tripData.value?.tripPredictions != null &&
                viewModel.tripData.value?.vehicle != null
        }
        assertEquals(2, tripPredictionsDisconnectedCount)
        assertEquals(1, tripPredictionsConnectedCount)
        assertEquals(2, vehicleDisconnectedCount)
        assertEquals(1, vehicleConnectedCount)

        // Call handle change again - only the vehicleId changed
        val newTripFilter = tripFilter.copy(vehicleId = vehicle1.id)

        viewModel.handleTripFilterChange(newTripFilter)

        composeTestRule.waitUntil { vehicleDisconnectedCount == 4 && vehicleConnectedCount == 2 }

        assertEquals(2, tripPredictionsDisconnectedCount)
        assertEquals(1, tripPredictionsConnectedCount)
        assertEquals(4, vehicleDisconnectedCount)
        assertEquals(2, vehicleConnectedCount)

        assertEquals(newTripFilter, viewModel.tripData.value?.tripFilter)
        assertEquals(trip0, viewModel.tripData.value?.trip)
        assertEquals(tripSchedules, viewModel.tripData.value?.tripSchedules)
        assertEquals(tripPredictions, viewModel.tripData.value?.tripPredictions)
        assertEquals(vehicle1, viewModel.tripData.value?.vehicle)
    }

    @Test
    fun testNullTripFilter() = runTest {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route = objects.route()

        val pattern =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip { headsign = "0" }
            }
        val trip1 = objects.trip(pattern) { headsign = "0" }
        val schedule =
            objects.schedule {
                trip = trip1
                routeId = route.id
                stopId = stop.id
            }
        val vehicle =
            objects.vehicle {
                tripId = trip1.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
                stopId = stop.id
                currentStopSequence = 0
            }

        val tripPredictions = PredictionsStreamDataResponse(objects)
        val tripSchedules = TripSchedulesResponse.Schedules(listOf(schedule))

        var tripPredictionsConnectedCount = 0
        var tripPredictionsDisconnectedCount = 0
        var vehicleConnectedCount = 0
        var vehicleDisconnectedCount = 0

        val viewModel =
            StopDetailsViewModel.mocked(
                tripPredictionsRepo =
                    MockTripPredictionsRepository(
                        { tripPredictionsConnectedCount += 1 },
                        { tripPredictionsDisconnectedCount += 1 },
                        tripPredictions
                    ),
                tripRepo =
                    MockTripRepository(
                        tripSchedulesResponse = tripSchedules,
                        tripResponse = TripResponse(trip1)
                    ),
                vehicleRepo =
                    MockVehicleRepository(
                        { vehicleConnectedCount += 1 },
                        { vehicleDisconnectedCount += 1 },
                        ApiResult.Ok(VehicleStreamDataResponse(vehicle))
                    )
            )

        val tripFilter = TripDetailsFilter(trip1.id, vehicle.id, 0, false)
        composeTestRule.setContent {
            LaunchedEffect(Unit) { viewModel.handleTripFilterChange(tripFilter) }
        }

        composeTestRule.waitUntil {
            tripPredictionsConnectedCount == 1 && vehicleConnectedCount == 1
        }
        assertEquals(2, tripPredictionsDisconnectedCount)
        assertEquals(2, vehicleDisconnectedCount)

        assertNotNull(viewModel.tripData.value)

        viewModel.handleTripFilterChange(null)

        composeTestRule.waitUntil {
            tripPredictionsDisconnectedCount == 3 && vehicleDisconnectedCount == 3
        }
        assertEquals(3, tripPredictionsDisconnectedCount)
        assertEquals(3, vehicleDisconnectedCount)

        assertNull(viewModel.tripData.value)
    }

    @Test
    fun testManagerHandlesStopChange() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val objects = ObjectCollectionBuilder()

        var connectCount = 0
        var disconnectCount = 0

        val predictionsRepo =
            MockPredictionsRepository(
                connectV2Outcome = ApiResult.Ok(PredictionsByStopJoinResponse(objects)),
                onConnectV2 = { connectCount += 1 },
                onDisconnect = { disconnectCount += 1 }
            )

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val errorBannerRepo = MockErrorBannerStateRepository()

        val viewModel =
            StopDetailsViewModel.mocked(
                errorBannerRepo,
                predictionsRepo,
                schedulesRepo,
                coroutineDispatcher = dispatcher
            )

        val stopFilters =
            mutableStateOf<StopDetailsPageFilters?>(StopDetailsPageFilters("stop1", null, null))

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.STARTED)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                var stopFilters by remember { stopFilters }
                stopDetailsManagedVM(
                    stopFilters,
                    viewModel = viewModel,
                    globalResponse = null,
                    alertData = null,
                    pinnedRoutes = setOf(),
                    updateStopFilter = { _, _ -> },
                    updateTripFilter = { _, _ -> },
                    setMapSelectedVehicle = {},
                    coroutineDispatcher = dispatcher
                )
            }
        }
        advanceUntilIdle()

        composeTestRule.waitUntil { connectCount == 1 }

        assertEquals(1, connectCount)
        assertEquals(1, disconnectCount)
        assertEquals("stop1", viewModel.stopData.value?.stopId)

        stopFilters.value = StopDetailsPageFilters("stop2", null, null)

        advanceUntilIdle()

        composeTestRule.waitUntil { connectCount == 2 }

        assertEquals(2, connectCount)
        assertEquals(2, disconnectCount)
        assertEquals("stop2", viewModel.stopData.value?.stopId)
    }

    @Test
    fun testManagerHandlesTripFilterChange() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route = objects.route()

        val pattern =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip { headsign = "0" }
            }
        val trip1 = objects.trip(pattern) { headsign = "0" }
        val schedule =
            objects.schedule {
                trip = trip1
                routeId = route.id
                stopId = stop.id
            }
        val vehicle =
            objects.vehicle {
                tripId = trip1.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
                stopId = stop.id
                currentStopSequence = 0
            }

        val tripPredictions = PredictionsStreamDataResponse(objects)
        val tripSchedules = TripSchedulesResponse.Schedules(listOf(schedule))

        val viewModel =
            StopDetailsViewModel.mocked(
                tripPredictionsRepo = MockTripPredictionsRepository({}, {}, tripPredictions),
                tripRepo =
                    MockTripRepository(
                        tripSchedulesResponse = tripSchedules,
                        tripResponse = TripResponse(trip1)
                    ),
                vehicleRepo =
                    MockVehicleRepository({}, {}, ApiResult.Ok(VehicleStreamDataResponse(vehicle))),
                coroutineDispatcher = dispatcher
            )

        val newTripFilter = TripDetailsFilter(trip1.id, vehicle.id, 0, false)

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.STARTED)

        val stopFilters =
            mutableStateOf<StopDetailsPageFilters?>(
                StopDetailsPageFilters(stop.id, StopDetailsFilter(route.id, 0), null)
            )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                var stopFilters by remember { stopFilters }
                stopDetailsManagedVM(
                    stopFilters,
                    viewModel = viewModel,
                    globalResponse = null,
                    alertData = null,
                    pinnedRoutes = setOf(),
                    updateStopFilter = { _, _ -> },
                    updateTripFilter = { _, _ -> },
                    setMapSelectedVehicle = {},
                    coroutineDispatcher = dispatcher
                )
            }
        }

        advanceUntilIdle()
        composeTestRule.waitUntil { viewModel.stopData.value != null }
        assertNull(viewModel.tripData.value)

        composeTestRule.runOnIdle {
            stopFilters.value = stopFilters.value?.copy(tripFilter = newTripFilter)
        }
        advanceUntilIdle()
        // This test reliably fails without also using `awaitIdle`.
        composeTestRule.awaitIdle()

        composeTestRule.waitUntil { viewModel.tripData.value != null }
        assertNotNull(viewModel.tripData.value)
    }

    @Test
    fun testManagerHandlesBackgrounding() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route = objects.route()

        val pattern =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip { headsign = "0" }
            }
        val trip0 = objects.trip(pattern) { headsign = "0" }

        val schedule =
            objects.schedule {
                trip = trip0
                routeId = route.id
                stopId = stop.id
            }
        val vehicle0 =
            objects.vehicle {
                tripId = trip0.id
                currentStatus = Vehicle.CurrentStatus.InTransitTo
                stopId = stop.id
                currentStopSequence = 0
            }

        var stopPredictionsConnectCount = 0
        var stopPredictionsDisconnectCount = 0

        var tripPredictionsConnectCount = 0
        var tripPredictionsDisconnectCount = 0

        var vehicleConnectCount = 0
        var vehicleDisconnectCount = 0

        val predictionsRepo =
            MockPredictionsRepository(
                connectV2Outcome = ApiResult.Ok(PredictionsByStopJoinResponse(objects)),
                onConnectV2 = { stopPredictionsConnectCount += 1 },
                onDisconnect = { stopPredictionsDisconnectCount += 1 }
            )

        val tripPredictionsRepo =
            MockTripPredictionsRepository(
                { tripPredictionsConnectCount += 1 },
                { tripPredictionsDisconnectCount += 1 },
                PredictionsStreamDataResponse(objects)
            )
        val vehicleRepo =
            MockVehicleRepository(
                { vehicleConnectCount += 1 },
                { vehicleDisconnectCount += 1 },
                ApiResult.Ok(VehicleStreamDataResponse(vehicle0))
            )

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val errorBannerRepo = MockErrorBannerStateRepository()

        val viewModel =
            StopDetailsViewModel.mocked(
                errorBannerRepo,
                predictionsRepo,
                schedulesRepo,
                tripPredictionsRepo = tripPredictionsRepo,
                vehicleRepo = vehicleRepo,
                coroutineDispatcher = dispatcher
            )

        val stopFilters =
            mutableStateOf(
                StopDetailsPageFilters(
                    "stop1",
                    StopDetailsFilter("route", 0),
                    TripDetailsFilter("tripId", "vehicleId", 0)
                )
            )

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                var stopFilters by remember { stopFilters }
                stopDetailsManagedVM(
                    stopFilters,
                    viewModel = viewModel,
                    globalResponse = null,
                    alertData = null,
                    pinnedRoutes = setOf(),
                    updateStopFilter = { _, _ -> },
                    updateTripFilter = { _, _ -> },
                    setMapSelectedVehicle = {},
                    coroutineDispatcher = dispatcher
                )
            }
        }

        advanceUntilIdle()

        composeTestRule.waitUntil {
            // In resumed state, so joined 1 time for stop, 1 time for resume.
            // Need to start with lifecycle resumed in order to test pause
            stopPredictionsConnectCount == 2

            // Trip channels rejoin is no-op because tripData hasn't been set yet
            && tripPredictionsConnectCount == 1 && vehicleConnectCount == 1
        }

        assertEquals(2, stopPredictionsConnectCount)
        assertEquals(1, stopPredictionsDisconnectCount)

        assertEquals(1, tripPredictionsConnectCount)
        assertEquals(2, tripPredictionsDisconnectCount)

        assertEquals(1, vehicleConnectCount)
        assertEquals(2, vehicleDisconnectCount)

        assertEquals("stop1", viewModel.stopData.value?.stopId)

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE) }
        advanceUntilIdle()

        composeTestRule.waitUntil {
            stopPredictionsDisconnectCount == 2 &&
                tripPredictionsDisconnectCount == 3 &&
                vehicleDisconnectCount == 3
        }

        assertEquals(2, stopPredictionsConnectCount)
        assertEquals(2, stopPredictionsDisconnectCount)
        assertEquals(3, tripPredictionsDisconnectCount)
        assertEquals(3, vehicleDisconnectCount)

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }

        advanceUntilIdle()
        composeTestRule.waitUntil {
            stopPredictionsConnectCount == 3 &&
                tripPredictionsConnectCount == 2 &&
                vehicleConnectCount == 2
        }

        assertEquals(3, stopPredictionsConnectCount)
        assertEquals(2, stopPredictionsDisconnectCount)
        assertEquals(2, tripPredictionsConnectCount)
        assertEquals(2, vehicleConnectCount)
    }

    @Test
    fun testManagerSetsDeparturesOnChange() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val objects = ObjectCollectionBuilder()
        objects.stop { id = "stop1" }

        val predictionsRepo =
            MockPredictionsRepository(
                connectV2Outcome = ApiResult.Ok(PredictionsByStopJoinResponse(objects))
            )

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val errorBannerRepo = MockErrorBannerStateRepository()

        val viewModel =
            StopDetailsViewModel.mocked(
                errorBannerRepo,
                predictionsRepo,
                schedulesRepo,
                coroutineDispatcher = dispatcher
            )

        val stopFilters = mutableStateOf(StopDetailsPageFilters("stop1", null, null))

        assertNull(viewModel.stopDepartures.value)

        composeTestRule.setContent {
            var stopFilters by remember { stopFilters }
            stopDetailsManagedVM(
                stopFilters,
                viewModel = viewModel,
                globalResponse = GlobalResponse(objects),
                alertData = AlertsStreamDataResponse(objects),
                pinnedRoutes = setOf(),
                updateStopFilter = { _, _ -> },
                updateTripFilter = { _, _ -> },
                setMapSelectedVehicle = {},
                coroutineDispatcher = dispatcher
            )
        }

        advanceUntilIdle()

        composeTestRule.waitUntil { viewModel.stopDepartures.value != null }

        assertNotNull(viewModel.stopDepartures.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testManagerDoesNotSetDeparturesWhenNothingLoaded() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val objects = ObjectCollectionBuilder()
        objects.stop { id = "stop1" }

        val emptyPredictionsRepo = MockPredictionsRepository()
        val emptySchedulesRepo = IdleScheduleRepository()

        val errorBannerRepo = MockErrorBannerStateRepository()

        val stopFilters = mutableStateOf(StopDetailsPageFilters("stop1", null, null))

        val viewModelNothingLoaded =
            StopDetailsViewModel.mocked(
                errorBannerRepo,
                emptyPredictionsRepo,
                emptySchedulesRepo,
                coroutineDispatcher = dispatcher
            )

        assertNull(viewModelNothingLoaded.stopDepartures.value)

        composeTestRule.setContent {
            var stopFilters by remember { stopFilters }
            stopDetailsManagedVM(
                stopFilters,
                viewModel = viewModelNothingLoaded,
                globalResponse = GlobalResponse(objects),
                alertData = AlertsStreamDataResponse(objects),
                pinnedRoutes = setOf(),
                updateStopFilter = { _, _ -> },
                updateTripFilter = { _, _ -> },
                setMapSelectedVehicle = {},
                coroutineDispatcher = dispatcher
            )
        }

        advanceUntilIdle()

        assertNull(viewModelNothingLoaded.stopDepartures.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testManagerDoesNotSetDeparturesWhenOnlySchedulesLoaded() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val objects = ObjectCollectionBuilder()
        objects.stop { id = "stop1" }

        val emptyPredictionsRepo = MockPredictionsRepository()
        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val errorBannerRepo = MockErrorBannerStateRepository()

        val stopFilters = mutableStateOf(StopDetailsPageFilters("stop1", null, null))

        val viewModelSchedulesLoaded =
            StopDetailsViewModel.mocked(
                errorBannerRepo,
                emptyPredictionsRepo,
                schedulesRepo,
                coroutineDispatcher = dispatcher
            )

        assertNull(viewModelSchedulesLoaded.stopDepartures.value)

        composeTestRule.setContent {
            var stopFilters by remember { stopFilters }
            stopDetailsManagedVM(
                stopFilters,
                viewModel = viewModelSchedulesLoaded,
                globalResponse = GlobalResponse(objects),
                alertData = AlertsStreamDataResponse(objects),
                pinnedRoutes = setOf(),
                updateStopFilter = { _, _ -> },
                updateTripFilter = { _, _ -> },
                setMapSelectedVehicle = {},
                coroutineDispatcher = dispatcher
            )
        }

        advanceUntilIdle()

        assertNull(viewModelSchedulesLoaded.stopDepartures.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testManagerDoesNotSetDeparturesWhenOnlyPredictionsLoaded() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val objects = ObjectCollectionBuilder()
        objects.stop { id = "stop1" }

        val predictionsRepo =
            MockPredictionsRepository(connectV2Response = PredictionsByStopJoinResponse(objects))
        val emptySchedulesRepo = IdleScheduleRepository()

        val errorBannerRepo = MockErrorBannerStateRepository()

        val stopFilters = mutableStateOf(StopDetailsPageFilters("stop1", null, null))

        val viewModelPredictionsLoaded =
            StopDetailsViewModel.mocked(
                errorBannerRepo,
                predictionsRepo,
                emptySchedulesRepo,
                coroutineDispatcher = dispatcher
            )

        assertNull(viewModelPredictionsLoaded.stopDepartures.value)

        composeTestRule.setContent {
            var stopFilters by remember { stopFilters }
            stopDetailsManagedVM(
                stopFilters,
                viewModel = viewModelPredictionsLoaded,
                globalResponse = GlobalResponse(objects),
                alertData = AlertsStreamDataResponse(objects),
                pinnedRoutes = setOf(),
                updateStopFilter = { _, _ -> },
                updateTripFilter = { _, _ -> },
                setMapSelectedVehicle = {},
                coroutineDispatcher = dispatcher
            )
        }

        advanceUntilIdle()

        assertNull(viewModelPredictionsLoaded.stopDepartures.value)
    }

    @Test
    fun testManagerChecksPredictionsStale() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val objects = ObjectCollectionBuilder()
        objects.prediction()

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val predictionsOnJoin = PredictionsByStopJoinResponse(objects)
        val predictionsRepo = MockPredictionsRepository({}, {}, {}, null, predictionsOnJoin)

        predictionsRepo.lastUpdated = Clock.System.now()

        val stopFilters = mutableStateOf(StopDetailsPageFilters("stop1", null, null))

        var checkPredictionsStaleCount = 0
        val errorBannerRepo =
            MockErrorBannerStateRepository(
                onCheckPredictionsStale = { checkPredictionsStaleCount += 1 }
            )

        val viewModel =
            StopDetailsViewModel.mocked(
                errorBannerRepo,
                predictionsRepo,
                schedulesRepo,
                coroutineDispatcher = dispatcher
            )

        composeTestRule.setContent {
            var stopFilters by remember { stopFilters }
            stopDetailsManagedVM(
                stopFilters,
                viewModel = viewModel,
                globalResponse = GlobalResponse(objects),
                alertData = AlertsStreamDataResponse(objects),
                pinnedRoutes = setOf(),
                checkPredictionsStaleInterval = 1.seconds,
                updateStopFilter = { _, _ -> },
                updateTripFilter = { _, _ -> },
                setMapSelectedVehicle = {},
                coroutineDispatcher = dispatcher
            )
        }

        advanceUntilIdle()

        composeTestRule.waitUntil(timeoutMillis = 3000) { checkPredictionsStaleCount >= 2 }
    }

    @Test
    fun testManagersAppliesStopFilterAutomaticallyOnDepartureChange() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val objects = ObjectCollectionBuilder()

        val now = Clock.System.now()

        val route = objects.route {}
        val stop = objects.stop {}

        val tripId = "trip"
        val routePattern = objects.routePattern(route) { representativeTripId = tripId }
        val trip1 =
            objects.trip(routePattern) {
                id = tripId
                directionId = 0
                stopIds = listOf(stop.id)
                routePatternId = routePattern.id
            }
        objects.schedule {
            routeId = route.id
            stopId = stop.id
            stopSequence = 0
            departureTime = now.plus(10.minutes)
            trip = trip1
        }

        objects.prediction()

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val predictionsOnJoin = PredictionsByStopJoinResponse(objects)
        val predictionsRepo = MockPredictionsRepository({}, {}, {}, null, predictionsOnJoin)
        val errorBannerRepo = MockErrorBannerStateRepository()

        val stopFilters = mutableStateOf(StopDetailsPageFilters(stop.id, null, null))

        val viewModel =
            StopDetailsViewModel.mocked(
                errorBannerRepo,
                predictionsRepo,
                schedulesRepo,
                coroutineDispatcher = dispatcher
            )

        var newStopFilter: StopDetailsFilter? = null

        composeTestRule.setContent {
            var stopFilters by remember { stopFilters }
            stopDetailsManagedVM(
                stopFilters,
                viewModel = viewModel,
                globalResponse = GlobalResponse(objects),
                alertData = AlertsStreamDataResponse(objects),
                pinnedRoutes = setOf(),
                checkPredictionsStaleInterval = 1.seconds,
                updateStopFilter = { _, filter -> newStopFilter = filter },
                updateTripFilter = { _, _ -> },
                setMapSelectedVehicle = {},
                coroutineDispatcher = dispatcher
            )

            LaunchedEffect(null) {
                viewModel.setDepartures(
                    StopDetailsDepartures.fromData(
                        stop,
                        GlobalResponse(objects),
                        ScheduleResponse(objects),
                        PredictionsStreamDataResponse(objects),
                        AlertsStreamDataResponse(objects),
                        setOf(),
                        now
                    )
                )
            }
        }

        advanceUntilIdle()

        composeTestRule.waitUntil {
            newStopFilter == StopDetailsFilter(route.id, routePattern.directionId, true)
        }

        assertEquals(StopDetailsFilter(route.id, routePattern.directionId, true), newStopFilter)
    }

    @Test
    fun testManagerAppliesTripFilterAutomaticallyOnDepartureChange() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}

        val now = Clock.System.now()

        val route = objects.route {}

        val tripId = "trip"
        val routePattern = objects.routePattern(route) { representativeTripId = tripId }
        val trip1 =
            objects.trip(routePattern) {
                id = tripId
                directionId = 0
                stopIds = listOf(stop.id)
                routePatternId = routePattern.id
            }
        val schedule =
            objects.schedule {
                routeId = route.id
                stopId = stop.id
                stopSequence = 0
                departureTime = now.plus(10.minutes)
                trip = trip1
            }

        objects.prediction(schedule) { departureTime = now.plus(10.minutes) }

        val viewModel = StopDetailsViewModel.mocked(coroutineDispatcher = dispatcher)

        val stopFilters =
            mutableStateOf(StopDetailsPageFilters(stop.id, StopDetailsFilter(route.id, 0), null))

        var newTripFilter: TripDetailsFilter? = null

        composeTestRule.setContent {
            var stopFilters by remember { stopFilters }
            stopDetailsManagedVM(
                stopFilters,
                viewModel = viewModel,
                globalResponse = GlobalResponse(objects),
                alertData = AlertsStreamDataResponse(objects),
                pinnedRoutes = setOf(),
                checkPredictionsStaleInterval = 1.seconds,
                updateStopFilter = { _, _ -> },
                updateTripFilter = { _, tripFilter -> newTripFilter = tripFilter },
                setMapSelectedVehicle = {},
                coroutineDispatcher = dispatcher
            )

            LaunchedEffect(null) {
                viewModel.setDepartures(
                    StopDetailsDepartures.fromData(
                        stop,
                        GlobalResponse(objects),
                        ScheduleResponse(objects),
                        PredictionsStreamDataResponse(objects),
                        AlertsStreamDataResponse(objects),
                        setOf(),
                        now
                    )
                )
            }
        }
        advanceUntilIdle()

        val expectedTripFilter = TripDetailsFilter(trip1.id, null, 0, false)

        composeTestRule.waitUntil(2000) { newTripFilter == expectedTripFilter }
        kotlin.test.assertEquals(expectedTripFilter, newTripFilter)
    }

    @Test
    fun testManagerAppliesTripFilterAutomaticallyOnFilterChange() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}

        val now = Clock.System.now()

        val route = objects.route {}

        val tripId = "trip"
        val routePattern = objects.routePattern(route) { representativeTripId = tripId }
        val trip1 =
            objects.trip(routePattern) {
                id = tripId
                directionId = 0
                stopIds = listOf(stop.id)
                routePatternId = routePattern.id
            }
        val schedule =
            objects.schedule {
                routeId = route.id
                stopId = stop.id
                stopSequence = 0
                departureTime = now.plus(10.minutes)
                trip = trip1
            }

        objects.prediction(schedule) { departureTime = now.plus(10.minutes) }

        val viewModel = StopDetailsViewModel.mocked(coroutineDispatcher = dispatcher)

        // There are no trips in direction 1
        val stopFilters =
            mutableStateOf(StopDetailsPageFilters(stop.id, StopDetailsFilter(route.id, 1), null))

        var newTripFilter: TripDetailsFilter? = null

        composeTestRule.setContent {
            var filters by remember { stopFilters }
            stopDetailsManagedVM(
                filters,
                viewModel = viewModel,
                globalResponse = GlobalResponse(objects),
                alertData = AlertsStreamDataResponse(objects),
                pinnedRoutes = setOf(),
                checkPredictionsStaleInterval = 1.seconds,
                updateStopFilter = { _, _ -> },
                updateTripFilter = { _, tripFilter -> newTripFilter = tripFilter },
                setMapSelectedVehicle = {},
                coroutineDispatcher = dispatcher
            )

            LaunchedEffect(null) {
                viewModel.setDepartures(
                    StopDetailsDepartures.fromData(
                        stop,
                        GlobalResponse(objects),
                        ScheduleResponse(objects),
                        PredictionsStreamDataResponse(objects),
                        AlertsStreamDataResponse(objects),
                        setOf(),
                        now
                    )
                )

                stopFilters.value =
                    StopDetailsPageFilters(stop.id, StopDetailsFilter(route.id, 0), null)
            }
        }

        val expectedTripFilter = TripDetailsFilter(trip1.id, null, 0, false)

        advanceUntilIdle()

        composeTestRule.waitUntil(2_000) { newTripFilter == expectedTripFilter }
        kotlin.test.assertEquals(expectedTripFilter, newTripFilter)
    }

    @Test
    fun testSetsMapSelectedVehicle() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val routePattern = objects.routePattern(route)
        val stop = objects.stop()
        val trip = objects.trip(routePattern)
        val vehicle = objects.vehicle { currentStatus = Vehicle.CurrentStatus.InTransitTo }
        val stopSequence = 10
        val now = Clock.System.now()

        val globalResponse = GlobalResponse(objects)
        val alertData = AlertsStreamDataResponse(objects)

        val tripFilter = TripDetailsFilter(trip.id, vehicle.id, stopSequence)

        val mapSelectedVehicleValues = mutableListOf<Vehicle?>()

        val viewModel =
            StopDetailsViewModel.mocked(
                vehicleRepo =
                    MockVehicleRepository(
                        outcome = ApiResult.Ok(VehicleStreamDataResponse(vehicle))
                    )
            )

        composeTestRule.setContent {
            stopDetailsManagedVM(
                filters =
                    StopDetailsPageFilters(
                        stop.id,
                        StopDetailsFilter(route.id, routePattern.directionId),
                        tripFilter
                    ),
                globalResponse,
                alertData,
                pinnedRoutes = emptySet(),
                updateStopFilter = { _, _ -> },
                updateTripFilter = { _, _ -> },
                setMapSelectedVehicle = mapSelectedVehicleValues::add,
                now,
                viewModel = viewModel
            )
        }

        composeTestRule.waitUntil { mapSelectedVehicleValues.size == 2 }
        assertEquals(listOf(null, vehicle), mapSelectedVehicleValues)
    }
}
