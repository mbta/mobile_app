package com.mbta.tid.mbta_app.android.pages

import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.AnalyticsScreen
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.alertDetails.AlertDetailsPage
import com.mbta.tid.mbta_app.android.component.DragHandle
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.sheet.BottomSheetScaffold
import com.mbta.tid.mbta_app.android.component.sheet.BottomSheetScaffoldState
import com.mbta.tid.mbta_app.android.component.sheet.SheetValue
import com.mbta.tid.mbta_app.android.location.LocationDataManager
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.android.map.HomeMapView
import com.mbta.tid.mbta_app.android.map.IMapViewModel
import com.mbta.tid.mbta_app.android.map.MapViewModel
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitTabViewModel
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitViewModel
import com.mbta.tid.mbta_app.android.search.SearchBarOverlay
import com.mbta.tid.mbta_app.android.state.SearchResultsViewModel
import com.mbta.tid.mbta_app.android.state.subscribeToVehicles
import com.mbta.tid.mbta_app.android.stopDetails.stopDetailsManagedVM
import com.mbta.tid.mbta_app.android.util.managePinnedRoutes
import com.mbta.tid.mbta_app.android.util.rememberPrevious
import com.mbta.tid.mbta_app.android.util.stateJsonSaver
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.history.Visit
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.usecases.VisitHistoryUsecase
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
data class NearbyTransit(
    val alertData: AlertsStreamDataResponse?,
    val globalResponse: GlobalResponse?,
    val hideMaps: Boolean,
    val lastNearbyTransitLocationState: MutableState<Position?>,
    val nearbyTransitSelectingLocationState: MutableState<Boolean>,
    val scaffoldState: BottomSheetScaffoldState,
    val locationDataManager: LocationDataManager,
    val viewportProvider: ViewportProvider,
) {
    var lastNearbyTransitLocation by this.lastNearbyTransitLocationState
    var nearbyTransitSelectingLocation by this.nearbyTransitSelectingLocationState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapAndSheetPage(
    modifier: Modifier = Modifier,
    nearbyTransit: NearbyTransit,
    sheetNavEntrypoint: SheetRoutes.Entrypoint,
    navBarVisible: Boolean,
    showNavBar: () -> Unit,
    hideNavBar: () -> Unit,
    bottomBar: @Composable () -> Unit,
    mapViewModel: IMapViewModel = viewModel(factory = MapViewModel.Factory()),
    searchResultsViewModel: SearchResultsViewModel,
    errorBannerViewModel: ErrorBannerViewModel =
        viewModel(factory = ErrorBannerViewModel.Factory(errorRepository = koinInject())),
    visitHistoryUsecase: VisitHistoryUsecase = koinInject(),
    clock: Clock = koinInject(),
) {
    LaunchedEffect(Unit) { errorBannerViewModel.activate() }

    val viewModel: NearbyTransitTabViewModel = viewModel()

    val navController = rememberNavController()

    val currentNavBackStackEntry by
        navController.currentBackStackEntryFlow.collectAsStateWithLifecycle(initialValue = null)

    val currentNavEntry = currentNavBackStackEntry?.let { SheetRoutes.fromNavBackStackEntry(it) }
    val previousNavEntry: SheetRoutes? = rememberPrevious(currentNavEntry)

    val (pinnedRoutes) = managePinnedRoutes()

    val now by timer(updateInterval = 5.seconds)

    fun updateStopFilter(stopId: String, stopFilter: StopDetailsFilter?) {
        viewModel.setStopFilter(
            currentNavEntry,
            stopId,
            stopFilter,
            { navController.popBackStack() },
            { navController.navigate(it) },
        )
    }

    fun updateTripFilter(stopId: String, tripFilter: TripDetailsFilter?) {
        viewModel.setTripFilter(
            currentNavEntry,
            stopId,
            tripFilter,
            { navController.popBackStack() },
            { navController.navigate(it) },
        )
    }

    val stopDetailsVM =
        stopDetailsManagedVM(
            filters =
                (when (currentNavEntry) {
                    is SheetRoutes.StopDetails ->
                        StopDetailsPageFilters(
                            currentNavEntry.stopId,
                            currentNavEntry.stopFilter,
                            currentNavEntry.tripFilter,
                        )
                    else -> null
                }),
            globalResponse = nearbyTransit.globalResponse,
            alertData = nearbyTransit.alertData,
            pinnedRoutes = pinnedRoutes ?: emptySet(),
            updateStopFilter = ::updateStopFilter,
            updateTripFilter = ::updateTripFilter,
            setMapSelectedVehicle = mapViewModel::setSelectedVehicle,
            now = now,
        )

    val routeCardData by viewModel.routeCardData.collectAsState()
    val tileScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(previousNavEntry) {
        if (
            previousNavEntry is SheetRoutes.StopDetails &&
                currentNavEntry is SheetRoutes.StopDetails &&
                (previousNavEntry.stopId != currentNavEntry.stopId ||
                    previousNavEntry.stopFilter != currentNavEntry.stopFilter)
        ) {
            tileScrollState.scrollTo(0)
        }
    }

    val vehiclesData: List<Vehicle> =
        subscribeToVehicles(
            routeDirection =
                when (currentNavEntry) {
                    is SheetRoutes.StopDetails -> currentNavEntry.stopFilter
                    else -> null
                }
        )

    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    val analytics: Analytics = koinInject()

    fun updateVisitHistory(stopId: String) {
        CoroutineScope(Dispatchers.Default).launch {
            visitHistoryUsecase.addVisit(Visit.StopVisit(stopId))
        }
    }

    fun handleSearchExpandedChange(expanded: Boolean) {
        searchExpanded = expanded
        searchResultsViewModel.expanded = expanded
        if (expanded) {
            hideNavBar()
            if (!nearbyTransit.hideMaps) {
                scope.launch {
                    nearbyTransit.scaffoldState.bottomSheetState.animateTo(SheetValue.Hidden)
                }
            }
        } else {
            showNavBar()
            if (!nearbyTransit.hideMaps) {
                scope.launch {
                    nearbyTransit.scaffoldState.bottomSheetState.animateTo(SheetValue.Medium)
                }
            }
        }
    }

    fun handleStopNavigation(stopId: String) {
        updateVisitHistory(stopId)
        mapViewModel.setSelectedVehicle(null)
        navController.navigate(SheetRoutes.StopDetails(stopId, null, null)) {
            popUpTo(SheetRoutes.NearbyTransit)
        }
    }

    fun handleVehicleTap(vehicle: Vehicle) {
        val tripId = vehicle.tripId ?: return
        val (stopId, stopFilter, tripFilter) =
            when (currentNavEntry) {
                is SheetRoutes.StopDetails ->
                    Triple(
                        currentNavEntry.stopId,
                        currentNavEntry.stopFilter,
                        currentNavEntry.tripFilter,
                    )
                else -> null
            } ?: return
        if (stopFilter == null || tripFilter?.tripId == tripId) return

        val routeCard =
            viewModel.routeCardData.value?.find { it.lineOrRoute.containsRoute(vehicle.routeId) }

        val upcoming =
            routeCard
                ?.stopData
                ?.flatMap { it.data }
                ?.flatMap { it.upcomingTrips }
                ?.firstOrNull { upcoming -> upcoming.trip.id == tripId }
        val stopSequence = upcoming?.stopSequence
        val routeId = upcoming?.trip?.routeId ?: vehicle.routeId ?: routeCard?.lineOrRoute?.id

        if (routeId != null) analytics.tappedVehicle(routeId)
        updateTripFilter(stopId, TripDetailsFilter(tripId, vehicle.id, stopSequence, true))
    }

    fun navigateToEntrypoint() {
        try {
            navController.navigate(sheetNavEntrypoint) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        } catch (e: IllegalStateException) {
            // This should only happen in tests when the navigation graph hasn't been initialized
            Log.w("MapAndSheetPage", "Failed to navigate to sheet entrypoint")
        }
    }

    fun openSearch() {
        searchFocusRequester.requestFocus()
    }

    var backgroundTimestamp: Long? by rememberSaveable { mutableStateOf(null) }
    LifecycleResumeEffect(null) {
        backgroundTimestamp?.let {
            val timeSinceBackground = clock.now().minus(Instant.fromEpochMilliseconds(it))
            if (timeSinceBackground > 1.hours) {
                navigateToEntrypoint()
                if (nearbyTransit.locationDataManager.hasPermission) {
                    nearbyTransit.viewportProvider.follow()
                }
            }
        }
        backgroundTimestamp = null
        onPauseOrDispose { backgroundTimestamp = clock.now().toEpochMilliseconds() }
    }

    LaunchedEffect(mapViewModel.lastMapboxErrorTimestamp.collectAsState(initial = null).value) {
        mapViewModel.loadConfig()
    }
    LaunchedEffect(nearbyTransit.alertData) { mapViewModel.setAlertsData(nearbyTransit.alertData) }

    LaunchedEffect(nearbyTransit.globalResponse) {
        mapViewModel.setGlobalResponse(nearbyTransit.globalResponse)
    }

    LaunchedEffect(sheetNavEntrypoint) { navigateToEntrypoint() }
    LaunchedEffect(currentNavEntry) {
        if (SheetRoutes.pageChanged(previousNavEntry, currentNavEntry)) {
            nearbyTransit.scaffoldState.bottomSheetState.animateTo(SheetValue.Medium)
        }
    }

    val modalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentModal by
        rememberSaveable(saver = stateJsonSaver()) { mutableStateOf<ModalRoutes?>(null) }

    fun openModal(modal: ModalRoutes) {
        currentModal = modal
        // modalSheetState.show() is implied by the `if (currentModal != null)`
    }

    fun closeModal() {
        scope.launch { modalSheetState.hide() }.invokeOnCompletion { currentModal = null }
    }

    @Composable
    fun SheetContent(modifier: Modifier = Modifier) {
        NavHost(
            navController,
            startDestination = sheetNavEntrypoint,
            modifier = Modifier.then(modifier),
            enterTransition = {
                val initialRoute = SheetRoutes.fromNavBackStackEntry(this.initialState)
                val (initialStopId, initialStopFilter) =
                    when (initialRoute) {
                        is SheetRoutes.StopDetails ->
                            Pair(initialRoute.stopId, initialRoute.stopFilter)
                        else -> Pair(null, null)
                    }

                val targetRoute = SheetRoutes.fromNavBackStackEntry(this.targetState)
                val (targetStopId, targetStopFilter) =
                    when (targetRoute) {
                        is SheetRoutes.StopDetails ->
                            Pair(targetRoute.stopId, targetRoute.stopFilter)
                        else -> Pair(null, null)
                    }

                // Skip animation if navigating within a single stop
                if (
                    initialStopId != null &&
                        targetStopId != null &&
                        initialStopId == targetStopId &&
                        initialStopFilter?.routeId == targetStopFilter?.routeId
                ) {
                    EnterTransition.None
                } else {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(easing = EaseInOut),
                    )
                }
            },
        ) {
            composable<SheetRoutes.Favorites>(typeMap = SheetRoutes.typeMap) {
                FavoritesPage(modifier = modifier, openSheetRoute = navController::navigate)
            }

            composable<SheetRoutes.StopDetails>(typeMap = SheetRoutes.typeMap) { backStackEntry ->
                val navRoute: SheetRoutes.StopDetails = backStackEntry.toRoute()
                val filters =
                    StopDetailsPageFilters(
                        navRoute.stopId,
                        navRoute.stopFilter,
                        navRoute.tripFilter,
                    )

                LaunchedEffect(navRoute) {
                    if (navBarVisible) {
                        hideNavBar()
                    }
                    if (filters.stopFilter != null) {
                        analytics.track(AnalyticsScreen.StopDetailsFiltered)
                    } else {
                        analytics.track(AnalyticsScreen.StopDetailsUnfiltered)
                    }
                }

                StopDetailsPage(
                    modifier = modifier,
                    viewModel = stopDetailsVM,
                    filters = filters,
                    allAlerts = nearbyTransit.alertData,
                    onClose = { navController.popBackStack() },
                    updateStopFilter = { updateStopFilter(navRoute.stopId, it) },
                    updateTripFilter = { updateTripFilter(navRoute.stopId, it) },
                    updateRouteCardData = { viewModel.setRouteCardData(it) },
                    tileScrollState = tileScrollState,
                    openModal = ::openModal,
                    openSheetRoute = navController::navigate,
                    errorBannerViewModel = errorBannerViewModel,
                )
            }

            composable<SheetRoutes.NearbyTransit>(typeMap = SheetRoutes.typeMap) {
                // for ViewModel reasons, must be within the `composable` to be the same instance
                val nearbyViewModel: NearbyTransitViewModel = koinViewModel()
                LaunchedEffect(true) {
                    if (!navBarVisible && !searchExpanded) {
                        showNavBar()
                    }
                    analytics.track(AnalyticsScreen.NearbyTransit)
                }

                NearbyTransitPage(
                    nearbyTransit,
                    onOpenStopDetails = { stopId, filter ->
                        updateVisitHistory(stopId)
                        navController.navigate(SheetRoutes.StopDetails(stopId, filter, null))
                    },
                    openSearch = ::openSearch,
                    nearbyViewModel = nearbyViewModel,
                    errorBannerViewModel = errorBannerViewModel,
                )
            }
        }
    }
    // setting WindowInsets top to 0 to prevent the sheet from having extra padding on top even
    // when not fully expanded https://stackoverflow.com/a/77361483
    Scaffold(bottomBar = bottomBar, contentWindowInsets = WindowInsets(top = 0.dp)) {
        outerSheetPadding ->
        val showSearchBar = remember(currentNavEntry) { currentNavEntry?.showSearchBar ?: true }
        if (nearbyTransit.hideMaps) {
            LaunchedEffect(null) {
                nearbyTransit.locationDataManager.currentLocation.collect { location ->
                    nearbyTransit.viewportProvider.updateCameraState(location)
                }
            }

            SearchBarOverlay(
                searchExpanded,
                showSearchBar,
                ::handleSearchExpandedChange,
                ::handleStopNavigation,
                onRouteNavigation = {},
                searchFocusRequester,
                searchResultsViewModel,
            ) {
                SheetContent(
                    Modifier.padding(top = if (showSearchBar) 94.dp else 0.dp)
                        .statusBarsPadding()
                        .fillMaxSize()
                )
            }
        } else {
            BottomSheetScaffold(
                sheetDragHandle = { DragHandle() },
                sheetContent = {
                    var sheetHeight by remember { mutableStateOf(0.dp) }
                    val density = LocalDensity.current

                    Box(
                        modifier =
                            Modifier.onGloballyPositioned {
                                    // https://issuetracker.google.com/issues/287390075#comment7
                                    sheetHeight =
                                        with(density) { it.boundsInWindow().height.toDp() }
                                }
                                .fillMaxSize()
                    ) {
                        val statusBarPadding =
                            if (
                                nearbyTransit.scaffoldState.bottomSheetState.currentValue !=
                                    SheetValue.Large
                            ) {
                                Modifier
                            } else {
                                Modifier.statusBarsPadding()
                            }

                        SheetContent(
                            statusBarPadding.height(sheetHeight).padding(outerSheetPadding)
                        )
                    }
                },
                sheetContainerColor =
                    when (currentNavEntry) {
                        SheetRoutes.NearbyTransit -> colorResource(R.color.sheet_background)
                        is SheetRoutes.StopDetails -> colorResource(R.color.fill2)
                        else -> colorResource(R.color.fill1)
                    },
                scaffoldState = nearbyTransit.scaffoldState,
            ) { sheetPadding ->
                SearchBarOverlay(
                    searchExpanded,
                    showSearchBar,
                    ::handleSearchExpandedChange,
                    ::handleStopNavigation,
                    onRouteNavigation = {},
                    searchFocusRequester,
                    searchResultsViewModel,
                ) {
                    HomeMapView(
                        sheetPadding = sheetPadding,
                        lastNearbyTransitLocation = nearbyTransit.lastNearbyTransitLocation,
                        nearbyTransitSelectingLocationState =
                            nearbyTransit.nearbyTransitSelectingLocationState,
                        locationDataManager = nearbyTransit.locationDataManager,
                        viewportProvider = nearbyTransit.viewportProvider,
                        currentNavEntry = currentNavEntry,
                        handleStopNavigation = ::handleStopNavigation,
                        handleVehicleTap = ::handleVehicleTap,
                        vehiclesData = vehiclesData,
                        routeCardData = routeCardData,
                        viewModel = mapViewModel,
                        searchResultsViewModel = searchResultsViewModel,
                    )
                }
            }
        }
    }
    if (currentModal != null) {
        ModalBottomSheet(
            onDismissRequest = { closeModal() },
            sheetState = modalSheetState,
            dragHandle = null,
        ) {
            Column {
                when (val modal = currentModal) {
                    is ModalRoutes.AlertDetails ->
                        AlertDetailsPage(
                            modal.alertId,
                            modal.lineId,
                            modal.routeIds,
                            modal.stopId,
                            nearbyTransit.alertData,
                            goBack = { closeModal() },
                        )
                    is ModalRoutes.Explainer ->
                        ExplainerPage(modal.type, modal.routeAccents, goBack = { closeModal() })
                    null -> {}
                }
            }
        }
    }
}
