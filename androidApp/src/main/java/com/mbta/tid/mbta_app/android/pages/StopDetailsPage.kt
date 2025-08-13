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
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.stopDetails.StopDetailsView
import com.mbta.tid.mbta_app.android.stopDetails.StopDetailsViewModel
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.manageFavorites
import com.mbta.tid.mbta_app.model.FavoriteBridge
import com.mbta.tid.mbta_app.model.FavoriteUpdateBridge
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
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

    val enhancedFavorites = SettingsCache.get(Settings.EnhancedFavorites)
    val (favorites, updateFavorites) = manageFavorites()

    fun isFavorite(favorite: FavoriteBridge): Boolean {
        if (favorite is FavoriteBridge.Pinned && !enhancedFavorites) {
            return false
        }

        if (favorite is FavoriteBridge.Favorite && enhancedFavorites) {
            return favorites?.contains(favorite.routeStopDirection) ?: false
        }

        return false
    }

    fun updateFavorites(favoritesUpdate: FavoriteUpdateBridge) {
        coroutineScope.launch {
            if (favoritesUpdate is FavoriteUpdateBridge.Pinned && !enhancedFavorites) {
                val pinned = false
                analytics.toggledPinnedRoute(pinned, favoritesUpdate.routeId)
            }

            if (favoritesUpdate is FavoriteUpdateBridge.Favorites && enhancedFavorites) {
                updateFavorites(
                    favoritesUpdate.updatedValues,
                    EditFavoritesContext.StopDetails,
                    favoritesUpdate.defaultDirection,
                )
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
