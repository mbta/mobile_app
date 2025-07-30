package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import org.koin.compose.koinInject

@Composable
fun StopDetailsUnfilteredView(
    stopId: String,
    now: EasternTimeInstant,
    viewModel: StopDetailsViewModel,
    isPinned: (String) -> Boolean,
    togglePinnedRoute: (String) -> Unit,
    onClose: () -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    openModal: (ModalRoutes) -> Unit,
    errorBannerViewModel: ErrorBannerViewModel,
) {
    val globalResponse = viewModel.globalResponse.collectAsState().value

    val stop: Stop? = globalResponse?.getStop(stopId)

    val analytics: Analytics = koinInject()

    val routeCardData = viewModel.unfilteredRouteCardData.collectAsState().value

    val onTapRoutePill = { pillFilter: PillFilter ->
        analytics.tappedRouteFilter(pillFilter.id, stopId)
        val filterId = pillFilter.id
        val routeData = routeCardData?.find { it.lineOrRoute.id == filterId }
        if (routeData != null) {
            val defaultDirectionId =
                routeData.stopData
                    .flatMap { it.data }
                    .flatMap { it.routePatterns }
                    .minOfOrNull { it.directionId } ?: 0
            updateStopFilter(StopDetailsFilter(filterId, defaultDirectionId))
        }
    }

    fun getFilterPillRoutes(
        routeCardData: List<RouteCardData>,
        global: GlobalResponse,
    ): List<PillFilter> =
        routeCardData.map {
            when (val lineOrRoute = it.lineOrRoute) {
                is RouteCardData.LineOrRoute.Line -> PillFilter.ByLine(lineOrRoute.line)
                is RouteCardData.LineOrRoute.Route ->
                    PillFilter.ByRoute(lineOrRoute.route, global.getLine(lineOrRoute.route.lineId))
            }
        }

    if (routeCardData != null) {
        stop?.let {
            val servedRoutes =
                remember(routeCardData, globalResponse) {
                    getFilterPillRoutes(routeCardData, globalResponse)
                }
            StopDetailsUnfilteredRoutesView(
                stop,
                routeCardData,
                servedRoutes,
                errorBannerViewModel,
                now,
                globalResponse,
                isPinned,
                togglePinnedRoute,
                onClose,
                onTapRoutePill,
                updateStopFilter,
                openModal,
            )
        }
    } else {
        CompositionLocalProvider(IsLoadingSheetContents provides true) {
            Column(modifier = Modifier.loadingShimmer()) {
                val placeholderData = LoadingPlaceholders.stopDetailsRouteCards()
                val filterRoutes =
                    if (globalResponse != null) {
                        getFilterPillRoutes(placeholderData, globalResponse)
                    } else {
                        emptyList()
                    }

                StopDetailsUnfilteredRoutesView(
                    placeholderData.first().stopData.first().stop,
                    placeholderData,
                    filterRoutes,
                    errorBannerViewModel,
                    now,
                    globalResponse,
                    { false },
                    {},
                    onClose = onClose,
                    onTapRoutePill = {},
                    updateStopFilter = {},
                    openModal = {},
                )
            }
        }
    }
}
