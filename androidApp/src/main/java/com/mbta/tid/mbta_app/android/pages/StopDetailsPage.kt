package com.mbta.tid.mbta_app.android.pages

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mapbox.maps.MapboxExperimental
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.stopDetails.StopDetailsView
import com.mbta.tid.mbta_app.android.stopDetails.StopDetailsViewModel
import com.mbta.tid.mbta_app.android.util.managePinnedRoutes
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters

@Composable
@ExperimentalMaterial3Api
@MapboxExperimental
fun StopDetailsPage(
    modifier: Modifier = Modifier,
    viewModel: StopDetailsViewModel,
    filters: StopDetailsPageFilters,
    onClose: () -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    updateDepartures: (StopDetailsDepartures?) -> Unit,
    errorBannerViewModel: ErrorBannerViewModel
) {
    val stopId = filters.stopId

    val (pinnedRoutes, togglePinnedRoute) = managePinnedRoutes()

    val departures by viewModel.stopDepartures.collectAsState()

    LaunchedEffect(departures) { updateDepartures(departures) }

    StopDetailsView(
        modifier,
        stopId,
        filters.stopFilter,
        filters.tripFilter,
        departures,
        pinnedRoutes.orEmpty(),
        togglePinnedRoute,
        onClose,
        updateStopFilter,
        errorBannerViewModel
    )
}
