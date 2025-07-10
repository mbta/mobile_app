package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.component.ScrollSeparatorLazyColumn
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.model.FavoriteBridge
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlin.time.Instant

@Composable
fun ColumnScope.RouteCardList(
    routeCardData: List<RouteCardData>?,
    emptyView: @Composable () -> Unit,
    global: GlobalResponse?,
    now: Instant,
    isFavorite: (FavoriteBridge) -> Boolean,
    togglePinnedRoute: (String) -> Unit,
    showStationAccessibility: Boolean,
    onOpenStopDetails: (String, StopDetailsFilter?) -> Unit,
) {
    val contentPadding = PaddingValues(start = 15.dp, top = 7.dp, end = 15.dp, bottom = 16.dp)
    val verticalArrangement = Arrangement.spacedBy(14.dp)
    val horizontalAlignment = Alignment.CenterHorizontally
    if (routeCardData == null) {
        CompositionLocalProvider(IsLoadingSheetContents provides true) {
            ScrollSeparatorLazyColumn(
                contentPadding = contentPadding,
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment,
                userScrollEnabled = false,
            ) {
                items(5) { LoadingRouteCard() }
            }
        }
    } else if (routeCardData.isEmpty()) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(8.dp).weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            emptyView()
        }
    } else {
        ScrollSeparatorLazyColumn(
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
        ) {
            items(routeCardData) {
                RouteCard(
                    it,
                    global,
                    now,
                    isFavorite,
                    togglePinnedRoute,
                    showStopHeader = true,
                    showStationAccessibility,
                    onOpenStopDetails,
                )
            }
        }
    }
}
