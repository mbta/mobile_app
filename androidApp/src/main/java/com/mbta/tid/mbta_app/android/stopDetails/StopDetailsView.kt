package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import kotlin.time.Duration.Companion.seconds
import org.koin.compose.koinInject

@Composable
fun StopDetailsView(
    modifier: Modifier = Modifier,
    stopId: String,
    viewModel: StopDetailsViewModel,
    stopFilter: StopDetailsFilter?,
    tripFilter: TripDetailsFilter?,
    pinnedRoutes: Set<String>,
    togglePinnedRoute: (String) -> Unit,
    onClose: () -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    updateTripDetailsFilter: (TripDetailsFilter?) -> Unit,
    setMapSelectedVehicle: (Vehicle?) -> Unit,
    openAlertDetails: (ModalRoutes.AlertDetails) -> Unit,
    errorBannerViewModel: ErrorBannerViewModel
) {
    val now = timer(updateInterval = 5.seconds)
    val analytics: Analytics = koinInject()

    val departures by viewModel.stopDepartures.collectAsState()

    fun openAndRecordAlertDetails(alertDetails: ModalRoutes.AlertDetails) {
        openAlertDetails(alertDetails)
        analytics.tappedAlertDetails(
            routeId = alertDetails.lineId ?: alertDetails.routeIds?.firstOrNull() ?: "",
            stopId = alertDetails.stopId ?: "",
            alertId = alertDetails.alertId
        )
    }

    if (stopFilter != null) {
        StopDetailsFilteredView(
            stopId,
            stopFilter,
            tripFilter,
            departures,
            now,
            viewModel,
            pinnedRoutes,
            togglePinnedRoute,
            onClose,
            updateStopFilter,
            updateTripDetailsFilter,
            ::openAndRecordAlertDetails,
            setMapSelectedVehicle,
            errorBannerViewModel,
        )
    } else {
        StopDetailsUnfilteredView(
            stopId,
            now,
            viewModel,
            pinnedRoutes,
            togglePinnedRoute,
            onClose,
            updateStopFilter,
            ::openAndRecordAlertDetails,
            errorBannerViewModel,
        )
    }
}
