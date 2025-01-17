package com.mbta.tid.mbta_app.android

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mbta.tid.mbta_app.analytics.AnalyticsColorScheme
import com.mbta.tid.mbta_app.analytics.AnalyticsScreen
import com.mbta.tid.mbta_app.android.analytics.AnalyticsProvider
import com.mbta.tid.mbta_app.android.component.BottomNavBar
import com.mbta.tid.mbta_app.android.component.sheet.rememberBottomSheetScaffoldState
import com.mbta.tid.mbta_app.android.component.sheet.rememberStandardBottomSheetState
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.android.location.rememberLocationDataManager
import com.mbta.tid.mbta_app.android.pages.MorePage
import com.mbta.tid.mbta_app.android.pages.NearbyTransit
import com.mbta.tid.mbta_app.android.pages.NearbyTransitPage
import com.mbta.tid.mbta_app.android.pages.OnboardingPage
import com.mbta.tid.mbta_app.android.phoenix.PhoenixSocketWrapper
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.state.subscribeToAlerts
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.repositories.IAccessibilityStatusRepository
import io.github.dellisd.spatialk.geojson.Position
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(MapboxExperimental::class, ExperimentalMaterial3Api::class)
@Composable
fun ContentView(
    socket: PhoenixSocket = koinInject(),
    viewModel: ContentViewModel = koinViewModel(),
    accessibilityStatusRepository: IAccessibilityStatusRepository = koinInject(),
) {
    val navController = rememberNavController()
    val alertData: AlertsStreamDataResponse? = subscribeToAlerts()
    val globalResponse = getGlobalData("ContentView.getGlobalData")
    val hideMaps by viewModel.hideMaps.collectAsState()
    val pendingOnboarding = viewModel.pendingOnboarding.collectAsState().value
    val locationDataManager = rememberLocationDataManager()
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(ViewportProvider.Companion.Defaults.center)
            zoom(ViewportProvider.Companion.Defaults.zoom)
            pitch(0.0)
            bearing(0.0)
            transitionToFollowPuckState()
        }
    }
    val viewportProvider = remember { ViewportProvider(mapViewportState) }
    val lastNearbyTransitLocationState = remember { mutableStateOf<Position?>(null) }
    val nearbyTransitSelectingLocationState = remember { mutableStateOf(false) }
    val scaffoldState =
        rememberBottomSheetScaffoldState(bottomSheetState = rememberStandardBottomSheetState())
    var navBarVisible by remember { mutableStateOf(true) }

    LifecycleResumeEffect(null) {
        socket.attach()
        (socket as? PhoenixSocketWrapper)?.attachLogging()
        onPauseOrDispose { socket.detach() }
    }

    val analytics = AnalyticsProvider.shared

    val colorScheme =
        if (isSystemInDarkTheme()) AnalyticsColorScheme.Dark else AnalyticsColorScheme.Light
    LaunchedEffect(colorScheme) { analytics.recordSession(colorScheme) }
    LaunchedEffect(hideMaps) { analytics.recordSessionHideMaps(hideMaps) }
    val screenReaderEnabled = accessibilityStatusRepository.isScreenReaderEnabled()
    LaunchedEffect(screenReaderEnabled) { analytics.recordSessionVoiceOver(screenReaderEnabled) }

    if (!pendingOnboarding.isNullOrEmpty()) {
        OnboardingPage(
            pendingOnboarding,
            onFinish = { viewModel.clearPendingOnboarding() },
            locationDataManager = locationDataManager
        )
        return
    }

    val sheetModifier = Modifier.fillMaxSize().background(colorResource(id = R.color.fill1))
    NavHost(navController = navController, startDestination = Routes.NearbyTransit) {
        composable<Routes.NearbyTransit> {
            LaunchedEffect(null) { viewModel.loadHideMaps() }

            NearbyTransitPage(
                modifier = sheetModifier,
                NearbyTransit(
                    alertData = alertData,
                    globalResponse = globalResponse,
                    hideMaps = hideMaps,
                    lastNearbyTransitLocationState = lastNearbyTransitLocationState,
                    nearbyTransitSelectingLocationState = nearbyTransitSelectingLocationState,
                    scaffoldState = scaffoldState,
                    locationDataManager = locationDataManager,
                    viewportProvider = viewportProvider,
                ),
                navBarVisible = navBarVisible,
                showNavBar = { navBarVisible = true },
                hideNavBar = { navBarVisible = false },
                bottomBar = {
                    if (navBarVisible) {
                        BottomNavBar(
                            currentDestination = navController.currentBackStackEntry?.destination,
                            navigateToNearby = { navController.navigate(Routes.NearbyTransit) },
                            navigateToMore = { navController.navigate(Routes.More) }
                        )
                    }
                }
            )
        }
        composable<Routes.More> {
            LaunchedEffect(null) { analytics.track(AnalyticsScreen.Settings) }
            MorePage(
                bottomBar = {
                    BottomNavBar(
                        currentDestination = navController.currentBackStackEntry?.destination,
                        navigateToNearby = { navController.navigate(Routes.NearbyTransit) },
                        navigateToMore = { navController.navigate(Routes.More) }
                    )
                }
            )
        }
    }
}
