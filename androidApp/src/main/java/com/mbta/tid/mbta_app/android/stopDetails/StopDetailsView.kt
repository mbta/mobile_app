package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel
import org.koin.compose.koinInject

@Composable
fun StopDetailsView(
    modifier: Modifier = Modifier,
    stopId: String,
    stopFilter: StopDetailsFilter?,
    tripFilter: TripDetailsFilter?,
    allAlerts: AlertsStreamDataResponse?,
    now: EasternTimeInstant,
    isFavorite: (RouteStopDirection) -> Boolean,
    updateFavorites: (Map<RouteStopDirection, Boolean>, Int) -> Unit,
    onClose: () -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    updateTripFilter: (TripDetailsFilter?) -> Unit,
    tileScrollState: ScrollState,
    openModal: (ModalRoutes) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    errorBannerViewModel: IErrorBannerViewModel,
) {
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
            isFavorite,
            updateFavorites,
            onClose,
            updateStopFilter,
            updateTripFilter,
            tileScrollState,
            ::openModalAndRecord,
            openSheetRoute,
            errorBannerViewModel,
        )
    } else {
        StopDetailsUnfilteredView(
            stopId,
            now,
            isFavorite,
            onClose,
            updateStopFilter,
            ::openModalAndRecord,
            errorBannerViewModel,
        )
    }
}
