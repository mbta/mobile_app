package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.SheetRoutes
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
    isFavorite: (RouteStopDirection) -> Boolean,
    updateFavorites: (Map<RouteStopDirection, Boolean>, Int) -> Unit,
    onClose: () -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    updateTripDetailsFilter: (TripDetailsFilter?) -> Unit,
    tileScrollState: ScrollState,
    openModal: (ModalRoutes) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    errorBannerViewModel: ErrorBannerViewModel,
) {
    val now by timer(updateInterval = 5.seconds)
    val analytics: Analytics = koinInject()

    fun openModalAndRecord(modal: ModalRoutes) {
        openModal(modal)
        if (modal is ModalRoutes.AlertDetails) {
            analytics.tappedAlertDetails(
                routeId = modal.lineId ?: modal.routeIds?.firstOrNull() ?: "",
                stopId = modal.stopId ?: "",
                alertId = modal.alertId,
            )
        }
    }

    if (stopFilter != null) {
        StopDetailsFilteredView(
            stopId,
            stopFilter,
            tripFilter,
            allAlerts,
            now,
            viewModel,
            isFavorite,
            updateFavorites,
            onClose,
            updateStopFilter,
            updateTripDetailsFilter,
            tileScrollState,
            ::openModalAndRecord,
            openSheetRoute,
            errorBannerViewModel,
        )
    } else {
        StopDetailsUnfilteredView(
            stopId,
            now,
            viewModel,
            isFavorite,
            onClose,
            updateStopFilter,
            ::openModalAndRecord,
            errorBannerViewModel,
        )
    }
}
