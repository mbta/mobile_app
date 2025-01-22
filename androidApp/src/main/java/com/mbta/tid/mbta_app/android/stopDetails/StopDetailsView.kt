package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
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
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import kotlin.time.Duration.Companion.seconds
import org.koin.compose.koinInject

@Composable
fun StopDetailsView(
    modifier: Modifier = Modifier,
    stopId: String,
    stopFilter: StopDetailsFilter?,
    tripFilter: TripDetailsFilter?,
    departures: StopDetailsDepartures?,
    pinnedRoutes: Set<String>,
    togglePinnedRoute: (String) -> Unit,
    onClose: () -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    openAlertDetails: (ModalRoutes.AlertDetails) -> Unit,
    errorBannerViewModel: ErrorBannerViewModel
) {
    val globalResponse = getGlobalData("StopDetailsView.getGlobalData")
    // TODO: Set this from the StopDetailsViewModel based on the feature toggle once the VM exists
    val showElevatorAccessibility = false

    val stop: Stop? = globalResponse?.stops?.get(stopId)

    val now = timer(updateInterval = 5.seconds)
    val analytics: Analytics = koinInject()

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

        Column(modifier) {
            Column {
                SheetHeader(onClose = onClose, title = stop.name)
                if (servedRoutes.size > 1) {
                    StopDetailsFilterPills(
                        servedRoutes = servedRoutes,
                        filter = stopFilter,
                        onTapRoutePill = onTapRoutePill,
                        onClearFilter = { updateStopFilter(null) }
                    )
                }
                HorizontalDivider(
                    Modifier.fillMaxWidth()
                        .padding(top = 8.dp)
                        .border(2.dp, colorResource(R.color.halo))
                )
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
                departures,
                globalResponse,
                now,
                stopFilter ?: departures?.autoStopFilter(),
                tripFilter,
                togglePinnedRoute,
                pinnedRoutes,
                updateStopFilter,
                openAlertDetails = ::openAndRecordAlertDetails
            )
        }
    }
}
