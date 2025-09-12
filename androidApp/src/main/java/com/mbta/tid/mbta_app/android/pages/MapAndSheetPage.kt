package com.mbta.tid.mbta_app.android.pages

import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
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
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.LocationAuthButton
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.component.routeCard.RouteCardList
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
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.state.subscribeToVehicles
import com.mbta.tid.mbta_app.android.typeMap
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.currentRouteAs
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.navigateFrom
import com.mbta.tid.mbta_app.android.util.plus
import com.mbta.tid.mbta_app.android.util.popBackStackFrom
import com.mbta.tid.mbta_app.android.util.rememberPrevious
import com.mbta.tid.mbta_app.android.util.selectedStopId
import com.mbta.tid.mbta_app.android.util.stateJsonSaver
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.history.Visit
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.usecases.VisitHistoryUsecase
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel
import com.mbta.tid.mbta_app.viewModel.IFavoritesViewModel
import com.mbta.tid.mbta_app.viewModel.IMapViewModel
import com.mbta.tid.mbta_app.viewModel.IRouteCardDataViewModel
import com.mbta.tid.mbta_app.viewModel.IStopDetailsViewModel
import com.mbta.tid.mbta_app.viewModel.ITripDetailsViewModel
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
    val lastLoadedLocationState: MutableState<Position?>,
    val isTargetingState: MutableState<Boolean>,
    val scaffoldState: BottomSheetScaffoldState,
    val locationDataManager: LocationDataManager,
    val viewportProvider: IViewportProvider,
) {
    var lastLoadedLocation by this.lastLoadedLocationState
    var isTargeting by this.isTargetingState
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
    errorBannerViewModel: IErrorBannerViewModel = koinInject(),
    visitHistoryUsecase: VisitHistoryUsecase = koinInject(),
    clock: Clock = koinInject(),
    favoritesViewModel: IFavoritesViewModel = koinInject(),
    routeCardDataViewModel: IRouteCardDataViewModel = koinInject(),
    stopDetailsViewModel: IStopDetailsViewModel = koinInject(),
    tripDetailsViewModel: ITripDetailsViewModel = koinInject(),
    mapboxConfigManager: IMapboxConfigManager = koinInject(),
) {
    val nearbyTabViewModel: NearbyTransitTabViewModel = viewModel()

    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()
    val hideMaps = SettingsCache.get(Settings.HideMaps)

    val currentNavBackStackEntry by
        navController.currentBackStackEntryFlow.collectAsStateWithLifecycle(initialValue = null)

    val currentNavEntry = currentNavBackStackEntry?.let { SheetRoutes.fromNavBackStackEntry(it) }
    val previousNavEntry: SheetRoutes? = rememberPrevious(currentNavEntry)

    val density = LocalDensity.current

    val now by timer(updateInterval = 5.seconds)

    fun updateStopDetailsFilters(filters: StopDetailsPageFilters) {
        nearbyTabViewModel.setStopDetailsFilters(
            currentNavEntry,
            filters,
            { navController.popBackStack() },
            { navController.navigate(it) },
        )
    }

    fun updateStopFilter(stopId: String, stopFilter: StopDetailsFilter?) {
        nearbyTabViewModel.setStopFilter(
            currentNavEntry,
            stopId,
            stopFilter,
            { navController.popBackStack() },
            { navController.navigate(it) },
        )
    }

    fun updateTripFilter(stopId: String, tripFilter: TripDetailsFilter?) {
        nearbyTabViewModel.setTripFilter(
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

    val selectedVehicleUpdate by tripDetailsViewModel.selectedVehicleUpdates.collectAsState()
    LaunchedEffect(selectedVehicleUpdate) {
        val stop = nearbyTransit.globalResponse?.getStop(filters?.stopId)
        filters?.tripFilter?.let {
            mapViewModel.selectedTrip(filters.stopFilter, stop, it, selectedVehicleUpdate)
        }
    }

    val routeCardDataState by routeCardDataViewModel.models.collectAsState()
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

    val filterUpdates by stopDetailsViewModel.filterUpdates.collectAsState()
    LaunchedEffect(filterUpdates) { filterUpdates?.let { updateStopDetailsFilters(it) } }

    val vehiclesData: List<Vehicle> =
        subscribeToVehicles(
            routeDirection =
                when (currentNavEntry) {
                    is SheetRoutes.StopDetails -> currentNavEntry.stopFilter
                    is SheetRoutes.TripDetails -> currentNavEntry.filter.stopFilter
                    else -> null
                }
        )

    LaunchedEffect(vehiclesData) { mapViewModel.vehiclesChanged(vehiclesData) }

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

    fun handleTripDetailsNavigation(
        stopId: String,
        tripDetailsFilter: TripDetailsFilter,
        stopDetailsFilter: StopDetailsFilter,
    ) {
        val filter = TripDetailsPageFilter(stopId, stopDetailsFilter, tripDetailsFilter)
        navController.navigate(SheetRoutes.TripDetails(filter))
    }

    fun handleVehicleTap(vehicle: Vehicle, hasTrackThisTrip: Boolean) {
        val tripId = vehicle.tripId ?: return
        val routeCardData = routeCardDataState.data
        val (stopId, stopFilter, tripFilter) =
            when (currentNavEntry) {
                is SheetRoutes.StopDetails ->
                    Triple(
                        currentNavEntry.stopId,
                        currentNavEntry.stopFilter,
                        currentNavEntry.tripFilter,
                    )
                is SheetRoutes.TripDetails ->
                    Triple(
                        currentNavEntry.filter.stopId,
                        currentNavEntry.filter.stopFilter,
                        currentNavEntry.filter.tripDetailsFilter,
                    )
                else -> null
            } ?: return
        if (stopFilter == null || tripFilter?.tripId == tripId) return
        val routeCard = routeCardData?.find { it.lineOrRoute.containsRoute(vehicle.routeId) }

        val upcoming =
            routeCard
                ?.stopData
                ?.flatMap { it.data }
                ?.flatMap { it.upcomingTrips }
                ?.firstOrNull { upcoming -> upcoming.trip.id == tripId }
        val stopSequence = upcoming?.stopSequence
        val routeId = upcoming?.trip?.routeId ?: vehicle.routeId ?: routeCard?.lineOrRoute?.id

        if (routeId != null) analytics.tappedVehicle(routeId)

        val newTripFilter = TripDetailsFilter(tripId, vehicle.id, stopSequence)
        val stop = nearbyTransit.globalResponse?.getStop(filters?.stopId)
        if (hasTrackThisTrip) {
            handleTripDetailsNavigation(
                stopId,
                newTripFilter.copy(selectionLock = true),
                stopFilter.copy(routeId = vehicle.routeId ?: stopFilter.routeId),
            )
        } else {
            updateTripFilter(stopId, newTripFilter)
            mapViewModel.selectedTrip(stopFilter, stop, newTripFilter, vehicle)
        }
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
        if (currentNavEntry?.showSearchBar == true && searchExpanded) {
            nearbyTransit.scaffoldState.bottomSheetState.animateTo(SheetValue.Hidden)
        } else if (SheetRoutes.shouldResetSheetHeight(previousNavEntry, currentNavEntry)) {
            nearbyTransit.scaffoldState.bottomSheetState.animateTo(SheetValue.Medium)
        }

        if (currentNavEntry is SheetRoutes.Entrypoint) {
            showNavBar()
        } else if (currentNavEntry != null) {
            hideNavBar()
        }
    }

    LaunchedEffect(currentNavEntry) { errorBannerViewModel.setSheetRoute(currentNavEntry) }

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
    fun LoadingSheetContents() {
        CompositionLocalProvider(IsLoadingSheetContents provides true) {
            SheetPage {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SheetHeader(title = stringResource(R.string.nearby_transit))
                    ErrorBanner(errorBannerViewModel)
                    RouteCardList(
                        routeCardData = null,
                        emptyView = {},
                        global = null,
                        now = now,
                        isFavorite = { false },
                        onOpenStopDetails = { _, _ -> },
                    )
                }
            }
        }
    }

    @Composable
    fun FavoritesSheetContents() {
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

    @Composable
    fun EditFavoritesSheetContents() {
        SheetPage {
            EditFavoritesPage(
                global = nearbyTransit.globalResponse,
                favoritesViewModel = favoritesViewModel,
                onClose = { navController.popBackStackFrom(SheetRoutes.EditFavorites::class) },
            )
        }
    }

    @Composable
    fun StopDetailsSheetContents(backStackEntry: NavBackStackEntry) {
        val navRoute: SheetRoutes.StopDetails = backStackEntry.toRoute()
        val filters =
            StopDetailsPageFilters(navRoute.stopId, navRoute.stopFilter, navRoute.tripFilter)

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
                filters = filters,
                allAlerts = nearbyTransit.alertData,
                onClose = { navController.popBackStack() },
                updateStopFilter = { updateStopFilter(navRoute.stopId, it) },
                updateTripFilter = { updateTripFilter(navRoute.stopId, it) },
                tileScrollState = tileScrollState,
                openModal = ::openModal,
                openSheetRoute = navController::navigate,
                errorBannerViewModel = errorBannerViewModel,
            )
        }
    }

    @Composable
    fun TripDetailsSheetContents(backStackEntry: NavBackStackEntry) {
        val navRoute: SheetRoutes.TripDetails = backStackEntry.toRoute()
        LaunchedEffect(navRoute) {
            if (navBarVisible) {
                hideNavBar()
            }
            analytics.track(AnalyticsScreen.TripDetails)
        }

        val global = getGlobalData("TripDetailsSheetContents")
        val route = global?.getRoute(navRoute.filter.routeId)
        val routeColor = route?.color?.let { Color.fromHex(it) }

        SheetPage(routeColor ?: colorResource(R.color.fill2)) {
            TripDetailsPage(
                filter = navRoute.filter,
                allAlerts = nearbyTransit.alertData,
                openModal = ::openModal,
                openSheetRoute = navController::navigate,
                onClose = { navController.popBackStack() },
            )
        }
    }

    @Composable
    fun RoutePickerSheetContents(backStackEntry: NavBackStackEntry) {
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
                        navController.navigate(SheetRoutes.RoutePicker(newPath, context))
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

    @Composable
    fun RouteDetailsSheetContents(backStackEntry: NavBackStackEntry) {
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
                onBack = { navController.popBackStackFrom(SheetRoutes.RouteDetails::class) },
                onClose = { navigateToEntrypointFrom(SheetRoutes.RouteDetails::class) },
                errorBannerViewModel = errorBannerViewModel,
            )
        }
    }

    @Composable
    fun NearbyTransitSheetContents() {
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
                    navController.navigate(SheetRoutes.StopDetails(stopId, filter, null))
                },
                openSearch = ::openSearch,
                nearbyViewModel = nearbyViewModel,
                errorBannerViewModel = errorBannerViewModel,
            )
        }
    }

    @Composable
    fun NavHost(sheetNavEntrypoint: SheetRoutes.Entrypoint, modifier: Modifier) {
        NavHost(
            navController,
            startDestination = sheetNavEntrypoint,
            modifier = modifier,
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
                } else if (initialRoute == targetRoute) {
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
                FavoritesSheetContents()
            }
            composable<SheetRoutes.EditFavorites>(typeMap = SheetRoutes.typeMap) {
                EditFavoritesSheetContents()
            }
            composable<SheetRoutes.StopDetails>(typeMap = SheetRoutes.typeMap) { backStackEntry ->
                StopDetailsSheetContents(backStackEntry)
            }
            composable<SheetRoutes.TripDetails>(typeMap = SheetRoutes.typeMap) { backStackEntry ->
                TripDetailsSheetContents(backStackEntry)
            }

            composable<SheetRoutes.RoutePicker>(typeMap = SheetRoutes.typeMap) { backStackEntry ->
                RoutePickerSheetContents(backStackEntry)
            }

            composable<SheetRoutes.RouteDetails>(typeMap = SheetRoutes.typeMap) { backStackEntry ->
                RouteDetailsSheetContents(backStackEntry)
            }

            composable<SheetRoutes.NearbyTransit>(typeMap = SheetRoutes.typeMap) {
                NearbyTransitSheetContents()
            }
        }
    }

    @Composable
    fun SheetContent(modifier: Modifier = Modifier) {
        if (sheetNavEntrypoint == null) {
            LoadingSheetContents()
        } else {
            NavHost(sheetNavEntrypoint, modifier)
        }
    }

    // setting WindowInsets top to 0 to prevent the sheet from having extra padding on top even
    // when not fully expanded https://stackoverflow.com/a/77361483
    BarAndToastScaffold(bottomBar = bottomBar, contentWindowInsets = WindowInsets(top = 0.dp)) {
        outerSheetPadding ->
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
                Column(modifier = Modifier.background(colorResource(R.color.sheet_background))) {
                    val shouldShowAuthButton =
                        !nearbyTransit.locationDataManager.hasPermission &&
                            (currentNavEntry?.allowTargeting == true)
                    if (shouldShowAuthButton) {
                        LocationAuthButton(
                            nearbyTransit.locationDataManager,
                            modifier =
                                Modifier.align(Alignment.CenterHorizontally)
                                    .padding(top = 80.dp)
                                    .statusBarsPadding(),
                        )
                    }
                    SheetContent(
                        Modifier.background(colorResource(R.color.sheet_background))
                            .padding(outerSheetPadding)
                            .padding(
                                top = if (showSearchBar && !shouldShowAuthButton) 80.dp else 0.dp
                            )
                            .then(
                                if (showSearchBar) {
                                    Modifier
                                } else {
                                    Modifier.statusBarsPadding()
                                }
                            )
                            .fillMaxSize()
                    )
                }
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
                            nearbyTabViewModel.setSearchBarHeight(
                                layoutCoordinates.size.height.toDp()
                            )
                        }
                    },
                ) {
                    val searchBarHeight by nearbyTabViewModel.searchBarHeight.collectAsState()
                    val mapPadding =
                        remember(sheetPadding, searchBarHeight) {
                            sheetPadding.plus(
                                PaddingValues(
                                    start = 0.dp,
                                    end = 0.dp,
                                    top = (searchBarHeight ?: 0.dp),
                                    bottom = 0.dp,
                                )
                            )
                        }
                    val hasTrackThisTrip = SettingsCache.get(Settings.TrackThisTrip)
                    HomeMapView(
                        sheetPadding = mapPadding,
                        lastLoadedLocation = nearbyTransit.lastLoadedLocation,
                        isTargetingState = nearbyTransit.isTargetingState,
                        locationDataManager = nearbyTransit.locationDataManager,
                        viewportProvider = nearbyTransit.viewportProvider,
                        currentNavEntry = currentNavEntry,
                        handleStopNavigation = ::handleStopNavigation,
                        handleVehicleTap = { handleVehicleTap(it, hasTrackThisTrip) },
                        vehiclesData = vehiclesData,
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
