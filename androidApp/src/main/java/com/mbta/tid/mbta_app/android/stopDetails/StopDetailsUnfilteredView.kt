package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.datetime.Instant
import org.koin.compose.koinInject

@Composable
fun StopDetailsUnfilteredView(
    stopId: String,
    now: Instant,
    viewModel: StopDetailsViewModel,
    pinnedRoutes: Set<String>,
    togglePinnedRoute: (String) -> Unit,
    onClose: () -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    openModal: (ModalRoutes) -> Unit,
    errorBannerViewModel: ErrorBannerViewModel
) {
    val globalResponse = getGlobalData("StopDetailsView.getGlobalData")
    val showElevatorAccessibility by viewModel.showElevatorAccessibility.collectAsState()

    val stop: Stop? = globalResponse?.getStop(stopId)

    val analytics: Analytics = koinInject()

    val departures by viewModel.stopDepartures.collectAsState()

    val onTapRoutePill = { pillFilter: PillFilter ->
        analytics.tappedRouteFilter(pillFilter.id, stopId)
        val filterId = pillFilter.id
        val patterns = departures?.routes?.find { it.routeIdentifier == filterId }
        if (patterns != null) {
            val defaultDirectionId =
                patterns.patterns.flatMap { it.patterns.mapNotNull { it?.directionId } }.minOrNull()
                    ?: 0
            updateStopFilter(StopDetailsFilter(filterId, defaultDirectionId))
        }
    }

    fun getFilterPillRoutes(
        departures: StopDetailsDepartures,
        global: GlobalResponse
    ): List<PillFilter> =
        departures.routes.map { patterns ->
            if (patterns.line != null) {
                PillFilter.ByLine(patterns.line!!)
            } else {
                PillFilter.ByRoute(
                    patterns.representativeRoute,
                    global.getLine(patterns.representativeRoute.lineId)
                )
            }
        }

    departures?.let { notNullDepartures ->
        stop?.let {
            val servedRoutes =
                remember(notNullDepartures, globalResponse) {
                    getFilterPillRoutes(notNullDepartures, globalResponse)
                }
            StopDetailsUnfilteredRoutesView(
                stop,
                notNullDepartures,
                servedRoutes,
                errorBannerViewModel,
                showElevatorAccessibility,
                now,
                togglePinnedRoute,
                pinnedRoutes,
                onClose,
                onTapRoutePill,
                updateStopFilter,
                openModal
            )
        }
    }
        ?: run {
            CompositionLocalProvider(IsLoadingSheetContents provides true) {
                Column(modifier = Modifier.loadingShimmer()) {
                    val placeholderDepartures = LoadingPlaceholders.stopDetailsDepartures(null)
                    val filterRoutes =
                        if (globalResponse != null) {
                            getFilterPillRoutes(placeholderDepartures, globalResponse)
                        } else {
                            emptyList()
                        }

                    StopDetailsUnfilteredRoutesView(
                        placeholderDepartures.routes.first().stop,
                        placeholderDepartures,
                        filterRoutes,
                        errorBannerViewModel,
                        showElevatorAccessibility,
                        now,
                        {},
                        emptySet(),
                        onClose = onClose,
                        onTapRoutePill = {},
                        updateStopFilter = {},
                        openModal = {}
                    )
                }
            }
        }
}
