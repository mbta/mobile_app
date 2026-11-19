package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.stopDetails.StopDetailsView
import com.mbta.tid.mbta_app.android.util.manageFavorites
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.RouteStopFilter
import com.mbta.tid.mbta_app.model.RouteStopPageFilters
import com.mbta.tid.mbta_app.model.StopCentricPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.utils.NavigationCallbacks
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel
import com.mbta.tid.mbta_app.viewModel.IStopCentricViewModel
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun StopDetailsPage(
    modifier: Modifier = Modifier,
    filters: StopCentricPageFilters,
    allAlerts: AlertsStreamDataResponse?,
    navCallbacks: NavigationCallbacks,
    updateRouteStopFilter: (RouteStopFilter?) -> Unit,
    updateTripFilter: (TripDetailsFilter?) -> Unit,
    tileScrollState: ScrollState,
    openModal: (ModalRoutes) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    errorBannerViewModel: IErrorBannerViewModel,
    stopCentricViewModel: IStopCentricViewModel = koinInject(),
) {
    val now by timer(updateInterval = 5.seconds)

    val stopId = filters.stopId

    val coroutineScope = rememberCoroutineScope()

    val (favorites, updateFavorites) = manageFavorites()

    val awaitingPredictionsAfterBackground =
        stopCentricViewModel.models.collectAsState().value.awaitingPredictionsAfterBackground

    fun isFavorite(routeStopDirection: RouteStopDirection): Boolean? {
        return favorites?.contains(routeStopDirection)
    }

    fun updateFavorites(
        updatedFavorites: Map<RouteStopDirection, FavoriteSettings?>,
        defaultDirection: Int,
    ) {
        coroutineScope.launch {
            updateFavorites(updatedFavorites, EditFavoritesContext.RouteStop, defaultDirection)
        }
    }

    LaunchedEffect(allAlerts) { stopCentricViewModel.setAlerts(allAlerts) }
    LaunchedEffect(filters) { stopCentricViewModel.setFilters(filters) }
    LaunchedEffect(now) { stopCentricViewModel.setNow(now) }

    LaunchedEffect(Unit) {
        stopCentricViewModel.setActive(active = true, wasSentToBackground = false)
    }

    LifecycleResumeEffect(Unit) {
        stopCentricViewModel.setActive(active = true, wasSentToBackground = false)
        onPauseOrDispose {
            stopCentricViewModel.setActive(active = false, wasSentToBackground = true)
        }
    }

    LaunchedEffect(awaitingPredictionsAfterBackground) {
        errorBannerViewModel.setIsLoadingWhenPredictionsStale(awaitingPredictionsAfterBackground)
    }

    StopDetailsView(
        modifier,
        stopId,
        (filters as? RouteStopPageFilters)?.routeStopFilter,
        (filters as? RouteStopPageFilters)?.tripFilter,
        allAlerts,
        now,
        ::isFavorite,
        navCallbacks,
        updateRouteStopFilter,
        updateTripFilter,
        tileScrollState,
        openModal,
        openSheetRoute,
        errorBannerViewModel,
    )
}
