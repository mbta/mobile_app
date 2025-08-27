package com.mbta.tid.mbta_app.android.map

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraState
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mbta.tid.mbta_app.android.location.MockLocationDataManager
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockRailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.MockStopRepository
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.TestData
import com.mbta.tid.mbta_app.viewModel.IMapViewModel
import com.mbta.tid.mbta_app.viewModel.MapViewModel
import com.mbta.tid.mbta_app.viewModel.MockRouteCardDataViewModel
import dev.mokkery.MockMode
import dev.mokkery.answering.autofill.AutofillProvider
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class HomeMapViewTests {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testRecenterNotShownWhenNoPermissions() = runBlocking {
        val locationManager = MockLocationDataManager()
        val viewModel =
            MapViewModel(
                MockRouteCardDataViewModel(),
                MockGlobalRepository(),
                MockRailRouteShapeRepository(),
                MockStopRepository(),
                Dispatchers.Default,
                Dispatchers.IO,
            )
        val viewportProvider = ViewportProvider(MapViewportState())
        val configManager = MapboxConfigManager()
        configManager.loadConfig()
        locationManager.hasPermission = false
        composeTestRule.setContent {
            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastLoadedLocation = null,
                isTargetingState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = viewportProvider,
                currentNavEntry = null,
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                viewModel = viewModel,
                configManager,
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Recenter map on my location")
            .assertDoesNotExist()
    }

    @Test
    fun testRecenterNotShownWhenPermissionsAndAtDefaultCenter(): Unit = runBlocking {
        val locationManager = MockLocationDataManager()
        val viewModel =
            MapViewModel(
                MockRouteCardDataViewModel(),
                MockGlobalRepository(),
                MockRailRouteShapeRepository(),
                MockStopRepository(),
                Dispatchers.Default,
                Dispatchers.IO,
            )
        val viewportProvider = ViewportProvider(MapViewportState())
        viewModel.setViewportManager(viewportProvider)
        val configManager = MapboxConfigManager()
        configManager.loadConfig()
        locationManager.hasPermission = true
        composeTestRule.setContent {
            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastLoadedLocation = null,
                isTargetingState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = viewportProvider,
                currentNavEntry = null,
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                viewModel = viewModel,
                configManager,
            )
        }
        composeTestRule
            .onNodeWithContentDescription("Recenter map on my location")
            .assertIsNotDisplayed()
    }

    @Test
    fun testRecenterShownWhenPermissionsAndMapNotAtDefaultCenter(): Unit = runBlocking {
        val locationManager = MockLocationDataManager()
        val customLocation =
            Point.fromLngLat(
                ViewportProvider.Companion.Defaults.center.latitude() + 1,
                ViewportProvider.Companion.Defaults.center.longitude() + 1,
            )

        val viewModel =
            MapViewModel(
                MockRouteCardDataViewModel(),
                MockGlobalRepository(),
                MockRailRouteShapeRepository(),
                MockStopRepository(),
                Dispatchers.Default,
                Dispatchers.IO,
            )
        val viewportProvider =
            ViewportProvider(
                MapViewportState(
                    initialCameraState =
                        CameraState(customLocation, EdgeInsets(0.0, 0.0, 0.0, 0.0), 0.0, 0.0, 0.0)
                )
            )
        viewModel.setViewportManager(viewportProvider)
        val configManager = MapboxConfigManager()
        configManager.loadConfig()
        locationManager.hasPermission = true
        composeTestRule.setContent {
            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastLoadedLocation = null,
                isTargetingState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = viewportProvider,
                currentNavEntry = null,
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                viewModel = viewModel,
                configManager,
            )
        }
        composeTestRule
            .onNodeWithContentDescription("Recenter map on my location")
            .assertIsDisplayed()
    }

    @Test
    fun testLocationAuthShownWhenNoPermissions(): Unit = runBlocking {
        val locationManager = MockLocationDataManager(null)
        val viewModel =
            MapViewModel(
                MockRouteCardDataViewModel(),
                MockGlobalRepository(),
                MockRailRouteShapeRepository(),
                MockStopRepository(),
                Dispatchers.Default,
                Dispatchers.IO,
            )
        val viewportProvider = ViewportProvider(MapViewportState())
        viewModel.setViewportManager(viewportProvider)
        val configManager = MapboxConfigManager()
        configManager.loadConfig()
        locationManager.hasPermission = false
        composeTestRule.setContent {
            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastLoadedLocation = null,
                isTargetingState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = viewportProvider,
                currentNavEntry = SheetRoutes.NearbyTransit,
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                viewModel = viewModel,
                configManager,
            )
        }
        composeTestRule.onNodeWithText("Location Services is off").assertIsDisplayed()
    }

    @Test
    fun testLocationAuthNotShownWhenPermissions() = runBlocking {
        val locationManager = MockLocationDataManager()
        val viewModel =
            MapViewModel(
                MockRouteCardDataViewModel(),
                MockGlobalRepository(),
                MockRailRouteShapeRepository(),
                MockStopRepository(),
                Dispatchers.Default,
                Dispatchers.IO,
            )
        val viewportProvider = ViewportProvider(MapViewportState())
        viewModel.setViewportManager(viewportProvider)
        val configManager = MapboxConfigManager()
        configManager.loadConfig()
        locationManager.hasPermission = true
        composeTestRule.setContent {
            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastLoadedLocation = null,
                isTargetingState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = viewportProvider,
                currentNavEntry = null,
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                viewModel = viewModel,
                configManager,
            )
        }

        composeTestRule.onNodeWithText("Location Services is off").assertDoesNotExist()
    }

    @Test
    fun testLocationAuthNotShownStopDetails() = runBlocking {
        val locationManager = MockLocationDataManager()
        val viewModel =
            MapViewModel(
                MockRouteCardDataViewModel(),
                MockGlobalRepository(),
                MockRailRouteShapeRepository(),
                MockStopRepository(),
                Dispatchers.Default,
                Dispatchers.IO,
            )
        val viewportProvider = ViewportProvider(MapViewportState())
        viewModel.setViewportManager(viewportProvider)
        val configManager = MapboxConfigManager()
        configManager.loadConfig()
        locationManager.hasPermission = false
        composeTestRule.setContent {
            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastLoadedLocation = null,
                isTargetingState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = viewportProvider,
                currentNavEntry = SheetRoutes.StopDetails("stopId", null, null),
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                viewModel = viewModel,
                configManager,
            )
        }

        composeTestRule.onNodeWithText("Location Services is off").assertDoesNotExist()
    }

    @Test
    fun testOverviewNotShownWhenNoPermissionsStopDetails() = runBlocking {
        val locationManager = MockLocationDataManager()
        val viewModel =
            MapViewModel(
                MockRouteCardDataViewModel(),
                MockGlobalRepository(),
                MockRailRouteShapeRepository(),
                MockStopRepository(),
                Dispatchers.Default,
                Dispatchers.IO,
            )
        val viewportProvider = ViewportProvider(MapViewportState())
        viewModel.setViewportManager(viewportProvider)
        val configManager = MapboxConfigManager()
        configManager.loadConfig()
        locationManager.hasPermission = false
        composeTestRule.setContent {
            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastLoadedLocation = null,
                isTargetingState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = viewportProvider,
                currentNavEntry = SheetRoutes.StopDetails("stopId", null, null),
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                viewModel = viewModel,
                configManager,
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Recenter map on my location")
            .assertDoesNotExist()
    }

    @Test
    fun testRecenterButtonVisibilityWhenOnStopDetails(): Unit = runBlocking {
        val locationManager = MockLocationDataManager()
        locationManager.hasPermission = true

        val viewportProvider = ViewportProvider(MapViewportState())

        AutofillProvider.forMockMode.types.register(StateFlow::class) { MutableStateFlow(null) }

        AutofillProvider.forMockMode.types.register(StateFlow::class) { MutableStateFlow(null) }

        AutofillProvider.forMockMode.types.register(Flow::class) { MutableStateFlow(null) }

        val mapVM = mock<IMapViewModel>(MockMode.autofill)
        val configManager = mock<IMapboxConfigManager>(MockMode.autofill)
        val state = MapViewModel.State.StopSelected(TestData.getStop("121"), null)
        every { mapVM.models } returns MutableStateFlow(state)
        every { configManager.configLoadAttempted } returns MutableStateFlow(true)
        composeTestRule.setContent {
            val isTargetingState = remember { mutableStateOf(false) }

            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastLoadedLocation = null,
                isTargetingState = isTargetingState,
                locationDataManager = locationManager,
                viewportProvider = viewportProvider,
                currentNavEntry = SheetRoutes.StopDetails(TestData.getStop("121").id, null, null),
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                viewModel = mapVM,
                configManager,
            )
        }

        composeTestRule.waitUntil { composeTestRule.onNodeWithTag("recenterButton").isDisplayed() }
        composeTestRule.onNodeWithTag("recenterButton").assertIsDisplayed()
    }

    @Test
    fun testPlaceholderGrid(): Unit = runBlocking {
        val viewModel =
            MapViewModel(
                MockRouteCardDataViewModel(),
                MockGlobalRepository(),
                MockRailRouteShapeRepository(),
                MockStopRepository(),
                Dispatchers.Default,
                Dispatchers.IO,
            )
        val viewportProvider = ViewportProvider(MapViewportState())
        viewModel.setViewportManager(viewportProvider)
        open class MockConfigManager : IMapboxConfigManager {
            private val _configLoadAttempted = MutableStateFlow(false)
            override val configLoadAttempted: StateFlow<Boolean> = _configLoadAttempted
            override var lastMapboxErrorTimestamp: Flow<EasternTimeInstant?> =
                MutableStateFlow(value = null)
            var loadConfigCalledCount = 0

            override suspend fun loadConfig() {
                _configLoadAttempted.value = true
            }
        }
        val configManager = MockConfigManager()
        composeTestRule.setContent {
            HomeMapView(
                sheetPadding = PaddingValues(0.dp),
                lastLoadedLocation = null,
                isTargetingState = mutableStateOf(false),
                locationDataManager = MockLocationDataManager(),
                viewportProvider = viewportProvider,
                currentNavEntry = null,
                handleStopNavigation = {},
                handleVehicleTap = {},
                vehiclesData = emptyList(),
                viewModel = viewModel,
                configManager,
            )
        }
        composeTestRule.onNodeWithTag("Empty map grid").assertIsDisplayed()
        configManager.loadConfig()
        composeTestRule.onNodeWithTag("Empty map grid").assertIsNotDisplayed()
    }
}
