package com.mbta.tid.mbta_app.android.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.mapbox.maps.MapboxExperimental
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.stopDetails.StopDetailsView
import com.mbta.tid.mbta_app.android.stopDetails.StopDetailsViewModel
import com.mbta.tid.mbta_app.android.util.managePinnedRoutes
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
@MapboxExperimental
fun StopDetailsPage(
    modifier: Modifier = Modifier,
    viewModel: StopDetailsViewModel,
    filters: StopDetailsPageFilters,
    onClose: () -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    updateTripFilter: (TripDetailsFilter?) -> Unit,
    updateDepartures: (StopDetailsDepartures?) -> Unit,
    openAlertDetails: (ModalRoutes.AlertDetails) -> Unit,
    errorBannerViewModel: ErrorBannerViewModel
) {
    val stopId = filters.stopId

    val analytics: Analytics = koinInject()
    val coroutineScope = rememberCoroutineScope()

    val (pinnedRoutes, rawTogglePinnedRoute) = managePinnedRoutes()
    fun togglePinnedRoute(routeId: String) {
        coroutineScope.launch {
            val pinned = rawTogglePinnedRoute(routeId)
            analytics.toggledPinnedRoute(pinned, routeId)
        }
    }

    val departures by viewModel.stopDepartures.collectAsState()

    LaunchedEffect(departures) { updateDepartures(departures) }

    StopDetailsView(
        modifier,
        stopId,
        viewModel,
        filters.stopFilter,
        filters.tripFilter,
        pinnedRoutes.orEmpty(),
        ::togglePinnedRoute,
        onClose,
        updateStopFilter,
        updateTripFilter,
        openAlertDetails,
        errorBannerViewModel
    )
}
