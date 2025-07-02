package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.component.routeCard.RouteCardList
import com.mbta.tid.mbta_app.android.state.getSchedule
import com.mbta.tid.mbta_app.android.state.subscribeToPredictions
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.manageFavorites
import com.mbta.tid.mbta_app.android.util.managePinnedRoutes
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.FavoriteBridge
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.Settings
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun NearbyTransitView(
    alertData: AlertsStreamDataResponse?,
    globalResponse: GlobalResponse?,
    targetLocation: Position?,
    setLastLocation: (Position) -> Unit,
    setSelectingLocation: (Boolean) -> Unit,
    onOpenStopDetails: (String, StopDetailsFilter?) -> Unit,
    noNearbyStopsView: @Composable () -> Unit,
    nearbyVM: NearbyTransitViewModel = koinViewModel(),
    errorBannerViewModel: ErrorBannerViewModel,
) {
    LaunchedEffect(targetLocation, globalResponse) {
        if (globalResponse != null && targetLocation != null) {
            nearbyVM.getNearby(
                globalResponse,
                targetLocation,
                setLastLocation,
                setSelectingLocation,
            )
        }
    }
    val now by timer(updateInterval = 5.seconds)
    val stopIds = nearbyVM.nearbyStopIds
    val schedules = getSchedule(stopIds, "NearbyTransitView.getSchedule")
    val predictionsVM = subscribeToPredictions(stopIds, errorBannerViewModel = errorBannerViewModel)
    val predictions by predictionsVM.predictionsFlow.collectAsState(initial = null)

    val showStationAccessibility = SettingsCache.get(Settings.StationAccessibility)

    val analytics: Analytics = koinInject()
    val coroutineScope = rememberCoroutineScope()

    val enhancedFavorites = SettingsCache.get(Settings.EnhancedFavorites)

    LaunchedEffect(targetLocation == null) {
        if (targetLocation == null) {
            predictionsVM.reset()
        }
    }
    val (pinnedRoutes, rawTogglePinnedRoute) = managePinnedRoutes()
    val (favorites) = manageFavorites()

    fun togglePinnedRoute(routeId: String) {
        coroutineScope.launch {
            val pinned = rawTogglePinnedRoute(routeId)
            analytics.toggledPinnedRoute(pinned, routeId)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SheetHeader(title = stringResource(R.string.nearby_transit))
        ErrorBanner(errorBannerViewModel)
        LaunchedEffect(
            stopIds,
            globalResponse,
            targetLocation,
            schedules,
            predictions,
            alertData,
            now,
            pinnedRoutes,
        ) {
            val pinnedRoutesForSorting =
                if (enhancedFavorites) {
                    emptySet()
                } else {
                    pinnedRoutes
                }

            nearbyVM.loadRouteCardData(
                globalResponse,
                targetLocation,
                schedules,
                predictions,
                alertData,
                now,
                pinnedRoutesForSorting,
            )
        }

        val routeCardData = nearbyVM.routeCardData

        RouteCardList(
            routeCardData = routeCardData,
            emptyView = {
                noNearbyStopsView()
                Spacer(Modifier.weight(1f))
            },
            global = globalResponse,
            now = now,
            isFavorite = { favoriteBridge ->
                if (!enhancedFavorites && favoriteBridge is FavoriteBridge.Pinned) {
                    (pinnedRoutes ?: emptySet()).contains(favoriteBridge.routeId)
                } else if (enhancedFavorites && favoriteBridge is FavoriteBridge.Favorite) {
                    (favorites ?: emptySet()).contains(favoriteBridge.routeStopDirection)
                } else {
                    false
                }
            },
            togglePinnedRoute = ::togglePinnedRoute,
            showStationAccessibility = showStationAccessibility,
            onOpenStopDetails = onOpenStopDetails,
        )
    }
}
