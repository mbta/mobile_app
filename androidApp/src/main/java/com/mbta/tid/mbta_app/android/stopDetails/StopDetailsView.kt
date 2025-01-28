package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.Stop
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
    val globalResponse = getGlobalData("StopDetailsView.getGlobalData")
    // TODO: Set this from the StopDetailsViewModel based on the feature toggle once the VM exists
    val showElevatorAccessibility = false

    val stop: Stop? = globalResponse?.stops?.get(stopId)

    val now = timer(updateInterval = 5.seconds)
    val analytics: Analytics = koinInject()

    val departures by viewModel.stopDepartures.collectAsState()

    val servedRoutes =
        remember(departures, globalResponse) {
            departures
                ?.routes
                ?.map { patterns ->
                    if (patterns.line != null) {
                        PillFilter.ByLine(patterns.line!!)
                    } else {
                        PillFilter.ByRoute(
                            patterns.representativeRoute,
                            globalResponse?.getLine(patterns.representativeRoute.lineId)
                        )
                    }
                }
                .orEmpty()
        }

    val onTapRoutePill = { pillFilter: PillFilter ->
        analytics.tappedRouteFilter(pillFilter.id, stopId)
        val filterId = pillFilter.id
        if (stopFilter?.routeId == filterId) {
            updateStopFilter(null)
        } else {
            val patterns = departures?.routes?.find { it.routeIdentifier == filterId }
            if (patterns != null) {
                val defaultDirectionId =
                    patterns.patterns
                        .flatMap { it.patterns.mapNotNull { it?.directionId } }
                        .minOrNull()
                        ?: 0
                updateStopFilter(StopDetailsFilter(filterId, defaultDirectionId))
            }
        }
    }

    fun openAndRecordAlertDetails(alertDetails: ModalRoutes.AlertDetails) {
        openAlertDetails(alertDetails)
        analytics.tappedAlertDetails(
            routeId = alertDetails.lineId ?: alertDetails.routeIds?.firstOrNull() ?: "",
            stopId = alertDetails.stopId ?: "",
            alertId = alertDetails.alertId
        )
    }

    if (stop != null) {

        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            stopFilter?.let { filter ->
                departures?.let {
                    val patternsByStop =
                        it.routes.find { patterns -> patterns.routeIdentifier == filter.routeId }
                    patternsByStop?.let {
                        StopDetailsFilteredHeader(
                            patternsByStop.representativeRoute,
                            patternsByStop.line,
                            patternsByStop.stop,
                            pinned = pinnedRoutes.contains(patternsByStop.routeIdentifier),
                            onPin = { togglePinnedRoute(patternsByStop.routeIdentifier) },
                            onClose = onClose
                        )
                    }
                }
            }
                ?: run {
                    SheetHeader(onClose = onClose, title = stop.name)
                    if (servedRoutes.size > 1) {
                        Box(Modifier.height(56.dp).fillMaxWidth()) {
                            StopDetailsFilterPills(
                                servedRoutes = servedRoutes,
                                onTapRoutePill = onTapRoutePill,
                                onClearFilter = { updateStopFilter(null) }
                            )
                        }
                    }
                }

            ErrorBanner(errorBannerViewModel)

            if (showElevatorAccessibility && !departures?.elevatorAlerts.isNullOrEmpty()) {
                Column(
                    Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    departures?.elevatorAlerts?.map {
                        Column(
                            Modifier.background(
                                    colorResource(R.color.fill3),
                                    RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .border(0.dp, Color.Unspecified, shape = RoundedCornerShape(8.dp))
                                .clickable(
                                    onClickLabel = stringResource(R.string.displays_more_info)
                                ) {
                                    openAndRecordAlertDetails(
                                        ModalRoutes.AlertDetails(it.id, null, null, stopId)
                                    )
                                }
                                .padding(end = 8.dp)
                        ) {
                            StopDetailsAlertHeader(it, Color.Unspecified, showInfoIcon = true)
                        }
                    }
                }
            }
            StopDetailsRoutesView(
                stopId,
                viewModel,
                globalResponse,
                now,
                stopFilter,
                tripFilter,
                togglePinnedRoute,
                pinnedRoutes,
                updateStopFilter,
                updateTripDetailsFilter,
                setMapSelectedVehicle,
                openAlertDetails = ::openAndRecordAlertDetails
            )
        }
    }
}
