package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.util.StopDetailsFilter
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import kotlin.time.Duration.Companion.seconds
import org.koin.compose.koinInject

@Composable
fun StopDetailsView(
    stop: Stop,
    filterState: MutableState<StopDetailsFilter?>,
    departures: StopDetailsDepartures?,
    pinnedRoutes: Set<String>,
    togglePinnedRoute: (String) -> Unit,
    onClose: () -> Unit,
    globalRepository: IGlobalRepository = koinInject(),
) {
    var filter by filterState
    var globalResponse: GlobalResponse? by remember { mutableStateOf(null) }

    LaunchedEffect(null) { globalResponse = globalRepository.getGlobalData() }

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
            filter = null
        } else {
            val patterns = departures?.routes?.find { it.routeIdentifier == filterId }
            if (patterns != null) {
                val defaultDirectionId =
                    patterns.patterns
                        .flatMap { it.patterns.map(RoutePattern::directionId) }
                        .minOrNull()
                        ?: 0
                filter = StopDetailsFilter(filterId, defaultDirectionId)
            }
        }
    }

    Column {
        Column(Modifier.padding(bottom = 8.dp).border(2.dp, colorResource(R.color.halo))) {
            SheetHeader(onClose = onClose, title = stop.name)
            StopDetailsFilterPills(
                servedRoutes = servedRoutes,
                filterState = filterState,
                onTapRoutePill = onTapRoutePill
            )
        }

        if (departures != null) {
            StopDetailsRoutesView(
                departures,
                globalResponse,
                now,
                filterState,
                togglePinnedRoute,
                pinnedRoutes
            )
        } else {
            CircularProgressIndicator()
        }
    }
}
