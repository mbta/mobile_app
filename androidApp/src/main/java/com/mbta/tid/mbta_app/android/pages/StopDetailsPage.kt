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
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel
import com.mbta.tid.mbta_app.viewModel.IStopDetailsViewModel
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun StopDetailsPage(
    modifier: Modifier = Modifier,
    filters: StopDetailsPageFilters,
    allAlerts: AlertsStreamDataResponse?,
    onClose: () -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    updateTripFilter: (TripDetailsFilter?) -> Unit,
    tileScrollState: ScrollState,
    openModal: (ModalRoutes) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    errorBannerViewModel: IErrorBannerViewModel,
    stopDetailsViewModel: IStopDetailsViewModel = koinInject(),
) {
    val now by timer(updateInterval = 5.seconds)

    val stopId = filters.stopId

    val coroutineScope = rememberCoroutineScope()

    val (favorites, updateFavorites) = manageFavorites()

    val awaitingPredictionsAfterBackground =
        stopDetailsViewModel.models.collectAsState().value.awaitingPredictionsAfterBackground

    fun isFavorite(routeStopDirection: RouteStopDirection): Boolean? {
        return favorites?.contains(routeStopDirection)
    }

    fun updateFavorites(updatedFavorites: Map<RouteStopDirection, Boolean>, defaultDirection: Int) {
        coroutineScope.launch {
            updateFavorites(updatedFavorites, EditFavoritesContext.StopDetails, defaultDirection)
        }
    }

    LaunchedEffect(allAlerts) { stopDetailsViewModel.setAlerts(allAlerts) }
    LaunchedEffect(filters) { stopDetailsViewModel.setFilters(filters) }
    LaunchedEffect(now) { stopDetailsViewModel.setNow(now) }

    LaunchedEffect(Unit) {
        stopDetailsViewModel.setActive(active = true, wasSentToBackground = false)
    }

    LifecycleResumeEffect(Unit) {
        stopDetailsViewModel.setActive(active = true, wasSentToBackground = false)
        onPauseOrDispose {
            stopDetailsViewModel.setActive(active = false, wasSentToBackground = true)
        }
    }

    LaunchedEffect(awaitingPredictionsAfterBackground) {
        errorBannerViewModel.setIsLoadingWhenPredictionsStale(awaitingPredictionsAfterBackground)
    }

    StopDetailsView(
        modifier,
        stopId,
        filters.stopFilter,
        filters.tripFilter,
        allAlerts,
        now,
        ::isFavorite,
        ::updateFavorites,
        onClose,
        updateStopFilter,
        updateTripFilter,
        tileScrollState,
        openModal,
        openSheetRoute,
        errorBannerViewModel,
    )
}
