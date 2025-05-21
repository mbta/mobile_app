package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.stopDetails.StopDetailsView
import com.mbta.tid.mbta_app.android.stopDetails.StopDetailsViewModel
import com.mbta.tid.mbta_app.android.util.managePinnedRoutes
import com.mbta.tid.mbta_app.model.FavoriteBridge
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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

    val analytics: Analytics = koinInject()
    val coroutineScope = rememberCoroutineScope()

    val (pinnedRoutes, rawTogglePinnedRoute) = managePinnedRoutes()

    fun isFavorite(favorite: FavoriteBridge): Boolean {
        // TODO: enhanced favorites support
        if (favorite is FavoriteBridge.Pinned) {
            return pinnedRoutes?.contains(favorite.routeId) ?: false
        }
        return false
    }

    fun toggleFavorite(favorite: FavoriteBridge) {
        coroutineScope.launch {
            when (favorite) {
                // TODO: enhanced favorites support
                is FavoriteBridge.Pinned -> {
                    val pinned = rawTogglePinnedRoute(favorite.routeId)
                    analytics.toggledPinnedRoute(pinned, favorite.routeId)
                }
                else -> {}
            }
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
        ::toggleFavorite,
        onClose,
        updateStopFilter,
        updateTripFilter,
        tileScrollState,
        openModal,
        openSheetRoute,
        errorBannerViewModel,
    )
}
