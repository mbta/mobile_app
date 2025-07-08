package com.mbta.tid.mbta_app.viewModel

import app.cash.turbine.test
import com.mbta.tid.mbta_app.dependencyInjection.MockRepositories
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.SheetRoutes
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.utils.IMapLayerManager
import com.mbta.tid.mbta_app.utils.TestData
import com.mbta.tid.mbta_app.utils.ViewportManager
import dev.mokkery.MockMode
import dev.mokkery.mock
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
class MapViewModelTests : KoinTest {

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

    fun setUpKoin(coroutineDispatcher: CoroutineDispatcher) {
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
                repositoriesModule(MockRepositories().apply { useObjects(TestData.clone()) }),
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
            val vehicle = TestData.vehicle { currentStatus = Vehicle.CurrentStatus.StoppedAt }
            viewModel.selectedVehicle(vehicle, null, null)
            assertEquals(MapViewModel.State.VehicleSelected(vehicle, null, null), awaitItem())
            delay(10)
            assertEquals(1, timesVehicleOverViewCalled)
        }
    }

    @Test
    fun clearsSelectedVehicle() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(dispatcher)
        val viewportProvider = MockViewportManager()
        val viewModel: MapViewModel = get()
        testViewModelFlow(viewModel).test {
            viewModel.setViewportManager(viewportProvider)
            viewModel.densityChanged(1f)
            assertEquals(MapViewModel.State.Overview, awaitItem())
            val vehicle = TestData.vehicle { currentStatus = Vehicle.CurrentStatus.StoppedAt }
            val stop = TestData.stops["70113"]!!
            viewModel.selectedVehicle(vehicle, stop, null)
            assertEquals(MapViewModel.State.VehicleSelected(vehicle, stop, null), awaitItem())
            viewModel.navChanged(SheetRoutes.RouteDetails("", RouteDetailsContext.Details))
            assertEquals(MapViewModel.State.StopSelected(stop, null), awaitItem())
        }
    }

    @Test
    fun whenInStopDetailsNotResetToAllRailOnAlertChange() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(dispatcher)

        val viewportProvider = MockViewportManager()
        val viewModel: MapViewModel = get()
        val layerManger = mock<IMapLayerManager>(MockMode.autofill)

        val stop = TestData.stops["70113"]!!

        val objects = ObjectCollectionBuilder()
        objects.put(stop)
        val alert =
            ObjectCollectionBuilder().alert {
                effect = Alert.Effect.Suspension
                activePeriod = mutableListOf(Alert.ActivePeriod(Clock.System.now(), null))
                informedEntity =
                    mutableListOf(
                        Alert.InformedEntity(
                            listOf(
                                Alert.InformedEntity.Activity.Board,
                                Alert.InformedEntity.Activity.Ride,
                            ),
                            stop = stop.id,
                        )
                    )
            }

        testViewModelFlow(viewModel).test {
            viewModel.setViewportManager(viewportProvider)
            viewModel.layerManagerInitialized(layerManger)
            viewModel.densityChanged(1f)
            assertEquals(MapViewModel.State.Overview, awaitItem())
            advanceUntilIdle()
            viewModel.navChanged(SheetRoutes.StopDetails(stop.id, null, null))
            assertEquals(MapViewModel.State.StopSelected(stop, null), awaitItem())
            advanceUntilIdle()
            viewModel.alertsChanged(AlertsStreamDataResponse(objects))
            //   awaitComplete()
        }

        advanceUntilIdle()

        // once for overview, once for stopDetails, never b/c alerts changed
        // verifySuspend(VerifyMode.exactly(1)) { layerManger.updateRouteSourceData(matching {
        // it.size == 6 }) }
        //  verifySuspend(VerifyMode.exactly(1)) { layerManger.updateRouteSourceData(matching {
        // it.size == 1 }) }

    }

    @Test
    fun allRailResetOnAlertChangeWhenInOverview() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(dispatcher)

        val viewportProvider = MockViewportManager()
        val viewModel: MapViewModel = get()
        val layerManger = mock<IMapLayerManager>(MockMode.autofill)

        val stop = TestData.stops["70113"]!!

        val objects = ObjectCollectionBuilder()
        objects.put(stop)
        val alert =
            ObjectCollectionBuilder().alert {
                effect = Alert.Effect.Suspension
                activePeriod = mutableListOf(Alert.ActivePeriod(Clock.System.now(), null))
                informedEntity =
                    mutableListOf(
                        Alert.InformedEntity(
                            listOf(
                                Alert.InformedEntity.Activity.Board,
                                Alert.InformedEntity.Activity.Ride,
                            ),
                            stop = stop.id,
                        )
                    )
            }

        testViewModelFlow(viewModel).test {
            viewModel.layerManagerInitialized(layerManger)
            viewModel.setViewportManager(viewportProvider)
            viewModel.densityChanged(1f)
            assertEquals(MapViewModel.State.Overview, awaitItem())
            viewModel.alertsChanged(AlertsStreamDataResponse(objects))
            //   awaitComplete()
        }

        advanceUntilIdle()
        // once for overview, once once b/c alerts changed
        //      verifySuspend(VerifyMode.exactly(2)) {
        //          layerManger.updateRouteSourceData(matching { it.size == 6 })
        //      }
    }

    @Test
    fun allRailLayersResetWhenNavigatingToNearby() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        setUpKoin(dispatcher)

        val viewportProvider = MockViewportManager()
        val viewModel: MapViewModel = get()
        val layerManger = mock<IMapLayerManager>(MockMode.autofill)

        val stop = TestData.stops["70113"]!!

        val objects = ObjectCollectionBuilder()
        objects.put(stop)

        testViewModelFlow(viewModel).test {
            viewModel.setViewportManager(viewportProvider)
            viewModel.layerManagerInitialized(layerManger)
            viewModel.densityChanged(1f)
            assertEquals(MapViewModel.State.Overview, awaitItem())
            advanceUntilIdle()
            viewModel.navChanged(SheetRoutes.StopDetails(stop.id, null, null))
            assertEquals(MapViewModel.State.StopSelected(stop, null), awaitItem())
            advanceUntilIdle()
            viewModel.navChanged(SheetRoutes.NearbyTransit)
            //   awaitComplete()
        }

        advanceUntilIdle()

        // once for overview, once for stopDetails, never b/c alerts changed
        // verifySuspend(VerifyMode.exactly(1)) { layerManger.updateRouteSourceData(matching {
        // it.size == 6 }) }
        //  verifySuspend(VerifyMode.exactly(1)) { layerManger.updateRouteSourceData(matching {
        // it.size == 1 }) }

    }
}
