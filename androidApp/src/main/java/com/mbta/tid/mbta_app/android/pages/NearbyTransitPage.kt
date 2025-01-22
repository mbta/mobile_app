package com.mbta.tid.mbta_app.android.pages

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mapbox.maps.MapboxExperimental
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.AnalyticsScreen
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.StopFilterParameterType
import com.mbta.tid.mbta_app.android.TripFilterParameterType
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
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitView
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitViewModel
import com.mbta.tid.mbta_app.android.nearbyTransit.NoNearbyStopsView
import com.mbta.tid.mbta_app.android.search.SearchBarOverlay
import com.mbta.tid.mbta_app.android.state.subscribeToVehicles
import com.mbta.tid.mbta_app.android.stopDetails.stopDetailsManagedVM
import com.mbta.tid.mbta_app.android.util.managePinnedRoutes
import com.mbta.tid.mbta_app.android.util.stateJsonSaver
import com.mbta.tid.mbta_app.android.util.toPosition
import com.mbta.tid.mbta_app.history.Visit
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.usecases.VisitHistoryUsecase
import io.github.dellisd.spatialk.geojson.Position
import kotlin.reflect.typeOf
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
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
    var lastNearbyTransitLocation by lastNearbyTransitLocationState
    var nearbyTransitSelectingLocation by nearbyTransitSelectingLocationState
}

@OptIn(ExperimentalMaterial3Api::class, MapboxExperimental::class, FlowPreview::class)
@Composable
fun NearbyTransitPage(
    modifier: Modifier = Modifier,
    nearbyTransit: NearbyTransit,
    navBarVisible: Boolean,
    showNavBar: () -> Unit,
    hideNavBar: () -> Unit,
    bottomBar: @Composable () -> Unit,
    mapViewModel: IMapViewModel = viewModel(factory = MapViewModel.Factory()),
    errorBannerViewModel: ErrorBannerViewModel =
        viewModel(
            factory =
                ErrorBannerViewModel.Factory(
                    errorRepository = koinInject(),
                    settingsRepository = koinInject()
                )
        ),
    visitHistoryUsecase: VisitHistoryUsecase = koinInject()
) {
    LaunchedEffect(Unit) { errorBannerViewModel.activate() }

    val navController = rememberNavController()

    val viewModel: NearbyTransitTabViewModel = viewModel()

    val currentNavEntry: SheetRoutes? =
        viewModel.currentNavEntry.collectAsState(initial = null).value
    val previousNavEntry: SheetRoutes? =
        viewModel.previousNavEntry.collectAsState(initial = null).value

    val (pinnedRoutes) = managePinnedRoutes()

    val stopDetailsVM =
        stopDetailsManagedVM(
            stopId =
                (when (currentNavEntry) {
                    is SheetRoutes.StopDetails -> currentNavEntry.stopId
                    else -> null
                }),
            globalResponse = nearbyTransit.globalResponse,
            alertData = nearbyTransit.alertData,
            pinnedRoutes = pinnedRoutes ?: emptySet()
        )

    val stopDetailsDepartures by viewModel.stopDetailsDepartures.collectAsState()
    val scope = rememberCoroutineScope()

    var vehiclesData: List<Vehicle> =
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
        navController.navigate(SheetRoutes.StopDetails(stopId, null, null)) {
            popUpTo(SheetRoutes.NearbyTransit)
        }
    }

    fun openSearch() {
        searchFocusRequester.requestFocus()
    }

    fun panToDefaultCenter() {
        nearbyTransit.viewportProvider.animateTo(
            ViewportProvider.Companion.Defaults.center,
            zoom = 13.75
        )
    }

    LaunchedEffect(mapViewModel.lastMapboxErrorTimestamp.collectAsState(initial = null).value) {
        mapViewModel.loadConfig()
    }
    LaunchedEffect(nearbyTransit.alertData) { mapViewModel.setAlertsData(nearbyTransit.alertData) }

    LaunchedEffect(nearbyTransit.globalResponse) {
        mapViewModel.setGlobalResponse(nearbyTransit.globalResponse)
    }

    LaunchedEffect(currentNavEntry) {
        if (SheetRoutes.pageChanged(previousNavEntry, currentNavEntry)) {
            nearbyTransit.scaffoldState.bottomSheetState.animateTo(SheetValue.Medium)
        }
    }

    fun updateStopFilter(stopFilter: StopDetailsFilter?) {
        viewModel.setStopFilter(
            currentNavEntry,
            stopFilter,
            { navController.popBackStack() },
            { navController.navigate(it) }
        )
    }

    val navTypeMap =
        mapOf(
            typeOf<StopDetailsFilter?>() to StopFilterParameterType,
            typeOf<TripDetailsFilter?>() to TripFilterParameterType,
        )

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
            startDestination = SheetRoutes.NearbyTransit,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface).then(modifier),
            enterTransition = {
                val initialStopId = this.initialState.arguments?.getString("stopId")
                val targetStopId = this.targetState.arguments?.getString("stopId")

                // Skip animation if navigating within a single stop
                if (
                    initialStopId != null && targetStopId != null && initialStopId == targetStopId
                ) {
                    EnterTransition.None
                } else {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(easing = EaseInOut)
                    )
                }
            }
        ) {
            composable<SheetRoutes.StopDetails>(typeMap = navTypeMap) { backStackEntry ->
                val navRoute: SheetRoutes.StopDetails = backStackEntry.toRoute()

                val filters =
                    StopDetailsPageFilters(
                        navRoute.stopId,
                        navRoute.stopFilter,
                        navRoute.tripFilter
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
                    viewModel.recordCurrentNavEntry(navRoute)
                }

                StopDetailsPage(
                    modifier = modifier,
                    viewModel = stopDetailsVM,
                    filters = filters,
                    alertData = nearbyTransit.alertData,
                    onClose = { navController.popBackStack() },
                    updateStopFilter = ::updateStopFilter,
                    updateDepartures = { viewModel.setStopDetailsDepartures(it) },
                    openAlertDetails = ::openModal,
                    errorBannerViewModel = errorBannerViewModel
                )
            }

            composable<SheetRoutes.NearbyTransit>(typeMap = navTypeMap) {
                // for ViewModel reasons, must be within the `composable` to be the same instance
                val nearbyViewModel: NearbyTransitViewModel = koinViewModel()
                LaunchedEffect(true) {
                    if (!navBarVisible && !searchExpanded) {
                        showNavBar()
                    }
                    analytics.track(AnalyticsScreen.NearbyTransit)
                    viewModel.recordCurrentNavEntry(SheetRoutes.NearbyTransit)
                }

                var targetLocation by remember { mutableStateOf<Position?>(null) }
                LaunchedEffect(nearbyTransit.viewportProvider) {
                    nearbyTransit.viewportProvider.cameraStateFlow.debounce(0.5.seconds).collect {
                        // since this LaunchedEffect is cancelled when not on the nearby transit
                        // page, we don't need to check
                        targetLocation = it.center.toPosition()
                    }
                }
                LaunchedEffect(nearbyTransit.viewportProvider.isManuallyCentering) {
                    if (nearbyTransit.viewportProvider.isManuallyCentering) {
                        nearbyViewModel.reset()
                        targetLocation = null
                    }
                }
                LaunchedEffect(nearbyTransit.viewportProvider.isFollowingPuck) {
                    if (nearbyTransit.viewportProvider.isFollowingPuck) {
                        nearbyViewModel.reset()
                        targetLocation = null
                    }
                }

                NearbyTransitView(
                    alertData = nearbyTransit.alertData,
                    globalResponse = nearbyTransit.globalResponse,
                    targetLocation = targetLocation,
                    setLastLocation = { nearbyTransit.lastNearbyTransitLocation = it },
                    setSelectingLocation = { nearbyTransit.nearbyTransitSelectingLocation = it },
                    onOpenStopDetails = { stopId, filter ->
                        updateVisitHistory(stopId)
                        navController.navigate(SheetRoutes.StopDetails(stopId, filter, null))
                    },
                    noNearbyStopsView = {
                        NoNearbyStopsView(
                            nearbyTransit.hideMaps,
                            ::openSearch,
                            ::panToDefaultCenter
                        )
                    },
                    errorBannerViewModel = errorBannerViewModel
                )
            }
        }
    }

    Scaffold(bottomBar = bottomBar) { outerSheetPadding ->
        if (nearbyTransit.hideMaps) {
            val isNearbyTransit = currentNavEntry?.let { it is SheetRoutes.NearbyTransit } ?: true
            SearchBarOverlay(
                searchExpanded,
                ::handleSearchExpandedChange,
                ::handleStopNavigation,
                currentNavEntry,
                searchFocusRequester
            ) {
                SheetContent(
                    Modifier.padding(top = if (isNearbyTransit) 94.dp else 0.dp).fillMaxSize()
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
                                .background(MaterialTheme.colorScheme.surface)
                    ) {
                        SheetContent(Modifier.height(sheetHeight).padding(outerSheetPadding))
                    }
                },
                sheetContainerColor = MaterialTheme.colorScheme.surface,
                scaffoldState = nearbyTransit.scaffoldState
            ) { sheetPadding ->
                SearchBarOverlay(
                    searchExpanded,
                    ::handleSearchExpandedChange,
                    ::handleStopNavigation,
                    currentNavEntry,
                    searchFocusRequester
                ) {
                    HomeMapView(
                        Modifier.padding(sheetPadding),
                        lastNearbyTransitLocation = nearbyTransit.lastNearbyTransitLocation,
                        nearbyTransitSelectingLocationState =
                            nearbyTransit.nearbyTransitSelectingLocationState,
                        locationDataManager = nearbyTransit.locationDataManager,
                        viewportProvider = nearbyTransit.viewportProvider,
                        currentNavEntry = currentNavEntry,
                        handleStopNavigation = ::handleStopNavigation,
                        vehiclesData = vehiclesData,
                        stopDetailsDepartures = stopDetailsDepartures,
                        viewModel = mapViewModel
                    )
                }
            }
        }
    }
    if (currentModal != null) {
        ModalBottomSheet(
            onDismissRequest = { closeModal() },
            sheetState = modalSheetState,
            dragHandle = null
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
                            goBack = { closeModal() }
                        )
                    null -> {}
                }
            }
        }
    }
}
