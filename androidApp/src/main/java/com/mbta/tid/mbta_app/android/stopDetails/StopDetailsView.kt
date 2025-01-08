package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import kotlin.time.Duration.Companion.seconds

@Composable
fun StopDetailsView(
    modifier: Modifier = Modifier,
    stop: Stop,
    filter: StopDetailsFilter?,
    departures: StopDetailsDepartures?,
    pinnedRoutes: Set<String>,
    togglePinnedRoute: (String) -> Unit,
    onClose: () -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    errorBannerViewModel: ErrorBannerViewModel
) {
    val globalResponse = getGlobalData()

    val now = timer(updateInterval = 5.seconds)

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
        val filterId = pillFilter.id
        if (filter?.routeId == filterId) {
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

    Column(modifier) {
        Column {
            SheetHeader(onClose = onClose, title = stop.name)
            if (servedRoutes.size > 1) {
                StopDetailsFilterPills(
                    servedRoutes = servedRoutes,
                    filter = filter,
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

        if (departures != null) {
            StopDetailsRoutesView(
                departures,
                globalResponse,
                now,
                filter ?: departures.autoStopFilter(),
                togglePinnedRoute,
                pinnedRoutes,
                updateStopFilter
            )
        } else {
            CircularProgressIndicator()
        }
    }
}
