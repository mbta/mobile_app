package com.mbta.tid.mbta_app.viewModel

import app.cash.turbine.test
import com.mbta.tid.mbta_app.dependencyInjection.MockRepositories
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.IMapLayerManager
import com.mbta.tid.mbta_app.utils.TestData
import com.mbta.tid.mbta_app.utils.ViewportManager
import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.matching
import dev.mokkery.mock
import dev.mokkery.resetCalls
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlin.test.AfterTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
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
internal class MapViewModelTests : KoinTest {

    class MockViewportManager(
        private val saveNearbyTransitViewportCalled: () -> Unit = {},
        private val restoreNearbyTransitViewportCalled: () -> Unit = {},
        private val stopCenterCalled: (stop: Stop) -> Unit = {},
        private val vehicleOverviewCalled: (vehicle: Vehicle, stop: Stop?, density: Float) -> Unit =
            { _, _, _ ->
            },
        private val followCalled: (transitionAnimationDuration: Long?) -> Unit = {},
        private val isDefaultCalled: () -> Boolean = { true },
    ) : ViewportManager {
        override suspend fun saveNearbyTransitViewport() = saveNearbyTransitViewportCalled()

        override suspend fun restoreNearbyTransitViewport() = restoreNearbyTransitViewportCalled()

        override suspend fun stopCenter(stop: Stop) = stopCenterCalled(stop)

        override suspend fun vehicleOverview(vehicle: Vehicle, stop: Stop?, density: Float) =
            vehicleOverviewCalled(vehicle, stop, density)

        override suspend fun follow(transitionAnimationDuration: Long?) =
            followCalled(transitionAnimationDuration)

        override suspend fun isDefault(): Boolean = isDefaultCalled()
    }

    fun setUpKoin(
        coroutineDispatcher: CoroutineDispatcher,
        configureRepositories: MockRepositories.() -> Unit = {},
    ) {
        startKoin {
            modules(
                module {
                    single<CoroutineDispatcher>(named("coroutineDispatcherDefault")) {
                        coroutineDispatcher
                    }
                },
                module {
                    single<CoroutineDispatcher>(named("coroutineDispatcherIO")) {
                        coroutineDispatcher
                    }
                },
                repositoriesModule(
                    MockRepositories().apply {
                        useObjects(TestData.clone())
                        configureRepositories()
                    }
                ),
                viewModelModule(),
                module { single<Clock> { Clock.System } },
            )
        }
    }

    @AfterTest
    fun teardownKoin() {
        stopKoin()
    }

    @Test
    fun handleNavChange() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(dispatcher)
        var timesSaveViewportCalled = 0
        var timesRestoreViewportCalled = 0
        val viewportProvider =
            MockViewportManager(
                saveNearbyTransitViewportCalled = { timesSaveViewportCalled += 1 },
                restoreNearbyTransitViewportCalled = { timesRestoreViewportCalled += 1 },
            )
        val viewModel: MapViewModel = get()
        testViewModelFlow(viewModel).test {
            viewModel.setViewportManager(viewportProvider)
            viewModel.navChanged(SheetRoutes.NearbyTransit)
            assertEquals(MapViewModel.State.Overview, awaitItem())
            val stop = TestData.stops["70113"]!!
            val stopDetails = SheetRoutes.StopDetails(stop.id, null, null)
            viewModel.navChanged(stopDetails)
            assertEquals(MapViewModel.State.StopSelected(stop, null), awaitItem())
            viewModel.navChanged(SheetRoutes.NearbyTransit)
            assertEquals(MapViewModel.State.Overview, awaitItem())
            delay(10)
            assertEquals(1, timesRestoreViewportCalled)
            assertEquals(1, timesSaveViewportCalled)
        }
    }

    @Test
    fun recenter() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(dispatcher)
        var timesFollowCalled = 0
        var timesVehicleOverViewCalled = 0
        val viewportProvider =
            MockViewportManager(
                vehicleOverviewCalled = { _, _, _ -> timesVehicleOverViewCalled += 1 },
                followCalled = { timesFollowCalled += 1 },
            )
        val viewModel: MapViewModel = get()

        val vehicle = TestData.vehicle { currentStatus = Vehicle.CurrentStatus.StoppedAt }
        val tripDetailsFilter = TripDetailsFilter("trip", vehicle.id, null, false)
        testViewModelFlow(viewModel).test {
            viewModel.setViewportManager(viewportProvider)
            viewModel.densityChanged(1f)
            assertEquals(MapViewModel.State.Overview, awaitItem())
            viewModel.recenter()
            // This delay approach seems potentially flakey
            delay(10)
            assertEquals(1, timesFollowCalled)
            val stop = TestData.stops["70113"]!!
            viewModel.selectedStop(stop, null)
            assertEquals(MapViewModel.State.StopSelected(stop, null), awaitItem())
            viewModel.recenter()
            delay(10)
            assertEquals(2, timesFollowCalled)
            viewModel.selectedTrip(null, null, tripDetailsFilter, vehicle)
            assertEquals(
                MapViewModel.State.TripSelected(null, null, tripDetailsFilter, vehicle),
                awaitItem(),
            )
            delay(10)
            assertEquals(1, timesVehicleOverViewCalled)
            viewModel.recenter(MapViewModel.Event.RecenterType.Trip)
            delay(10)
            assertEquals(2, timesVehicleOverViewCalled)
            viewModel.recenter(MapViewModel.Event.RecenterType.CurrentLocation)
            delay(10)
            assertEquals(3, timesFollowCalled)
        }
    }

    @Test
    fun clearsSelectedVehicle() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(dispatcher)
        val viewportProvider = MockViewportManager()
        val viewModel: MapViewModel = get()
        val vehicle = TestData.vehicle { currentStatus = Vehicle.CurrentStatus.StoppedAt }
        val tripDetailsFilter = TripDetailsFilter("trip", vehicle.id, null, false)
        testViewModelFlow(viewModel).test {
            viewModel.setViewportManager(viewportProvider)
            viewModel.densityChanged(1f)
            assertEquals(MapViewModel.State.Overview, awaitItem())
            val stop = TestData.stops["70113"]!!
            viewModel.selectedTrip(null, stop, tripDetailsFilter, vehicle)
            assertEquals(
                MapViewModel.State.TripSelected(stop, null, tripDetailsFilter, vehicle),
                awaitItem(),
            )
            viewModel.navChanged(SheetRoutes.RouteDetails("", RouteDetailsContext.Details))
            assertEquals(MapViewModel.State.StopSelected(stop, null), awaitItem())
        }
    }

    @Test
    @Ignore // TODO: Address flakiness
    fun whenInStopDetailsNotResetToAllRailOnAlertChange() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(dispatcher)

        val viewportProvider = MockViewportManager()
        val viewModel: MapViewModel = get()
        val layerManger = mock<IMapLayerManager>(MockMode.autofill)

        val objects = TestData.clone()
        val chestnutHill = objects.getStop("place-chill")
        val southStreet = objects.getStop("place-sougr")
        val informedEntities =
            listOf(chestnutHill, southStreet).flatMap {
                listOf(
                    Alert.InformedEntity(
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Ride,
                        ),
                        stop = it.id,
                        route = "Green-B",
                    )
                )
            }
        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                activePeriod =
                    mutableListOf(Alert.ActivePeriod(EasternTimeInstant.now().minus(5.hours), null))
                informedEntity = informedEntities.toMutableList()
            }

        viewModel.setViewportManager(viewportProvider)
        viewModel.layerManagerInitialized(layerManger)
        viewModel.densityChanged(1f)
        viewModel.navChanged(SheetRoutes.StopDetails(chestnutHill.id, null, null))
        advanceUntilIdle()

        testViewModelFlow(viewModel).test {
            awaitItemSatisfying { it == MapViewModel.State.StopSelected(chestnutHill, null) }
            advanceUntilIdle()
            verifySuspend { layerManger.updateRouteSourceData(matching { it.size == 1 }) }
            resetCalls(layerManger)
            viewModel.alertsChanged(AlertsStreamDataResponse(objects))
        }
        advanceUntilIdle()

        verifySuspend(VerifyMode.exactly(0)) {
            layerManger.updateRouteSourceData(matching { it.size == 6 })
        }
    }

    @Test
    @Ignore // TODO: Address flakiness
    fun allRailResetOnAlertChangeWhenInOverview() = runTest {
        val dispatcher = StandardTestDispatcher(this.testScheduler)
        setUpKoin(dispatcher)

        val viewportProvider = MockViewportManager()
        val viewModel: MapViewModel = get()
        val layerManger = mock<IMapLayerManager>(MockMode.autofill)

        val objects = TestData.clone()
        val chestnutHill = objects.getStop("place-chill")
        val southStreet = objects.getStop("place-sougr")
        val informedEntities =
            listOf(chestnutHill, southStreet).flatMap {
                listOf(
                    Alert.InformedEntity(
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Ride,
                        ),
                        stop = it.id,
                        route = "Green-B",
                    )
                )
            }
        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                activePeriod =
                    mutableListOf(Alert.ActivePeriod(EasternTimeInstant.now().minus(5.hours), null))
                informedEntity = informedEntities.toMutableList()
            }

        viewModel.layerManagerInitialized(layerManger)
        viewModel.setViewportManager(viewportProvider)
        viewModel.densityChanged(1f)
        advanceUntilIdle()
        testViewModelFlow(viewModel).test {
            awaitItemSatisfying { it == MapViewModel.State.Overview }
            advanceUntilIdle()
            verifySuspend() { layerManger.updateRouteSourceData(matching { it.size == 6 }) }
            resetCalls(layerManger)
            viewModel.alertsChanged(AlertsStreamDataResponse(objects))
        }

        advanceUntilIdle()
        verifySuspend() { layerManger.updateRouteSourceData(matching { it.size == 6 }) }
    }

    @Test
    @Ignore // TODO: Address flakiness
    fun allRailLayersResetWhenNavigatingToNearby() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(dispatcher)

        val viewportProvider = MockViewportManager()
        val viewModel: MapViewModel = get()
        val layerManger = mock<IMapLayerManager>(MockMode.autofill)

        val objects = TestData.clone()
        val chestnutHill = objects.getStop("place-chill")

        viewModel.setViewportManager(viewportProvider)
        viewModel.layerManagerInitialized(layerManger)
        viewModel.densityChanged(1f)
        viewModel.navChanged(SheetRoutes.StopDetails(chestnutHill.id, null, null))
        advanceUntilIdle()

        testViewModelFlow(viewModel).test {
            awaitItemSatisfying { it == MapViewModel.State.StopSelected(chestnutHill, null) }
            advanceUntilIdle()
            verifySuspend { layerManger.updateRouteSourceData(matching { it.size == 1 }) }
            resetCalls(layerManger)
            viewModel.navChanged(SheetRoutes.NearbyTransit)
            assertEquals(MapViewModel.State.Overview, awaitItem())
        }
        advanceUntilIdle()

        verifySuspend() { layerManger.updateRouteSourceData(matching { it.size == 6 }) }
    }

    @Test
    fun `layer manager can change`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(dispatcher)

        val layerManager1 = mock<IMapLayerManager>(MockMode.autofill)
        val layerManager2 = mock<IMapLayerManager>(MockMode.autofill)

        val viewModel: MapViewModel = get()

        testViewModelFlow(viewModel).test {
            viewModel.layerManagerInitialized(layerManager1)
            awaitItem()
            advanceUntilIdle()
            verifySuspend {
                layerManager1.addLayers(
                    any<List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>>(),
                    any(),
                    any(),
                    any(),
                )
            }
            viewModel.layerManagerInitialized(layerManager2)
            advanceUntilIdle()
            verifySuspend {
                layerManager2.addLayers(
                    any<List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>>(),
                    any(),
                    any(),
                    any(),
                )
            }
        }
    }

    @Test
    fun `applies timeout to events`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val sentryRepository = mock<ISentryRepository>(MockMode.autofill)
        setUpKoin(dispatcher) { sentry = sentryRepository }

        val viewportManager =
            mock<ViewportManager>(MockMode.autofill) {
                everySuspend { isDefault() } calls { suspendCancellableCoroutine {} }
            }

        val viewModel: MapViewModel = get()

        testViewModelFlow(viewModel).test {
            viewModel.setViewportManager(viewportManager)
            viewModel.locationPermissionsChanged(true)
            advanceUntilIdle()
            verify {
                sentryRepository.captureException(
                    matching<TimeoutCancellationException> {
                        it.message?.startsWith("Timed out after 10s") == true
                    }
                )
            }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
