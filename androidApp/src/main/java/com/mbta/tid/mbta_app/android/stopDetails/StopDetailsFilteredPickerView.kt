package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.datetime.Instant

@Composable
fun StopDetailsFilteredPickerView(
    stopId: String,
    stopFilter: StopDetailsFilter,
    tripFilter: TripDetailsFilter?,
    routeStopData: RouteCardData.RouteStopData,
    allAlerts: AlertsStreamDataResponse?,
    global: GlobalResponse?,
    now: Instant,
    viewModel: StopDetailsViewModel,
    errorBannerViewModel: ErrorBannerViewModel,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    updateTripFilter: (TripDetailsFilter?) -> Unit,
    tileScrollState: ScrollState,
    pinnedRoutes: Set<String>,
    togglePinnedRoute: (String) -> Unit,
    openModal: (ModalRoutes) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    onClose: () -> Unit,
) {
    val leaf = routeStopData.data.find { it.directionId == stopFilter.directionId }

    val lineOrRoute = routeStopData.lineOrRoute
    val stop = routeStopData.stop
    val availableDirections = routeStopData.data.map { it.directionId }.distinct().sorted()
    val directions = routeStopData.directions

    val pinned = pinnedRoutes.contains(lineOrRoute.id)

    val routeHex: String = lineOrRoute.backgroundColor
    val routeColor: Color = Color.fromHex(routeHex)

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        StopDetailsFilteredHeader(
            lineOrRoute.sortRoute,
            (lineOrRoute as? RouteCardData.LineOrRoute.Line)?.line,
            stop,
            pinned = pinned,
            onPin = { togglePinnedRoute(lineOrRoute.id) },
            onClose = onClose,
        )

        ErrorBanner(errorBannerViewModel, Modifier.padding(vertical = 16.dp))

        Box(Modifier.fillMaxSize().background(routeColor)) {
            HorizontalDivider(
                Modifier.fillMaxWidth().zIndex(1f).border(2.dp, colorResource(R.color.halo))
            )
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DirectionPicker(
                    availableDirections = availableDirections,
                    directions = directions,
                    route = lineOrRoute.sortRoute,
                    line = (lineOrRoute as? RouteCardData.LineOrRoute.Line)?.line,
                    stopFilter,
                    updateStopFilter,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )

                if (leaf != null) {
                    StopDetailsFilteredDeparturesView(
                        stopId = stopId,
                        stopFilter = stopFilter,
                        tripFilter = tripFilter,
                        leaf = leaf,
                        allAlerts = allAlerts,
                        global = global,
                        now = now,
                        viewModel = viewModel,
                        updateTripFilter = updateTripFilter,
                        tileScrollState = tileScrollState,
                        pinnedRoutes = pinnedRoutes,
                        openModal = openModal,
                        openSheetRoute = openSheetRoute,
                    )
                } else {
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
                            val placeholderLeaf = stopData.data.first()

                            StopDetailsFilteredDeparturesView(
                                stopId = stopId,
                                stopFilter = stopFilter,
                                tripFilter = tripFilter,
                                leaf = placeholderLeaf,
                                allAlerts = AlertsStreamDataResponse(emptyMap()),
                                global = global,
                                now = now,
                                viewModel = viewModel,
                                updateTripFilter = {},
                                tileScrollState = rememberScrollState(),
                                pinnedRoutes = emptySet(),
                                openModal = {},
                                openSheetRoute = {},
                            )
                        }
                    }
                }
            }
        }
    }
}
