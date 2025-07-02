package com.mbta.tid.mbta_app.viewModel

import app.cash.turbine.test
import com.mbta.tid.mbta_app.dependencyInjection.MockRepositories
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.model.SheetRoutes
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.utils.TestData
import com.mbta.tid.mbta_app.utils.ViewportManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

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

    @BeforeTest
    fun setupKoin() {
        startKoin {
            modules(
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
            assertEquals(MapViewModel.State.Unfiltered, awaitItem())
            val stop = TestData.stops["70113"]!!
            val stopDetails = SheetRoutes.StopDetails(stop.id, null, null)
            viewModel.navChanged(stopDetails)
            assertEquals(MapViewModel.State.StopSelected(stop, null), awaitItem())
            viewModel.navChanged(SheetRoutes.NearbyTransit)
            assertEquals(MapViewModel.State.Unfiltered, awaitItem())
            delay(10)
            assertEquals(1, timesRestoreViewportCalled)
            assertEquals(1, timesSaveViewportCalled)
        }
    }

    @Test
    fun recenter() = runTest {
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
            assertEquals(MapViewModel.State.Unfiltered, awaitItem())
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
        val viewportProvider = MockViewportManager()
        val viewModel: MapViewModel = get()
        testViewModelFlow(viewModel).test {
            viewModel.setViewportManager(viewportProvider)
            viewModel.densityChanged(1f)
            assertEquals(MapViewModel.State.Unfiltered, awaitItem())
            val vehicle = TestData.vehicle { currentStatus = Vehicle.CurrentStatus.StoppedAt }
            val stop = TestData.stops["70113"]!!
            viewModel.selectedVehicle(vehicle, stop, null)
            assertEquals(MapViewModel.State.VehicleSelected(vehicle, stop, null), awaitItem())
            viewModel.navChanged(SheetRoutes.RouteDetails("", RouteDetailsContext.Details))
            assertEquals(MapViewModel.State.StopSelected(stop, null), awaitItem())
        }
    }
}
