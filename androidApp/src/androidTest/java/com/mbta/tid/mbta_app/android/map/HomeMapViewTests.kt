package com.mbta.tid.mbta_app.android.map

import android.location.Location
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.ComposeNavigator
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mbta.tid.mbta_app.android.location.MockLocationDataManager
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.repositories.MockConfigRepository
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import com.mbta.tid.mbta_app.usecases.ConfigUseCase
import org.junit.Rule
import org.junit.Test

class HomeMapViewTests {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testRecenterNotShownWhenNoPermissions() {

        val locationManager = MockLocationDataManager(Location("mock"))

        locationManager.hasPermission = false

        composeTestRule.setContent {
            HomeMapView(
                lastNearbyTransitLocation = null,
                nearbyTransitSelectingLocationState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = ViewportProvider(MapViewportState()),
                currentNavEntry = null,
                handleStopNavigation = {},
                vehiclesData = emptyList(),
                stopDetailsDepartures = null,
                stopDetailsFilter = null,
                viewModel =
                    MapViewModel(ConfigUseCase(MockConfigRepository(), MockSentryRepository()), {})
            )
        }

        composeTestRule.onNodeWithContentDescription("Recenter").assertDoesNotExist()
    }

    @Test
    fun testRecenterNotShownWhenPermissions() {

        val locationManager = MockLocationDataManager(Location("mock"))

        locationManager.hasPermission = true

        composeTestRule.setContent {
            HomeMapView(
                lastNearbyTransitLocation = null,
                nearbyTransitSelectingLocationState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = ViewportProvider(MapViewportState()),
                currentNavEntry = null,
                handleStopNavigation = {},
                vehiclesData = emptyList(),
                stopDetailsDepartures = null,
                stopDetailsFilter = null,
                viewModel =
                    MapViewModel(ConfigUseCase(MockConfigRepository(), MockSentryRepository()), {})
            )
        }

        composeTestRule.onNodeWithContentDescription("Recenter").assertIsDisplayed()
    }

    @Test
    fun testLocationAuthShownWhenNoPermissions() {

        val locationManager = MockLocationDataManager(Location("mock"))

        locationManager.hasPermission = false

        val destination = ComposeNavigator().createDestination()

        destination.route = "NearbyTransit"

        composeTestRule.setContent {
            HomeMapView(
                lastNearbyTransitLocation = null,
                nearbyTransitSelectingLocationState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = ViewportProvider(MapViewportState()),
                currentNavEntry = NavBackStackEntry.create(LocalContext.current, destination),
                handleStopNavigation = {},
                vehiclesData = emptyList(),
                stopDetailsDepartures = null,
                stopDetailsFilter = null,
                viewModel =
                    MapViewModel(ConfigUseCase(MockConfigRepository(), MockSentryRepository()), {})
            )
        }

        composeTestRule.onNodeWithText("Location Services is off").assertIsDisplayed()
    }

    @Test
    fun testLocationAuthNotShownWhenPermissions() {

        val locationManager = MockLocationDataManager(Location("mock"))

        locationManager.hasPermission = true

        composeTestRule.setContent {
            HomeMapView(
                lastNearbyTransitLocation = null,
                nearbyTransitSelectingLocationState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = ViewportProvider(MapViewportState()),
                currentNavEntry = null,
                handleStopNavigation = {},
                vehiclesData = emptyList(),
                stopDetailsDepartures = null,
                stopDetailsFilter = null,
                viewModel =
                    MapViewModel(ConfigUseCase(MockConfigRepository(), MockSentryRepository()), {})
            )
        }

        composeTestRule.onNodeWithText("Location Services is off").assertDoesNotExist()
    }

    @Test
    fun testLocationAuthNotShownStopDetails() {

        val locationManager = MockLocationDataManager(Location("mock"))

        locationManager.hasPermission = false

        val destination = ComposeNavigator().createDestination()

        destination.route = "StopDetails"

        composeTestRule.setContent {
            HomeMapView(
                lastNearbyTransitLocation = null,
                nearbyTransitSelectingLocationState = mutableStateOf(false),
                locationDataManager = locationManager,
                viewportProvider = ViewportProvider(MapViewportState()),
                currentNavEntry = NavBackStackEntry.create(LocalContext.current, destination),
                handleStopNavigation = {},
                vehiclesData = emptyList(),
                stopDetailsDepartures = null,
                stopDetailsFilter = null,
                viewModel =
                    MapViewModel(ConfigUseCase(MockConfigRepository(), MockSentryRepository()), {})
            )
        }

        composeTestRule.onNodeWithText("Location Services is off").assertDoesNotExist()
    }
}
