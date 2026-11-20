package com.mbta.tid.mbta_app.android.routeStop

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.ModalRoutes.SaveFavorite
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ChannelDebugView
import com.mbta.tid.mbta_app.android.component.DirectionPicker
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.loading
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.RouteStopFilter
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.ErrorKey
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.NavigationCallbacks
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel
import com.mbta.tid.mbta_app.viewModel.IStopCentricViewModel
import com.mbta.tid.mbta_app.viewModel.StopCentricViewModel
import org.koin.compose.koinInject

@Composable
fun RouteStopView(
    modifier: Modifier = Modifier,
    stopId: String,
    routeStopFilter: RouteStopFilter,
    tripFilter: TripDetailsFilter?,
    allAlerts: AlertsStreamDataResponse?,
    now: EasternTimeInstant,
    isFavorite: (RouteStopDirection) -> Boolean?,
    navCallbacks: NavigationCallbacks,
    updateRouteStopFilter: (RouteStopFilter) -> Unit,
    updateTripFilter: (TripDetailsFilter?) -> Unit,
    tileScrollState: ScrollState,
    openModal: (ModalRoutes) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    errorBannerViewModel: IErrorBannerViewModel,
    stopCentricViewModel: IStopCentricViewModel = koinInject(),
) {
    val state by stopCentricViewModel.models.collectAsState()
    val global: GlobalResponse? =
        getGlobalData(ErrorKey(setOf(SheetRoutes.RouteStop::class), "RouteStopView"))

    val lineOrRoute: LineOrRoute? = global?.getLineOrRoute(routeStopFilter.routeId)

    val stop = global?.getStop(stopId)
    val realtimeStopData =
        when (val data = state.routeData) {
            is StopCentricViewModel.RouteData.RouteStop -> data.stopData
            else -> null
        }

    val leaf =
        realtimeStopData?.data?.firstOrNull {
            it.stop.id == stopId &&
                it.lineOrRoute.id == routeStopFilter.routeId &&
                it.direction.id == routeStopFilter.directionId
        }

    @Composable
    fun Header(lineOrRoute: LineOrRoute, stop: Stop) {
        val rsd = RouteStopDirection(lineOrRoute.id, stop.id, routeStopFilter.directionId)

        return Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            RouteStopHeader(
                lineOrRoute.sortRoute,
                (lineOrRoute as? LineOrRoute.Line)?.line,
                stop,
                isFavorite = isFavorite(rsd),
                onFavorite = {
                    openModal(
                        SaveFavorite(
                            lineOrRoute.id,
                            stop.id,
                            selectedDirection = routeStopFilter.directionId,
                            context = EditFavoritesContext.RouteStop,
                        ),
                    )
                },
                navCallbacks,
            )

            ErrorBanner(errorBannerViewModel, Modifier.padding(bottom = 16.dp))
            ChannelDebugView(
                content = {
                    Column(Modifier.align(Alignment.Start), horizontalAlignment = Alignment.Start) {
                        Text("stop id: $stopId")
                    }
                },
            )
        }
    }

    @Composable
    fun LoadingDepartures() {
        CompositionLocalProvider(IsLoadingSheetContents provides true) {
            Column(modifier = Modifier.loading()) {
                val routeData =
                    LoadingPlaceholders.routeCardData(
                        routeStopFilter.routeId,
                        trips = 10,
                        RouteCardData.Context.RouteStop,
                        now,
                    )
                val stopData = routeData.stopData.single()
                val placeholderLeaf = stopData.data.first()

                RouteStopDeparturesView(
                    stopId = stopId,
                    routeStopFilter = routeStopFilter,
                    tripFilter = tripFilter,
                    leaf = placeholderLeaf,
                    selectedDirection = stopData.data.first().direction,
                    allAlerts = AlertsStreamDataResponse(emptyMap()),
                    now = now,
                    updateTripFilter = {},
                    tileScrollState = rememberScrollState(),
                    isFavorite = false,
                    openModal = {},
                    openSheetRoute = {},
                )
            }
        }
    }

    @Composable
    fun Loaded(lineOrRoute: LineOrRoute, stop: Stop, global: GlobalResponse) {
        val routeHex: String = lineOrRoute.backgroundColor

        val routeColor: Color = Color.fromHex(routeHex)

        val allPatternsForStop: List<RoutePattern> = global.getPatternsFor(stopId, lineOrRoute)
        val directions: List<Direction> =
            lineOrRoute.directions(
                global,
                stop,
                allPatternsForStop.filter { it.isTypical() || it.isCanonicalOnly() },
            )

        val availableDirections = directions.filter {
            !stop.isLastStopForAllPatterns(it.id, allPatternsForStop, global)
        }

        val rsd = RouteStopDirection(lineOrRoute.id, stop.id, routeStopFilter.directionId)

        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Header(lineOrRoute, stop)
            Box(Modifier.fillMaxSize().background(routeColor)) {
                HorizontalDivider(
                    Modifier.fillMaxWidth().zIndex(1f).border(2.dp, colorResource(R.color.halo)),
                )
                Column(
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DirectionPicker(
                        availableDirections = availableDirections.map { it.id },
                        directions = directions,
                        route = lineOrRoute.sortRoute,
                        selectedDirectionId = routeStopFilter.directionId,
                        updateDirectionId = {
                            updateRouteStopFilter(routeStopFilter.copy(directionId = it))
                        },
                        modifier = Modifier.padding(horizontal = 10.dp),
                    )

                    if (leaf != null) {
                        RouteStopDeparturesView(
                            stopId = stopId,
                            routeStopFilter = routeStopFilter,
                            tripFilter = tripFilter,
                            leaf = leaf,
                            selectedDirection = directions[rsd.direction],
                            allAlerts = allAlerts,
                            now = now,
                            updateTripFilter = updateTripFilter,
                            tileScrollState = tileScrollState,
                            isFavorite = isFavorite(rsd) ?: false,
                            openModal = openModal,
                            openSheetRoute = openSheetRoute,
                        )
                    } else {
                        LoadingDepartures()
                    }
                }
            }
        }
    }

    return if (global != null && lineOrRoute != null && stop != null) {
        Loaded(lineOrRoute, stop, global)
    } else {
        val routeData =
            LoadingPlaceholders.routeCardData(
                routeStopFilter.routeId,
                10,
                RouteCardData.Context.RouteStop,
                now,
            )

        CompositionLocalProvider(IsLoadingSheetContents provides true) {
            Column(modifier = Modifier.loading()) {
                Loaded(
                    routeData.lineOrRoute,
                    routeData.stopData.first().stop,
                    GlobalResponse(ObjectCollectionBuilder("RouteStopViewLoading")),
                )
            }
        }
    }
}
