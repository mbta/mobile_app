package com.mbta.tid.mbta_app.android.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.component.ActionButton
import com.mbta.tid.mbta_app.android.component.ActionButtonKind
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.routeCard.RouteCardList
import com.mbta.tid.mbta_app.android.state.getSchedule
import com.mbta.tid.mbta_app.android.state.subscribeToPredictions
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.FavoriteBridge
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import com.mbta.tid.mbta_app.repositories.Settings
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds

@Composable
fun FavoritesView(
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

    fun onAddFavorites() {
        openSheetRoute(SheetRoutes.RoutePicker(RoutePickerPath.Root, RouteDetailsContext.Favorites))
    }

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
        favoritesViewModel.loadRealtimeRouteCardData(
            globalResponse,
            targetLocation,
            schedules,
            predictions,
            alertData,
            now,
        )
    }

    val routeCardData = favoritesViewModel.routeCardData

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp)
                .padding(horizontal = 16.dp),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.favorites_link),
                modifier = Modifier.semantics { heading() },
                style = Typography.title2Bold,
            )
            Spacer(Modifier.weight(1f))
            if (!routeCardData.isNullOrEmpty()) {
                ActionButton(ActionButtonKind.Plus, action = ::onAddFavorites)
                TextButton(onClick = { openSheetRoute(SheetRoutes.EditFavorites) }) {
                    Text(
                        text = stringResource(R.string.edit),
                        modifier =
                            Modifier
                                .background(
                                    colorResource(R.color.text).copy(alpha = 0.6f),
                                    RoundedCornerShape(percent = 100),
                                )
                                .padding(17.dp, 5.dp),
                        color = colorResource(R.color.fill2),
                        style = Typography.callout,
                    )
                }
            }
        }
        ErrorBanner(errorBannerViewModel)
        RouteCardList(
            routeCardData = routeCardData,
            emptyView = { NoFavoritesView(::onAddFavorites) },
            global = globalResponse,
            now = now,
            isFavorite = { favoriteBridge ->
                favoriteBridge is FavoriteBridge.Favorite &&
                    (favoritesViewModel.favorites ?: emptySet()).contains(
                        favoriteBridge.routeStopDirection
                    )
            },
            togglePinnedRoute = {},
            showStationAccessibility = showStationAccessibility,
            onOpenStopDetails = { stopId, filter ->
                openSheetRoute(SheetRoutes.StopDetails(stopId, filter, null))
            },
        )
    }
}
