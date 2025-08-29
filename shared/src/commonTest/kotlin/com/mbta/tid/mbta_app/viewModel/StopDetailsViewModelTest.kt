package com.mbta.tid.mbta_app.viewModel

import app.cash.turbine.test
import com.mbta.tid.mbta_app.dependencyInjection.MockRepositories
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.IdleScheduleRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockScheduleRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest

@OptIn(ExperimentalCoroutinesApi::class)
class StopDetailsViewModelTest : KoinTest {
    val objects = TestData.clone()

    private fun setUpKoin(
        objects: ObjectCollectionBuilder = this.objects,
        coroutineDispatcher: CoroutineDispatcher,
        testModule: Module = module {},
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
                testModule,
            )
        }
    }

    @AfterTest
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun testLoadsSchedules() = runTest {
        var schedulesLoaded = false
        val scheduleRepo =
            MockScheduleRepository(ScheduleResponse(objects)) { _ -> schedulesLoaded = true }

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher) { schedules = scheduleRepo }

        val viewModel: StopDetailsViewModel = get()
        val filters = StopDetailsPageFilters("place-gover", null, null)
        viewModel.setFilters(filters)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(EasternTimeInstant.now())

        testViewModelFlow(viewModel).test {
            awaitItemSatisfying { it.routeData is StopDetailsViewModel.RouteData.Unfiltered }
            assertTrue(schedulesLoaded)
        }
    }

    @Test
    fun testLoadsPredictions() = runTest {
        var predictionsLoaded = false

        val predictionRepo =
            MockPredictionsRepository(
                onConnectV2 = { _ -> predictionsLoaded = true },
                connectV2Response = PredictionsByStopJoinResponse(objects),
            )

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher) { predictions = predictionRepo }

        val viewModel: StopDetailsViewModel = get()
        val filters = StopDetailsPageFilters("place-gover", null, null)
        viewModel.setFilters(filters)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(EasternTimeInstant.now())

        testViewModelFlow(viewModel).test {
            awaitItemSatisfying { it.routeData is StopDetailsViewModel.RouteData.Unfiltered }
            assertTrue(predictionsLoaded)
        }
    }

    @Test
    fun testRejoinPredictionsWhenStopChanges() = runTest {
        var predictionLoadCount = 0

        val predictionRepo =
            MockPredictionsRepository(
                onConnectV2 = { _ -> predictionLoadCount++ },
                connectV2Response = PredictionsByStopJoinResponse(objects),
            )

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher) { predictions = predictionRepo }

        val viewModel: StopDetailsViewModel = get()
        val initialFilters = StopDetailsPageFilters("place-gover", null, null)
        val updatedFilters = StopDetailsPageFilters("place-rugg", null, null)
        viewModel.setFilters(initialFilters)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(EasternTimeInstant.now())

        testViewModelFlow(viewModel).test {
            awaitItemSatisfying {
                it.routeData is StopDetailsViewModel.RouteData.Unfiltered &&
                    it.routeData.filters == initialFilters
            }
            assertEquals(1, predictionLoadCount)
            viewModel.setFilters(updatedFilters)
            awaitItemSatisfying {
                it.routeData is StopDetailsViewModel.RouteData.Unfiltered &&
                    it.routeData.filters == updatedFilters
            }
            assertEquals(2, predictionLoadCount)
        }
    }

    @Test
    fun testLeavesPredictionsWhenInactive() = runTest {
        var predictionLoadCount = 0
        var predictionDisconnectCount = 0

        val predictionRepo =
            MockPredictionsRepository(
                onConnectV2 = { _ -> predictionLoadCount++ },
                onDisconnect = { predictionDisconnectCount++ },
                connectV2Response = PredictionsByStopJoinResponse(objects),
            )

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher) { predictions = predictionRepo }

        val viewModel: StopDetailsViewModel = get()
        viewModel.setActive(active = true, wasSentToBackground = false)
        viewModel.setFilters(StopDetailsPageFilters("place-gover", null, null))
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(EasternTimeInstant.now())

        testViewModelFlow(viewModel).test {
            awaitItemSatisfying { it.routeData is StopDetailsViewModel.RouteData.Unfiltered }
            assertEquals(1, predictionLoadCount)
            assertEquals(1, predictionDisconnectCount)
            viewModel.setActive(active = false, wasSentToBackground = false)
            advanceUntilIdle()
            assertEquals(1, predictionLoadCount)
            assertEquals(3, predictionDisconnectCount)
        }
    }

    @Test
    fun testRouteCardDataPublishing() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher)

        val routeCardVM: RouteCardDataViewModel = get()
        val viewModel: StopDetailsViewModel = get()
        val filters =
            StopDetailsPageFilters("place-gover", StopDetailsFilter("line-Green", 0, false), null)
        viewModel.setFilters(filters)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(EasternTimeInstant.now())

        launch {
            testViewModelFlow(viewModel).test {
                awaitItemSatisfying { it.routeData is StopDetailsViewModel.RouteData.Filtered }
            }
        }

        launch { testViewModelFlow(routeCardVM).test { awaitItemSatisfying { it.data != null } } }

        testScheduler.advanceUntilIdle()
    }

    @Test
    fun testRouteDataIsNullBeforePredictionsLoad() = runTest {
        val predictionRepo = MockPredictionsRepository()

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher) { predictions = predictionRepo }

        val viewModel: StopDetailsViewModel = get()
        val filters = StopDetailsPageFilters("place-gover", null, null)
        viewModel.setFilters(filters)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(EasternTimeInstant.now())

        testViewModelFlow(viewModel).test { assertNull(awaitItem().routeData) }
    }

    @Test
    fun testRouteDataIsNullBeforeSchedulesLoad() = runTest {
        val scheduleRepo = IdleScheduleRepository()

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher) { schedules = scheduleRepo }

        val viewModel: StopDetailsViewModel = get()
        val filters = StopDetailsPageFilters("place-gover", null, null)
        viewModel.setFilters(filters)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(EasternTimeInstant.now())

        testViewModelFlow(viewModel).test { assertNull(awaitItem().routeData) }
    }

    @Test
    fun testChecksPredictionsStale() = runTest {
        var staleCheckCount = 0
        val errorBannerStateRepository =
            MockErrorBannerStateRepository(onCheckPredictionsStale = { staleCheckCount++ })

        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher) {
            predictions =
                MockPredictionsRepository(
                    connectV2Response = PredictionsByStopJoinResponse(objects)
                )
            errorBanner = errorBannerStateRepository
        }

        val viewModel: StopDetailsViewModel = get()
        val filters = StopDetailsPageFilters("place-gover", null, null)
        viewModel.setFilters(filters)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(EasternTimeInstant.now())

        testViewModelFlow(viewModel).test {
            awaitItem()
            advanceTimeBy(6.seconds)
            assertTrue(staleCheckCount > 0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testAutoStopFilter() = runTest {
        val objects = TestData.clone()
        val trip = objects.getTrip("68596178")
        objects.prediction { this.trip = trip }
        objects.schedule { this.trip = trip }

        val scheduleRepo = MockScheduleRepository(ScheduleResponse(objects))
        val predictionRepo =
            MockPredictionsRepository(connectV2Response = PredictionsByStopJoinResponse(objects))
        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher) {
            schedules = scheduleRepo
            predictions = predictionRepo
        }

        val viewModel: StopDetailsViewModel = get()
        val filters = StopDetailsPageFilters("2595", null, null)

        viewModel.setFilters(filters)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(EasternTimeInstant.now())
        viewModel.setActive(active = true, wasSentToBackground = false)

        launch {
            testViewModelFlow(viewModel).test { awaitItemSatisfying { it.routeData != null } }
        }

        launch {
            viewModel.filterUpdates.test {
                awaitItem()
                awaitItemSatisfying { it?.stopFilter == StopDetailsFilter("87", 1, true) }
            }
        }

        testScheduler.advanceUntilIdle()
    }

    @Test
    fun testAutoTripFilter() = runTest {
        val now = EasternTimeInstant.now()
        val objects = TestData.clone()

        val stop = objects.getStop("2595")
        val trip = objects.getTrip("68596178")
        objects.prediction {
            departureTime = now.plus(5.minutes)
            stopId = stop.id
            this.trip = trip
        }
        objects.schedule {
            departureTime = now.plus(4.minutes)
            stopId = stop.id
            this.trip = trip
        }

        val scheduleRepo = MockScheduleRepository(ScheduleResponse(objects))
        val predictionRepo =
            MockPredictionsRepository(connectV2Response = PredictionsByStopJoinResponse(objects))
        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher) {
            schedules = scheduleRepo
            predictions = predictionRepo
        }

        val viewModel: StopDetailsViewModel = get()
        val filters = StopDetailsPageFilters(stop.id, null, null)

        viewModel.setActive(active = true, wasSentToBackground = false)
        viewModel.setFilters(filters)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(now)

        launch {
            testViewModelFlow(viewModel).test { awaitItemSatisfying { it.routeData != null } }
        }

        launch {
            viewModel.filterUpdates.test {
                awaitItem()
                awaitItemSatisfying { it?.tripFilter == TripDetailsFilter(trip.id, null, 0, false) }
            }
        }

        testScheduler.advanceUntilIdle()
    }

    @Test
    fun testAutoTripFilterOnDirectionChange() = runTest {
        val now = EasternTimeInstant.now()
        val objects = TestData.clone()

        val stop0 = objects.getStop("70010")
        val trip0 = objects.getTrip("canonical-Orange-C1-0")

        val stop1 = objects.getStop("70011")
        val trip1 = objects.getTrip("canonical-Orange-C1-1")
        objects.prediction {
            departureTime = now.plus(5.minutes)
            stopId = stop0.id
            this.trip = trip0
        }
        objects.schedule {
            departureTime = now.plus(4.minutes)
            stopId = trip0.id
            this.trip = trip0
        }
        objects.prediction {
            departureTime = now.plus(5.minutes)
            stopId = stop1.id
            this.trip = trip1
        }
        objects.schedule {
            departureTime = now.plus(4.minutes)
            stopId = trip1.id
            this.trip = trip1
        }

        val scheduleRepo = MockScheduleRepository(ScheduleResponse(objects))
        val predictionRepo =
            MockPredictionsRepository(connectV2Response = PredictionsByStopJoinResponse(objects))
        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(objects, dispatcher) {
            schedules = scheduleRepo
            predictions = predictionRepo
        }

        val viewModel: StopDetailsViewModel = get()
        val filters =
            StopDetailsPageFilters("place-rugg", StopDetailsFilter("Orange", 0, false), null)

        viewModel.setFilters(filters)
        viewModel.setAlerts(AlertsStreamDataResponse(emptyMap()))
        viewModel.setNow(now)
        viewModel.setActive(active = true, wasSentToBackground = false)

        launch {
            viewModel.filterUpdates.test {
                awaitItem()
                awaitItemSatisfying { it?.tripFilter?.tripId == trip0.id }
                awaitItem()
                awaitItemSatisfying { it?.tripFilter?.tripId == trip1.id }
            }
        }

        launch {
            testViewModelFlow(viewModel).test {
                awaitItemSatisfying { it.routeData?.filters?.stopFilter?.directionId == 0 }
                viewModel.setFilters(
                    StopDetailsPageFilters(
                        "place-rugg",
                        StopDetailsFilter("Orange", 1, false),
                        null,
                    )
                )
                awaitItemSatisfying { it.routeData?.filters?.stopFilter?.directionId == 1 }
            }
        }

        testScheduler.advanceUntilIdle()
    }
}
