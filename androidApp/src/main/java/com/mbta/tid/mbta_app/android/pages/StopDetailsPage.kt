package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.stopDetails.StopDetailsView
import com.mbta.tid.mbta_app.android.stopDetails.StopDetailsViewModel
import com.mbta.tid.mbta_app.android.util.manageFavorites
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.SheetRoutes
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import kotlinx.coroutines.launch

@Composable
fun StopDetailsPage(
    modifier: Modifier = Modifier,
    viewModel: StopDetailsViewModel,
    filters: StopDetailsPageFilters,
    allAlerts: AlertsStreamDataResponse?,
    onClose: () -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    updateTripFilter: (TripDetailsFilter?) -> Unit,
    updateRouteCardData: (List<RouteCardData>?) -> Unit,
    tileScrollState: ScrollState,
    openModal: (ModalRoutes) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    errorBannerViewModel: ErrorBannerViewModel,
) {
    val stopId = filters.stopId

    val coroutineScope = rememberCoroutineScope()

    val (favorites, updateFavorites) = manageFavorites()

    fun isFavorite(routeStopDirection: RouteStopDirection): Boolean {
        return favorites?.contains(routeStopDirection) ?: false
    }

    fun updateFavorites(updatedFavorites: Map<RouteStopDirection, Boolean>, defaultDirection: Int) {
        coroutineScope.launch {
            updateFavorites(updatedFavorites, EditFavoritesContext.StopDetails, defaultDirection)
        }
    }

    val routeCardData by viewModel.routeCardData.collectAsState()

    LaunchedEffect(routeCardData) { updateRouteCardData(routeCardData) }

    StopDetailsView(
        modifier,
        stopId,
        viewModel,
        filters.stopFilter,
        filters.tripFilter,
        allAlerts,
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
