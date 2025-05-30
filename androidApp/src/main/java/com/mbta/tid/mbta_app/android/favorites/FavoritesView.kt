package com.mbta.tid.mbta_app.android.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.routeCard.RouteCardList
import com.mbta.tid.mbta_app.android.state.getSchedule
import com.mbta.tid.mbta_app.android.state.subscribeToPredictions
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.Settings
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds

@Composable
fun FavoritesView(
    modifier: Modifier = Modifier,
    openSheetRoute: (SheetRoutes) -> Unit,
    favoritesViewModel: FavoritesViewModel,
    errorBannerViewModel: ErrorBannerViewModel,
    alertData: AlertsStreamDataResponse?,
    globalResponse: GlobalResponse?,
    targetLocation: Position?,
) {
    val now by timer(updateInterval = 5.seconds)
    val stopIds = favoritesViewModel.favorites?.map { it.stop }
    val showStationAccessibility = SettingsCache.get(Settings.StationAccessibility)
    val schedules = getSchedule(stopIds, "FavoritesView.getSchedule")
    val predictionsVM = subscribeToPredictions(stopIds, errorBannerViewModel = errorBannerViewModel)
    val predictions by predictionsVM.predictionsFlow.collectAsState(initial = null)

    LaunchedEffect(Unit) { favoritesViewModel.loadFavorites() }

    LaunchedEffect(
        globalResponse,
        schedules,
        predictions,
        alertData,
        now,
        stopIds,
        targetLocation,
    ) {
        favoritesViewModel.loadRouteCardData(
            globalResponse,
            targetLocation,
            schedules,
            predictions,
            alertData,
            now,
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.favorites_link),
            modifier = Modifier.semantics { heading() }.padding(horizontal = 16.dp),
            style = Typography.title3Semibold,
        )
        ErrorBanner(errorBannerViewModel)
        val routeCardData = favoritesViewModel.routeCardData
        RouteCardList(
            routeCardData = routeCardData,
            emptyView = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(8.dp).weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    NoFavoritesView()
                }
            },
            global = globalResponse,
            now = now,
            pinnedRoutes = emptySet(),
            togglePinnedRoute = {},
            showStationAccessibility = showStationAccessibility,
            onOpenStopDetails = { stopId, filter ->
                openSheetRoute(SheetRoutes.StopDetails(stopId, filter, null))
            },
        )
    }
}
