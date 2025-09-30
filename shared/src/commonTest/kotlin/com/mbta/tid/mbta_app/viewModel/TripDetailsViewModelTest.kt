package com.mbta.tid.mbta_app.viewModel

import app.cash.turbine.test
import com.mbta.tid.mbta_app.dependencyInjection.MockRepositories
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.TripResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import com.mbta.tid.mbta_app.model.response.VehicleStreamDataResponse
import com.mbta.tid.mbta_app.repositories.MockPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.MockTripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockTripRepository
import com.mbta.tid.mbta_app.repositories.MockVehicleRepository
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest

@OptIn(ExperimentalCoroutinesApi::class)
class TripDetailsViewModelTest : KoinTest {
    private fun setUpKoin(
        objects: ObjectCollectionBuilder,
        coroutineDispatcher: CoroutineDispatcher,
        repositoriesBlock: MockRepositories.() -> Unit = {},
    ) {
        startKoin {
            modules(
                repositoriesModule(
                    MockRepositories().apply {
                        useObjects(objects)
                        pinnedRoutes = MockPinnedRoutesRepository()
                        repositoriesBlock()
                    }
                ),
                viewModelModule(),
                module {
                    single<CoroutineDispatcher>(named("coroutineDispatcherIO")) {
                        coroutineDispatcher
                    }
                    single<Clock> { Clock.System }
                },
            )
        }
    }

    @AfterTest
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun testLoadsStaticData() = runTest {
        val objects = TestData.clone()
        val trip = objects.trip {}

        var tripLoaded = false
        var schedulesLoaded = false

        val tripRepo =
            MockTripRepository(
                tripSchedulesResponse =
                    TripSchedulesResponse.Schedules(listOf(objects.schedule { this.trip = trip })),
                tripResponse = TripResponse(trip),
                onGetTrip = { tripLoaded = true },
                onGetTripSchedules = { schedulesLoaded = true },
            )

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher) { this.trip = tripRepo }

        val viewModel: TripDetailsViewModel = get()
        val filters = TripDetailsPageFilter(trip.id, null, "", 0, "", 0)
        viewModel.setFilters(filters)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))

        testViewModelFlow(viewModel).test {
            awaitItemSatisfying { it.tripData?.tripFilter == filters && it.tripData.trip == trip }
            assertTrue(tripLoaded)
            assertTrue(schedulesLoaded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testLoadsVehicle() = runTest {
        val objects = TestData.clone()
        val trip = objects.trip {}
        val vehicle =
            objects.vehicle {
                this.tripId = trip.id
                currentStatus = Vehicle.CurrentStatus.StoppedAt
            }

        var vehicleLoaded = false

        val tripRepo =
            MockTripRepository(
                tripSchedulesResponse =
                    TripSchedulesResponse.Schedules(listOf(objects.schedule { this.trip = trip })),
                tripResponse = TripResponse(trip),
            )
        val vehicleRepo =
            MockVehicleRepository(
                { vehicleLoaded = true },
                outcome = ApiResult.Ok(VehicleStreamDataResponse(vehicle)),
            )

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher) {
            this.trip = tripRepo
            this.vehicle = vehicleRepo
        }

        val viewModel: TripDetailsViewModel = get()
        val filters = TripDetailsPageFilter(trip.id, vehicle.id, "", 0, "", 0)
        viewModel.setFilters(filters)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))

        testViewModelFlow(viewModel).test {
            awaitItemSatisfying {
                it.tripData?.tripFilter == filters && it.tripData.vehicle == vehicle
            }
            assertTrue(vehicleLoaded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testLoadsPredictions() = runTest {
        val objects = TestData.clone()
        val trip = objects.trip {}

        var predictionsLoaded = false

        val tripRepo =
            MockTripRepository(
                tripSchedulesResponse =
                    TripSchedulesResponse.Schedules(listOf(objects.schedule { this.trip = trip })),
                tripResponse = TripResponse(trip),
            )
        val tripPredictionRepo =
            MockTripPredictionsRepository(
                onConnect = { predictionsLoaded = true },
                response = PredictionsStreamDataResponse(objects),
            )

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher) {
            this.trip = tripRepo
            tripPredictions = tripPredictionRepo
        }

        val viewModel: TripDetailsViewModel = get()
        val filters = TripDetailsPageFilter(trip.id, null, "", 0, "", 0)
        viewModel.setFilters(filters)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))

        testViewModelFlow(viewModel).test {
            awaitItemSatisfying {
                it.tripData?.tripFilter == filters && it.tripData.tripPredictionsLoaded
            }
            assertTrue(predictionsLoaded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testLeavesStopPredictionsWhenInactive() = runTest {
        val objects = TestData.clone()
        val trip = objects.trip {}

        var predictionLoadCount = 0
        var predictionDisconnectCount = 0

        val tripRepo =
            MockTripRepository(
                tripSchedulesResponse =
                    TripSchedulesResponse.Schedules(listOf(objects.schedule { this.trip = trip })),
                tripResponse = TripResponse(trip),
            )
        val tripPredictionRepo =
            MockTripPredictionsRepository(
                { predictionLoadCount++ },
                { predictionDisconnectCount++ },
                PredictionsStreamDataResponse(objects),
            )

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher) {
            this.trip = tripRepo
            tripPredictions = tripPredictionRepo
        }

        val viewModel: TripDetailsViewModel = get()
        val filters = TripDetailsPageFilter(trip.id, null, "", 0, "", 0)
        viewModel.setFilters(filters)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))

        testViewModelFlow(viewModel).test {
            awaitItemSatisfying { it.tripData?.trip?.id == trip.id }
            assertEquals(1, predictionLoadCount)
            assertEquals(1, predictionDisconnectCount)
            viewModel.setActive(active = false, wasSentToBackground = false)
            advanceUntilIdle()
            assertEquals(1, predictionLoadCount)
            assertEquals(3, predictionDisconnectCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testEmitsSelectedVehicle() = runTest {
        val objects = TestData.clone()
        val trip = objects.trip {}
        val vehicle =
            objects.vehicle {
                this.tripId = trip.id
                currentStatus = Vehicle.CurrentStatus.StoppedAt
            }

        val tripRepo =
            MockTripRepository(
                tripSchedulesResponse =
                    TripSchedulesResponse.Schedules(listOf(objects.schedule { this.trip = trip })),
                tripResponse = TripResponse(trip),
            )
        val vehicleRepo =
            MockVehicleRepository(outcome = ApiResult.Ok(VehicleStreamDataResponse(vehicle)))

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher) {
            this.trip = tripRepo
            this.vehicle = vehicleRepo
        }

        val viewModel: TripDetailsViewModel = get()
        val filters = TripDetailsPageFilter(trip.id, vehicle.id, "", 0, "", 0)
        viewModel.setFilters(filters)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setActive(active = true, wasSentToBackground = false)

        launch {
            testViewModelFlow(viewModel).test {
                awaitItemSatisfying {
                    it.tripData?.tripFilter == filters &&
                        it.tripData.vehicle == vehicle &&
                        it.stopList != null
                }
            }
        }

        launch {
            viewModel.selectedVehicleUpdates.test {
                awaitItem()
                awaitItemSatisfying { it == vehicle }
            }
        }

        testScheduler.advanceUntilIdle()
    }

    @Test
    fun testTripDataIsNullWithUnloadedVehicle() = runTest {
        val objects = TestData.clone()
        val trip = objects.trip {}

        val tripRepo =
            MockTripRepository(
                tripSchedulesResponse =
                    TripSchedulesResponse.Schedules(listOf(objects.schedule { this.trip = trip })),
                tripResponse = TripResponse(trip),
            )

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher) { this.trip = tripRepo }

        val viewModel: TripDetailsViewModel = get()
        val filters = TripDetailsPageFilter(trip.id, "vehicle id", "", 0, "", 0)
        viewModel.setFilters(filters)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setActive(active = true, wasSentToBackground = false)

        testViewModelFlow(viewModel).test { assertNull(awaitItem().tripData) }
    }
}
