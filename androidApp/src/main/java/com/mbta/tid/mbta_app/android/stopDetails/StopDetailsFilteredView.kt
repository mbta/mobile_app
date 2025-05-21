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
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.datetime.Instant

@Composable
fun StopDetailsFilteredView(
    stopId: String,
    stopFilter: StopDetailsFilter,
    tripFilter: TripDetailsFilter?,
    allAlerts: AlertsStreamDataResponse?,
    now: Instant,
    viewModel: StopDetailsViewModel,
    pinnedRoutes: Set<String>,
    togglePinnedRoute: (String) -> Unit,
    onClose: () -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    updateTripFilter: (TripDetailsFilter?) -> Unit,
    tileScrollState: ScrollState,
    openModal: (ModalRoutes) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    errorBannerViewModel: ErrorBannerViewModel,
) {
    val globalResponse by viewModel.globalResponse.collectAsState()
    val routeStopData by viewModel.filteredRouteStopData.collectAsState()

    routeStopData?.let {
        StopDetailsFilteredPickerView(
            stopId = stopId,
            stopFilter = stopFilter,
            tripFilter = tripFilter,
            routeStopData = it,
            allAlerts = allAlerts,
            global = globalResponse,
            now = now,
            viewModel = viewModel,
            errorBannerViewModel = errorBannerViewModel,
            updateStopFilter = updateStopFilter,
            updateTripFilter = updateTripFilter,
            tileScrollState = tileScrollState,
            pinnedRoutes = pinnedRoutes,
            togglePinnedRoute = togglePinnedRoute,
            onClose = onClose,
            openModal = openModal,
            openSheetRoute = openSheetRoute,
        )
    }
        ?: Loading(
            stopId,
            stopFilter,
            tripFilter,
            now,
            viewModel,
            onClose,
            errorBannerViewModel,
            globalResponse,
        )
}

@Composable
private fun Loading(
    stopId: String,
    stopFilter: StopDetailsFilter,
    tripFilter: TripDetailsFilter?,
    now: Instant,
    viewModel: StopDetailsViewModel,
    onClose: () -> Unit,
    errorBannerViewModel: ErrorBannerViewModel,
    globalResponse: GlobalResponse?,
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
                global = globalResponse,
                now = now,
                viewModel = viewModel,
                errorBannerViewModel = errorBannerViewModel,
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                pinnedRoutes = emptySet(),
                togglePinnedRoute = {},
                onClose = onClose,
                openModal = {},
                openSheetRoute = {},
            )
        }
    }
}
