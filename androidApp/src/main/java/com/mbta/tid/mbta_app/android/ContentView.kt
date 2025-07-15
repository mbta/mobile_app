package com.mbta.tid.mbta_app.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.AnalyticsColorScheme
import com.mbta.tid.mbta_app.analytics.AnalyticsScreen
import com.mbta.tid.mbta_app.android.component.BottomNavBar
import com.mbta.tid.mbta_app.android.component.sheet.rememberBottomSheetScaffoldState
import com.mbta.tid.mbta_app.android.component.sheet.rememberStandardBottomSheetState
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.android.location.rememberLocationDataManager
import com.mbta.tid.mbta_app.android.pages.MapAndSheetPage
import com.mbta.tid.mbta_app.android.pages.MorePage
import com.mbta.tid.mbta_app.android.pages.NearbyTransit
import com.mbta.tid.mbta_app.android.pages.OnboardingPage
import com.mbta.tid.mbta_app.android.phoenix.PhoenixSocketWrapper
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.state.subscribeToAlerts
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.model.SheetRoutes
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.repositories.IAccessibilityStatusRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.viewModel.MapViewModel
import io.github.dellisd.spatialk.geojson.Position
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentView(
    socket: PhoenixSocket = koinInject(),
    viewModel: ContentViewModel = koinViewModel(),
    mapViewModel: MapViewModel = koinInject(),
    accessibilityStatusRepository: IAccessibilityStatusRepository = koinInject(),
) {
    val navController = rememberNavController()
    var sheetNavEntrypoint: SheetRoutes.Entrypoint by
        rememberSaveable(stateSaver = SheetRoutes.EntrypointSaver) {
            mutableStateOf(SheetRoutes.NearbyTransit)
        }

    val alertData: AlertsStreamDataResponse? = subscribeToAlerts()
    val globalResponse = getGlobalData("ContentView.getGlobalData")
    val hideMaps = SettingsCache.get(Settings.HideMaps)
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

    LaunchedEffect(Unit) { mapViewModel.setViewportManager(viewportProvider) }

    LifecycleResumeEffect(null) {
        socket.attach()
        (socket as? PhoenixSocketWrapper)?.attachLogging()
        onPauseOrDispose { socket.detach() }
    }

    val analytics: Analytics = koinInject()

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
            locationDataManager = locationDataManager,
        )
        return
    }

    val sheetModifier = Modifier.fillMaxSize()
    NavHost(navController = navController, startDestination = Routes.MapAndSheet) {
        composable<Routes.MapAndSheet> {
            MapAndSheetPage(
                modifier = sheetModifier,
                NearbyTransit(
                    alertData = alertData,
                    globalResponse = globalResponse,
                    lastNearbyTransitLocationState = lastNearbyTransitLocationState,
                    nearbyTransitSelectingLocationState = nearbyTransitSelectingLocationState,
                    scaffoldState = scaffoldState,
                    locationDataManager = locationDataManager,
                    viewportProvider = viewportProvider,
                ),
                sheetNavEntrypoint = sheetNavEntrypoint,
                navBarVisible = navBarVisible,
                showNavBar = { navBarVisible = true },
                hideNavBar = { navBarVisible = false },
                mapViewModel = mapViewModel,
                bottomBar = {
                    if (navBarVisible) {
                        BottomNavBar(
                            currentDestination =
                                Routes.fromNavBackStackEntry(navController.currentBackStackEntry),
                            sheetNavEntrypoint = sheetNavEntrypoint,
                            navigateToFavorites = { sheetNavEntrypoint = SheetRoutes.Favorites },
                            navigateToNearby = { sheetNavEntrypoint = SheetRoutes.NearbyTransit },
                            navigateToMore = { navController.navigate(Routes.More) },
                        )
                    }
                },
            )
        }
        composable<Routes.More> {
            LaunchedEffect(null) { analytics.track(AnalyticsScreen.Settings) }
            MorePage(
                bottomBar = {
                    BottomNavBar(
                        currentDestination =
                            Routes.fromNavBackStackEntry(navController.currentBackStackEntry),
                        sheetNavEntrypoint = sheetNavEntrypoint,
                        navigateToFavorites = {
                            navController.popBackStack()
                            sheetNavEntrypoint = SheetRoutes.Favorites
                        },
                        navigateToNearby = {
                            navController.popBackStack()
                            sheetNavEntrypoint = SheetRoutes.NearbyTransit
                        },
                        navigateToMore = {},
                    )
                }
            )
        }
    }
}
