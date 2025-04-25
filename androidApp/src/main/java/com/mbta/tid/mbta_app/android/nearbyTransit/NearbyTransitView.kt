package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.routeCard.LoadingRouteCard
import com.mbta.tid.mbta_app.android.component.routeCard.RouteCard
import com.mbta.tid.mbta_app.android.state.getSchedule
import com.mbta.tid.mbta_app.android.state.subscribeToPredictions
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.managePinnedRoutes
import com.mbta.tid.mbta_app.android.util.rememberSuspend
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopsAssociated
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.withRealtimeInfo
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
    errorBannerViewModel: ErrorBannerViewModel
) {
    LaunchedEffect(null) { nearbyVM.loadSettings() }

    LaunchedEffect(targetLocation, globalResponse) {
        if (globalResponse != null && targetLocation != null) {
            nearbyVM.getNearby(
                globalResponse,
                targetLocation,
                setLastLocation,
                setSelectingLocation
            )
        }
    }
    val now by timer(updateInterval = 5.seconds)
    val stopIds = remember(nearbyVM.nearby) { nearbyVM.nearby?.stopIds()?.toList() }
    val schedules = getSchedule(stopIds, "NearbyTransitView.getSchedule")
    val predictionsVM = subscribeToPredictions(stopIds, errorBannerViewModel = errorBannerViewModel)
    val predictions by predictionsVM.predictionsFlow.collectAsState(initial = null)

    val groupByDirection by nearbyVM.groupByDirection.collectAsState(false)
    val showStationAccessibility by nearbyVM.showStationAccessibility.collectAsState(false)

    val analytics: Analytics = koinInject()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(targetLocation == null) {
        if (targetLocation == null) {
            predictionsVM.reset()
        }
    }
    val (pinnedRoutes, rawTogglePinnedRoute) = managePinnedRoutes()

    fun togglePinnedRoute(routeId: String) {
        coroutineScope.launch {
            val pinned = rawTogglePinnedRoute(routeId)
            analytics.toggledPinnedRoute(pinned, routeId)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.nearby_transit),
            modifier = Modifier.semantics { heading() }.padding(horizontal = 16.dp),
            style = Typography.title3Semibold,
        )
        ErrorBanner(errorBannerViewModel)
        if (groupByDirection) {
            LaunchedEffect(
                nearbyVM.nearby,
                globalResponse,
                targetLocation,
                schedules,
                predictions,
                alertData,
                now,
                pinnedRoutes
            ) {
                nearbyVM.loadRouteCardData(
                    globalResponse,
                    targetLocation,
                    schedules,
                    predictions,
                    alertData,
                    now,
                    pinnedRoutes
                )
            }

            val routeCardData = nearbyVM.routeCardData

            if (routeCardData == null) {
                CompositionLocalProvider(IsLoadingSheetContents provides true) {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).padding(8.dp).weight(1f),
                    ) {
                        for (i in 1..5) {
                            LoadingRouteCard()
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }
            } else if (routeCardData.isEmpty()) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(8.dp).weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    noNearbyStopsView()
                    Spacer(Modifier.weight(1f))
                }
            } else {
                LazyColumn(
                    contentPadding =
                        PaddingValues(start = 15.dp, top = 7.dp, end = 15.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(routeCardData) {
                        RouteCard(
                            it,
                            globalResponse,
                            now,
                            pinnedRoutes?.contains(it.lineOrRoute.id) ?: false,
                            ::togglePinnedRoute,
                            showStationAccessibility,
                            onOpenStopDetails
                        )
                    }
                }
            }
        } else {
            val nearbyWithRealtimeInfo =
                rememberSuspend(
                    nearbyVM.nearby,
                    globalResponse,
                    targetLocation,
                    schedules,
                    predictions,
                    alertData,
                    now,
                    pinnedRoutes
                ) {
                    if (targetLocation != null) {
                        nearbyVM.nearby?.withRealtimeInfo(
                            globalData = globalResponse,
                            sortByDistanceFrom = targetLocation,
                            schedules,
                            predictions,
                            alertData,
                            now,
                            pinnedRoutes.orEmpty(),
                        )
                    } else {
                        null
                    }
                }

            if (nearbyWithRealtimeInfo == null) {
                CompositionLocalProvider(IsLoadingSheetContents provides true) {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).padding(8.dp).weight(1f),
                    ) {
                        for (i in 1..5) {
                            LoadingRouteCard()
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }
            } else if (nearbyWithRealtimeInfo.isEmpty()) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(8.dp).weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    noNearbyStopsView()
                    Spacer(Modifier.weight(1f))
                }
            } else {
                LazyColumn {
                    items(nearbyWithRealtimeInfo) {
                        when (it) {
                            is StopsAssociated.WithRoute ->
                                NearbyRouteView(
                                    it,
                                    pinnedRoutes.orEmpty().contains(it.id),
                                    ::togglePinnedRoute,
                                    now,
                                    onOpenStopDetails,
                                    showStationAccessibility
                                )
                            is StopsAssociated.WithLine ->
                                NearbyLineView(
                                    it,
                                    pinnedRoutes.orEmpty().contains(it.id),
                                    ::togglePinnedRoute,
                                    now,
                                    onOpenStopDetails,
                                    showStationAccessibility
                                )
                        }
                    }
                }
            }
        }
    }
}
