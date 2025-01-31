package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import kotlinx.datetime.Instant
import org.koin.compose.koinInject

@Composable
fun StopDetailsFilteredView(
    stopId: String,
    stopFilter: StopDetailsFilter,
    tripFilter: TripDetailsFilter?,
    departures: StopDetailsDepartures?,
    now: Instant,
    viewModel: StopDetailsViewModel,
    pinnedRoutes: Set<String>,
    togglePinnedRoute: (String) -> Unit,
    onClose: () -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    updateTripDetailsFilter: (TripDetailsFilter?) -> Unit,
    openAlertDetails: (ModalRoutes.AlertDetails) -> Unit,
    openExplainer: (ModalRoutes.Explainer) -> Unit,
    setMapSelectedVehicle: (Vehicle?) -> Unit,
    errorBannerViewModel: ErrorBannerViewModel
) {
    val globalResponse = getGlobalData("StopDetailsView.getGlobalData")
    val patternsByStop =
        departures?.let {
            it.routes.find { patterns -> patterns.routeIdentifier == stopFilter.routeId }
        }

    val analytics: Analytics = koinInject()

    fun openAndRecordAlertDetails(alertDetails: ModalRoutes.AlertDetails) {
        openAlertDetails(alertDetails)
        analytics.tappedAlertDetails(
            routeId = alertDetails.lineId ?: alertDetails.routeIds?.firstOrNull() ?: "",
            stopId = alertDetails.stopId ?: "",
            alertId = alertDetails.alertId
        )
    }

    if (patternsByStop != null) {

        val tileData =
            departures.stopDetailsFormattedTrips(stopFilter.routeId, stopFilter.directionId, now)

        val realtimePatterns =
            patternsByStop.patterns.filter { it.directionId() == stopFilter.directionId }
        val noPredictionsStatus =
            if (tileData.isEmpty()) {
                StopDetailsDepartures.getNoPredictionsStatus(realtimePatterns, now)
            } else {
                null
            }

        StopDetailsFilteredDeparturesView(
            stopId = stopId,
            stopFilter = stopFilter,
            tripFilter = tripFilter,
            patternsByStop = patternsByStop,
            tileData = tileData,
            noPredictionsStatus = noPredictionsStatus,
            elevatorAlerts = departures.elevatorAlerts,
            global = globalResponse,
            now = now,
            viewModel = viewModel,
            errorBannerViewModel = errorBannerViewModel,
            updateStopFilter = updateStopFilter,
            updateTripFilter = updateTripDetailsFilter,
            pinnedRoutes = pinnedRoutes,
            togglePinnedRoute = togglePinnedRoute,
            onClose = onClose,
            setMapSelectedVehicle = setMapSelectedVehicle,
            openAlertDetails = ::openAndRecordAlertDetails,
            openExplainer = openExplainer,
        )
    } else {
        CompositionLocalProvider(IsLoadingSheetContents provides true) {
            Column(modifier = Modifier.loadingShimmer()) {
                val placeholderDepartures = LoadingPlaceholders.stopDetailsDepartures(stopFilter)
                StopDetailsFilteredDeparturesView(
                    stopId = stopId,
                    stopFilter = stopFilter,
                    tripFilter = tripFilter,
                    patternsByStop = placeholderDepartures.routes.first(),
                    tileData =
                        placeholderDepartures.stopDetailsFormattedTrips(
                            stopFilter.routeId,
                            stopFilter.directionId,
                            now
                        ),
                    noPredictionsStatus = null,
                    elevatorAlerts = placeholderDepartures.elevatorAlerts,
                    global = globalResponse,
                    now = now,
                    viewModel = viewModel,
                    errorBannerViewModel = errorBannerViewModel,
                    updateStopFilter = {},
                    updateTripFilter = {},
                    pinnedRoutes = emptySet(),
                    togglePinnedRoute = {},
                    onClose = onClose,
                    setMapSelectedVehicle = {},
                    openAlertDetails = {},
                    openExplainer = {},
                )
            }
        }
    }
}
