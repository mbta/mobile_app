package com.mbta.tid.mbta_app.android.pages

import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.AnalyticsScreen
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.alertDetails.AlertDetailsPage
import com.mbta.tid.mbta_app.android.component.BarAndToastScaffold
import com.mbta.tid.mbta_app.android.component.DragHandle
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.sheet.BottomSheetScaffold
import com.mbta.tid.mbta_app.android.component.sheet.BottomSheetScaffoldState
import com.mbta.tid.mbta_app.android.component.sheet.SheetValue
import com.mbta.tid.mbta_app.android.fromNavBackStackEntry
import com.mbta.tid.mbta_app.android.location.IViewportProvider
import com.mbta.tid.mbta_app.android.location.LocationDataManager
import com.mbta.tid.mbta_app.android.map.HomeMapView
import com.mbta.tid.mbta_app.android.map.IMapboxConfigManager
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitTabViewModel
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyTransitViewModel
import com.mbta.tid.mbta_app.android.routeDetails.RouteDetailsView
import com.mbta.tid.mbta_app.android.routePicker.RoutePickerView
import com.mbta.tid.mbta_app.android.routePicker.backgroundColor
import com.mbta.tid.mbta_app.android.search.SearchBarOverlay
import com.mbta.tid.mbta_app.android.state.subscribeToVehicles
import com.mbta.tid.mbta_app.android.stopDetails.stopDetailsManagedVM
import com.mbta.tid.mbta_app.android.typeMap
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.currentRouteAs
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.managePinnedRoutes
import com.mbta.tid.mbta_app.android.util.navigateFrom
import com.mbta.tid.mbta_app.android.util.plus
import com.mbta.tid.mbta_app.android.util.popBackStackFrom
import com.mbta.tid.mbta_app.android.util.rememberPrevious
import com.mbta.tid.mbta_app.android.util.selectedStopId
import com.mbta.tid.mbta_app.android.util.stateJsonSaver
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.history.Visit
import com.mbta.tid.mbta_app.model.SheetRoutes
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.usecases.VisitHistoryUsecase
import com.mbta.tid.mbta_app.viewModel.IFavoritesViewModel
import com.mbta.tid.mbta_app.viewModel.IMapViewModel
import io.github.dellisd.spatialk.geojson.Position
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
data class NearbyTransit(
    val alertData: AlertsStreamDataResponse?,
    val globalResponse: GlobalResponse?,
    val lastNearbyTransitLocationState: MutableState<Position?>,
    val nearbyTransitSelectingLocationState: MutableState<Boolean>,
    val scaffoldState: BottomSheetScaffoldState,
    val locationDataManager: LocationDataManager,
    val viewportProvider: IViewportProvider,
) {
    var lastNearbyTransitLocation by this.lastNearbyTransitLocationState
    var nearbyTransitSelectingLocation by this.nearbyTransitSelectingLocationState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapAndSheetPage(
    modifier: Modifier = Modifier,
    nearbyTransit: NearbyTransit,
    sheetNavEntrypoint: SheetRoutes.Entrypoint?,
    navBarVisible: Boolean,
    showNavBar: () -> Unit,
    hideNavBar: () -> Unit,
    bottomBar: @Composable () -> Unit,
    mapViewModel: IMapViewModel = koinInject(),
    errorBannerViewModel: ErrorBannerViewModel =
        viewModel(factory = ErrorBannerViewModel.Factory(errorRepository = koinInject())),
    visitHistoryUsecase: VisitHistoryUsecase = koinInject(),
    clock: Clock = koinInject(),
    favoritesViewModel: IFavoritesViewModel = koinInject(),
    mapboxConfigManager: IMapboxConfigManager = koinInject(),
) {
    LaunchedEffect(Unit) { errorBannerViewModel.activate() }

    val viewModel: NearbyTransitTabViewModel = viewModel()

    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()
    val hideMaps = SettingsCache.get(Settings.HideMaps)

    val currentNavBackStackEntry by
    navController.currentBackStackEntryFlow.collectAsStateWithLifecycle(initialValue = null)

    val currentNavEntry = currentNavBackStackEntry?.let { SheetRoutes.fromNavBackStackEntry(it) }
    val previousNavEntry: SheetRoutes? = rememberPrevious(currentNavEntry)

    val (pinnedRoutes) = managePinnedRoutes()

    val density = LocalDensity.current

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

    val filters =
        (when (currentNavEntry) {
            is SheetRoutes.StopDetails ->
                StopDetailsPageFilters(
                    currentNavEntry.stopId,
                    currentNavEntry.stopFilter,
                    currentNavEntry.tripFilter,
                )

            else -> null
        })
    val stopDetailsVM =
        stopDetailsManagedVM(
            filters = filters,
            globalResponse = nearbyTransit.globalResponse,
            alertData = nearbyTransit.alertData,
            pinnedRoutes = pinnedRoutes ?: emptySet(),
            updateStopFilter = ::updateStopFilter,
            updateTripFilter = ::updateTripFilter,
            setMapSelectedVehicle = { vehicle ->
                val stop = nearbyTransit.globalResponse?.getStop(filters?.stopId)
                filters?.tripFilter?.let {
                    mapViewModel.selectedTrip(filters.stopFilter, stop, it, vehicle)
                }
            },
            now = now,
        )

    val routeCardData by viewModel.routeCardData.collectAsState()
    val tileScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentNavEntry) {
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
                },
            routeCardData = routeCardData,
        )

    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    val analytics: Analytics = koinInject()

    fun updateVisitHistory(stopId: String) {
        CoroutineScope(Dispatchers.Default).launch {
            visitHistoryUsecase.addVisit(Visit.StopVisit(stopId))
        }
    }

    fun handleRouteSearchExpandedChange(expanded: Boolean) {
        if (expanded && !hideMaps) {
            scope.launch {
                nearbyTransit.scaffoldState.bottomSheetState.animateTo(SheetValue.Large)
            }
        }
    }

    fun handleSearchExpandedChange(expanded: Boolean) {
        searchExpanded = expanded
        if (expanded) {
            hideNavBar()
            if (!hideMaps) {
                scope.launch {
                    nearbyTransit.scaffoldState.bottomSheetState.animateTo(SheetValue.Hidden)
                }
            }
        } else {
            showNavBar()
            if (!hideMaps) {
                scope.launch {
                    nearbyTransit.scaffoldState.bottomSheetState.animateTo(SheetValue.Medium)
                }
            }
        }
    }

    fun handleStopNavigation(stopId: String) {
        if (navController.selectedStopId == stopId) return

        updateVisitHistory(stopId)
        navController.navigate(SheetRoutes.StopDetails(stopId, null, null)) {
            popUpTo(SheetRoutes.NearbyTransit)
        }
    }

    fun handleRouteNavigation(
        routeId: String,
        context: RouteDetailsContext = RouteDetailsContext.Details,
    ) {
        navController.navigate(SheetRoutes.RouteDetails(routeId, context)) {
            popUpTo(SheetRoutes.NearbyTransit)
        }
    }

    fun handlePickRouteNavigation(
        routeId: String,
        context: RouteDetailsContext = RouteDetailsContext.Details,
    ) {
        navController.navigateFrom(
            SheetRoutes.RoutePicker::class,
            SheetRoutes.RouteDetails(routeId, context),
        ) {
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

        val newTripFilter = TripDetailsFilter(tripId, vehicle.id, stopSequence, true)

        updateTripFilter(stopId, newTripFilter)
        val stop = nearbyTransit.globalResponse?.getStop(filters?.stopId)

        // We know the exact vehicle, so set that now rather than waiting for the next vehicle
        // data update to set it
        mapViewModel.selectedTrip(stopFilter, stop, newTripFilter, vehicle)
    }

    val popUp: NavOptionsBuilder.() -> Unit = {
        popUpTo(navController.graph.id) { inclusive = true }
    }

    fun navigateToEntrypoint(entrypoint: SheetRoutes.Entrypoint) {
        try {
            navController.navigate(entrypoint, popUp)
        } catch (e: IllegalStateException) {
            // This should only happen in tests when the navigation graph hasn't been initialized
            Log.w("MapAndSheetPage", "Failed to navigate to sheet entrypoint")
        }
    }

    fun <T : SheetRoutes> navigateToEntrypointFrom(routeType: KClass<T>) {
        try {
            if (sheetNavEntrypoint != null) {
                navController.navigateFrom(routeType, sheetNavEntrypoint as SheetRoutes, popUp)
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
            if (timeSinceBackground > 1.hours && sheetNavEntrypoint != null) {
                navigateToEntrypoint(sheetNavEntrypoint)
                if (nearbyTransit.locationDataManager.hasPermission) {
                    coroutineScope.launch { nearbyTransit.viewportProvider.follow() }
                }
            }
        }
        backgroundTimestamp = null
        onPauseOrDispose { backgroundTimestamp = clock.now().toEpochMilliseconds() }
    }

    LaunchedEffect(
        mapboxConfigManager.lastMapboxErrorTimestamp.collectAsState(initial = null).value
    ) {
        mapboxConfigManager.loadConfig()
    }
    LaunchedEffect(nearbyTransit.alertData) { mapViewModel.alertsChanged(nearbyTransit.alertData) }

    LaunchedEffect(sheetNavEntrypoint) {
        if (sheetNavEntrypoint != null) {
            navigateToEntrypoint(sheetNavEntrypoint)
        }
    }
    LaunchedEffect(currentNavEntry) {
        if (
            !SheetRoutes.retainSheetSize(previousNavEntry, currentNavEntry) &&
            SheetRoutes.pageChanged(previousNavEntry, currentNavEntry)
        ) {
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
    fun SheetPage(
        backgroundColor: Color = colorResource(R.color.sheet_background),
        body: @Composable () -> Unit,
    ) {
        val sheetState = nearbyTransit.scaffoldState.bottomSheetState.currentValue
        Column(
            Modifier.background(backgroundColor)
                .padding(top = 12.dp)
                .fillMaxSize()
                .then(
                    if (sheetState != SheetValue.Large) Modifier else Modifier.statusBarsPadding()
                )
        ) {
            body()
        }
    }

    @Composable
    fun SheetContent(modifier: Modifier = Modifier) {
        if (sheetNavEntrypoint == null) {
            CircularProgressIndicator()
        } else {
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
                    LaunchedEffect(Unit) { analytics.track(AnalyticsScreen.Favorites) }

                    fun navigate(route: SheetRoutes) {
                        navController.navigateFrom(SheetRoutes.Favorites::class, route)
                    }

                    SheetPage {
                        FavoritesPage(
                            openSheetRoute = ::navigate,
                            favoritesViewModel = favoritesViewModel,
                            errorBannerViewModel = errorBannerViewModel,
                            nearbyTransit = nearbyTransit,
                        )
                    }
                }
                composable<SheetRoutes.EditFavorites>(typeMap = SheetRoutes.typeMap) {
                    SheetPage {
                        EditFavoritesPage(
                            global = nearbyTransit.globalResponse,
                            favoritesViewModel = favoritesViewModel,
                            onClose = {
                                navController.popBackStackFrom(SheetRoutes.EditFavorites::class)
                            },
                        )
                    }
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

                    SheetPage(colorResource(R.color.fill2)) {
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
                }

                composable<SheetRoutes.RoutePicker>(typeMap = SheetRoutes.typeMap) { backStackEntry ->
                    val navRoute: SheetRoutes.RoutePicker = backStackEntry.toRoute()

                    LaunchedEffect(Unit) {
                        if (navBarVisible) hideNavBar()
                        analytics.track(AnalyticsScreen.RoutePicker)
                    }

                    SheetPage(navRoute.path.backgroundColor) {
                        RoutePickerView(
                            navRoute.path,
                            navRoute.context,
                            onOpenPickerPath = { newPath, context ->
                                val currentPickerRoute =
                                    navController.currentRouteAs(SheetRoutes.RoutePicker::class)
                                if (currentPickerRoute == null || currentPickerRoute.path != newPath) {
                                    navController.navigate(
                                        SheetRoutes.RoutePicker(
                                            newPath,
                                            context
                                        )
                                    )
                                }
                            },
                            onOpenRouteDetails = ::handlePickRouteNavigation,
                            onRouteSearchExpandedChange = ::handleRouteSearchExpandedChange,
                            onBack = onBack@{
                                val currentPickerRoute =
                                    navController.currentRouteAs(SheetRoutes.RoutePicker::class)
                                        ?: return@onBack
                                if (currentPickerRoute.path != RoutePickerPath.Root) {
                                    navController.popBackStackFrom(SheetRoutes.RoutePicker::class)
                                }
                            },
                            onClose = { navigateToEntrypointFrom(SheetRoutes.RoutePicker::class) },
                            errorBannerViewModel = errorBannerViewModel,
                        )
                    }
                }

                composable<SheetRoutes.RouteDetails>(typeMap = SheetRoutes.typeMap) { backStackEntry ->
                    val navRoute: SheetRoutes.RouteDetails = backStackEntry.toRoute()

                    val lineOrRoute = nearbyTransit.globalResponse?.getLineOrRoute(navRoute.routeId)
                    LaunchedEffect(Unit) {
                        if (navBarVisible) hideNavBar()
                        analytics.track(AnalyticsScreen.RouteDetails)
                    }

                    SheetPage(
                        lineOrRoute?.backgroundColor?.let { Color.fromHex(it) }
                            ?: colorResource(R.color.sheet_background)
                    ) {
                        RouteDetailsView(
                            selectionId = navRoute.routeId,
                            context = navRoute.context,
                            onOpenStopDetails = ::handleStopNavigation,
                            onBack = {
                                navController.popBackStackFrom(SheetRoutes.RouteDetails::class)
                            },
                            onClose = { navigateToEntrypointFrom(SheetRoutes.RouteDetails::class) },
                            errorBannerViewModel = errorBannerViewModel,
                        )
                    }
                }

                composable<SheetRoutes.NearbyTransit>(typeMap = SheetRoutes.typeMap) {
                    // for ViewModel reasons, must be within the `composable` to be the same instance
                    val nearbyViewModel: NearbyTransitViewModel = koinViewModel()
                    LaunchedEffect(Unit) {
                        if (!navBarVisible && !searchExpanded) showNavBar()
                        analytics.track(AnalyticsScreen.NearbyTransit)
                    }

                    SheetPage {
                        NearbyTransitPage(
                            nearbyTransit,
                            onOpenStopDetails = { stopId, filter ->
                                updateVisitHistory(stopId)
                                navController.navigate(
                                    SheetRoutes.StopDetails(
                                        stopId,
                                        filter,
                                        null
                                    )
                                )
                            },
                            openSearch = ::openSearch,
                            nearbyViewModel = nearbyViewModel,
                            errorBannerViewModel = errorBannerViewModel,
                        )
                    }
                }
            }
        }

        // setting WindowInsets top to 0 to prevent the sheet from having extra padding on top even
        // when not fully expanded https://stackoverflow.com/a/77361483
        BarAndToastScaffold(
            bottomBar = bottomBar,
            contentWindowInsets = WindowInsets(top = 0.dp)
        ) { outerSheetPadding ->
            val showSearchBar = remember(currentNavEntry) { currentNavEntry?.showSearchBar ?: true }
            if (hideMaps) {
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
                    ::handleRouteNavigation,
                    searchFocusRequester,
                    onBarGloballyPositioned = {},
                ) {
                    SheetContent(
                        Modifier.background(colorResource(R.color.sheet_background))
                            .padding(top = if (showSearchBar) 64.dp else 0.dp)
                            .statusBarsPadding()
                            .fillMaxSize()
                    )
                }
            } else {
                BottomSheetScaffold(
                    sheetDragHandle = { DragHandle() },
                    sheetContent = {
                        Box(Modifier.fillMaxSize()) {
                            SheetContent(Modifier.padding(outerSheetPadding))
                        }
                    },
                    sheetContainerColor = colorResource(R.color.sheet_background),
                    scaffoldState = nearbyTransit.scaffoldState,
                ) { sheetPadding ->
                    SearchBarOverlay(
                        searchExpanded,
                        showSearchBar,
                        ::handleSearchExpandedChange,
                        ::handleStopNavigation,
                        ::handleRouteNavigation,
                        searchFocusRequester,
                        onBarGloballyPositioned = { layoutCoordinates ->
                            with(density) {
                                viewModel.setSearchBarHeight(layoutCoordinates.size.height.toDp())
                            }
                        },
                    ) {
                        HomeMapView(
                            sheetPadding =
                                sheetPadding.plus(
                                    PaddingValues(
                                        start = 0.dp,
                                        end = 0.dp,
                                        top = (viewModel.searchBarHeight.value ?: 0.dp),
                                        bottom = 0.dp,
                                    )
                                ),
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
                            mapboxConfigManager = mapboxConfigManager,
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
}