package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.datetime.Instant

@Composable
fun RouteCardList(
    routeCardData: List<RouteCardData>?,
    emptyView: @Composable () -> Unit,
    global: GlobalResponse?,
    now: Instant,
    pinnedRoutes: Set<String>?,
    togglePinnedRoute: (String) -> Unit,
    showStationAccessibility: Boolean,
    onOpenStopDetails: (String, StopDetailsFilter?) -> Unit,
) {
    if (routeCardData == null) {
        CompositionLocalProvider(IsLoadingSheetContents provides true) {
            LazyColumn(
                contentPadding =
                    PaddingValues(start = 15.dp, top = 7.dp, end = 15.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(5) { LoadingRouteCard() }
            }
        }
    } else if (routeCardData.isEmpty()) {
        emptyView()
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 15.dp, top = 7.dp, end = 15.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(routeCardData) {
                RouteCard(
                    it,
                    global,
                    now,
                    pinnedRoutes?.contains(it.lineOrRoute.id) ?: false,
                    togglePinnedRoute,
                    showStopHeader = true,
                    showStationAccessibility,
                    onOpenStopDetails,
                )
            }
        }
    }
}
