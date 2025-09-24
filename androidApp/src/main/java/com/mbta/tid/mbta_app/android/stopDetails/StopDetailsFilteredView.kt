package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel
import com.mbta.tid.mbta_app.viewModel.IStopDetailsViewModel
import com.mbta.tid.mbta_app.viewModel.StopDetailsViewModel
import org.koin.compose.koinInject

@Composable
fun StopDetailsFilteredView(
    stopId: String,
    stopFilter: StopDetailsFilter,
    tripFilter: TripDetailsFilter?,
    allAlerts: AlertsStreamDataResponse?,
    now: EasternTimeInstant,
    isFavorite: (RouteStopDirection) -> Boolean?,
    updateFavorites: (Map<RouteStopDirection, FavoriteSettings?>, Int) -> Unit,
    onClose: () -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    updateTripFilter: (TripDetailsFilter?) -> Unit,
    tileScrollState: ScrollState,
    openModal: (ModalRoutes) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    errorBannerViewModel: IErrorBannerViewModel,
    stopDetailsViewModel: IStopDetailsViewModel = koinInject(),
) {
    val state by stopDetailsViewModel.models.collectAsState()
    val routeStopData =
        when (val data = state.routeData) {
            is StopDetailsViewModel.RouteData.Filtered -> data.stopData
            else -> null
        }

    routeStopData?.let {
        StopDetailsFilteredPickerView(
            stopId = stopId,
            stopFilter = stopFilter,
            tripFilter = tripFilter,
            routeStopData = it,
            allAlerts = allAlerts,
            now = now,
            errorBannerViewModel = errorBannerViewModel,
            updateStopFilter = updateStopFilter,
            updateTripFilter = updateTripFilter,
            tileScrollState = tileScrollState,
            isFavorite = isFavorite,
            updateFavorites = updateFavorites,
            openModal = openModal,
            openSheetRoute = openSheetRoute,
            onClose = onClose,
        )
    } ?: Loading(stopId, stopFilter, tripFilter, now, onClose, errorBannerViewModel)
}

@Composable
private fun Loading(
    stopId: String,
    stopFilter: StopDetailsFilter,
    tripFilter: TripDetailsFilter?,
    now: EasternTimeInstant,
    onClose: () -> Unit,
    errorBannerViewModel: IErrorBannerViewModel,
) {
    CompositionLocalProvider(IsLoadingSheetContents provides true) {
        Column(modifier = Modifier.loadingShimmer()) {
            val routeData =
                LoadingPlaceholders.routeCardData(
                    stopFilter.routeId,
                    trips = 10,
                    RouteCardData.Context.StopDetailsFiltered,
                    now,
                )
            val stopData = routeData.stopData.single()
            StopDetailsFilteredPickerView(
                stopId = stopId,
                stopFilter = stopFilter,
                tripFilter = tripFilter,
                routeStopData = stopData,
                allAlerts = null,
                now = now,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                isFavorite = { _ -> false },
                updateFavorites = { _, _ -> },
                openModal = {},
                openSheetRoute = {},
                onClose = onClose,
            )
        }
    }
}
