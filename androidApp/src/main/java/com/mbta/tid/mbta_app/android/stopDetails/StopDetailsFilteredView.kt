package com.mbta.tid.mbta_app.android.stopDetails

import android.util.Log
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.SheetRoutes
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
    tileScrollState: ScrollState,
    openModal: (ModalRoutes) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    setMapSelectedVehicle: (Vehicle?) -> Unit,
    errorBannerViewModel: ErrorBannerViewModel
) {
    val globalResponse = getGlobalData("StopDetailsView.getGlobalData")
    val patternsByStop =
        departures?.let {
            it.routes.find { patterns -> patterns.routeIdentifier == stopFilter.routeId }
        }

    if (patternsByStop != null) {

        val tileData =
            departures
                .stopDetailsFormattedTrips(stopFilter.routeId, stopFilter.directionId, now)
                .mapNotNull { tripAndFormat ->
                    val upcoming = tripAndFormat.upcoming
                    val route = patternsByStop.routes.find { it.id == upcoming.trip.routeId }
                    if (route == null) {
                        Log.e(
                            "StopDetailsFilteredView",
                            "Failed to find route ID ${upcoming.trip.routeId} from upcoming trip in patternsByStop.routes (${patternsByStop.routes.map { it.id }}"
                        )
                        null
                    } else {
                        TileData.fromUpcoming(upcoming, route, now)
                    }
                }

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
            tileScrollState = tileScrollState,
            pinnedRoutes = pinnedRoutes,
            togglePinnedRoute = togglePinnedRoute,
            onClose = onClose,
            setMapSelectedVehicle = setMapSelectedVehicle,
            openModal = openModal,
            openSheetRoute = openSheetRoute
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
                        placeholderDepartures
                            .stopDetailsFormattedTrips(
                                stopFilter.routeId,
                                stopFilter.directionId,
                                now
                            )
                            .mapNotNull { tripAndFormat ->
                                TileData.fromUpcoming(
                                    tripAndFormat.upcoming,
                                    placeholderDepartures.routes.first().representativeRoute,
                                    now
                                )
                            },
                    noPredictionsStatus = null,
                    elevatorAlerts = placeholderDepartures.elevatorAlerts,
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
                    setMapSelectedVehicle = {},
                    openModal = {},
                    openSheetRoute = {}
                )
            }
        }
    }
}
