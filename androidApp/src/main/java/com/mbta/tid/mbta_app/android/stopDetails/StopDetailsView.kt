package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import kotlin.time.Duration.Companion.seconds
import org.koin.compose.koinInject

@Composable
fun StopDetailsView(
    modifier: Modifier = Modifier,
    stopId: String,
    viewModel: StopDetailsViewModel,
    stopFilter: StopDetailsFilter?,
    tripFilter: TripDetailsFilter?,
    allAlerts: AlertsStreamDataResponse?,
    pinnedRoutes: Set<String>,
    togglePinnedRoute: (String) -> Unit,
    onClose: () -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    updateTripDetailsFilter: (TripDetailsFilter?) -> Unit,
    tileScrollState: ScrollState,
    openModal: (ModalRoutes) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    errorBannerViewModel: ErrorBannerViewModel
) {
    val now = timer(updateInterval = 5.seconds)
    val analytics: Analytics = koinInject()

    val departures by viewModel.stopDepartures.collectAsState()
    val routeCardData by viewModel.routeCardData.collectAsState()
    val groupByDirection by viewModel.groupByDirection.collectAsState()

    LaunchedEffect(null) { viewModel.loadSettings() }

    fun openModalAndRecord(modal: ModalRoutes) {
        openModal(modal)
        if (modal is ModalRoutes.AlertDetails) {
            analytics.tappedAlertDetails(
                routeId = modal.lineId ?: modal.routeIds?.firstOrNull() ?: "",
                stopId = modal.stopId ?: "",
                alertId = modal.alertId
            )
        }
    }

    if (stopFilter != null) {
        if (groupByDirection) {
            StopDetailsFilteredView(
                stopId,
                stopFilter,
                tripFilter,
                routeCardData,
                allAlerts,
                now,
                viewModel,
                pinnedRoutes,
                togglePinnedRoute,
                onClose,
                updateStopFilter,
                updateTripDetailsFilter,
                tileScrollState,
                ::openModalAndRecord,
                openSheetRoute,
                errorBannerViewModel,
            )
        } else {
            StopDetailsFilteredView(
                stopId,
                stopFilter,
                tripFilter,
                departures,
                allAlerts,
                now,
                viewModel,
                pinnedRoutes,
                togglePinnedRoute,
                onClose,
                updateStopFilter,
                updateTripDetailsFilter,
                tileScrollState,
                ::openModalAndRecord,
                openSheetRoute,
                errorBannerViewModel,
            )
        }
    } else {
        StopDetailsUnfilteredView(
            stopId,
            now,
            viewModel,
            pinnedRoutes,
            togglePinnedRoute,
            onClose,
            updateStopFilter,
            ::openModalAndRecord,
            errorBannerViewModel,
        )
    }
}
