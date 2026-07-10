package com.mbta.tid.mbta_app.android.stopDetails

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.DebugView
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.SaveFavoritesFlow
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.loading
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.ErrorKey
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.NavigationCallbacks
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel
import com.mbta.tid.mbta_app.viewModel.IStopDetailsViewModel
import com.mbta.tid.mbta_app.viewModel.StopDetailsViewModel
import org.koin.compose.koinInject

@Composable
fun StopDetailsFilteredView(
    stopId: String,
    stopFilter: StopDetailsFilter,
    tripFilter: TripDetailsFilter?,
    allAlerts: AlertsStreamDataResponse?,
    now: EasternTimeInstant,
    isFavorite: (RouteStopDirection) -> Boolean?,
    updateFavorites: (Map<RouteStopDirection, FavoriteSettings?>, Int) -> Unit,
    navCallbacks: NavigationCallbacks,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    updateTripFilter: (TripDetailsFilter?) -> Unit,
    tileScrollState: ScrollState,
    openModal: (ModalRoutes) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    errorBannerViewModel: IErrorBannerViewModel,
    stopDetailsViewModel: IStopDetailsViewModel = koinInject(),
) {
    val state by stopDetailsViewModel.models.collectAsState()
    val global: GlobalResponse? =
        getGlobalData(ErrorKey(setOf(SheetRoutes.StopDetails::class), "StopDetailsFilteredView"))

    val lineOrRoute: LineOrRoute? = global?.getLineOrRoute(stopFilter.routeId)

    val stop = global?.getStop(stopId)
    val realtimeStopData =
        when (val data = state.routeData) {
            is StopDetailsViewModel.RouteData.Filtered -> data.stopData
            else -> null
        }

    val leaf =
        realtimeStopData?.data?.firstOrNull {
            it.stop.id == stopId &&
                it.lineOrRoute.id == stopFilter.routeId &&
                it.direction.id == stopFilter.directionId
        }

    var inSaveFavoritesFlow by rememberSaveable { mutableStateOf(false) }

    @Composable
    fun Header(lineOrRoute: LineOrRoute, stop: Stop, availableDirections: List<Direction>) {
        val rsd = RouteStopDirection(lineOrRoute.id, stop.id, stopFilter.directionId)

        return Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            if (inSaveFavoritesFlow) {
                SaveFavoritesFlow(
                    lineOrRoute,
                    stop,
                    availableDirections,
                    selectedDirection = stopFilter.directionId,
                    context = EditFavoritesContext.StopDetails,
                    updateFavorites = { newValues ->
                        updateFavorites(newValues, stopFilter.directionId)
                    },
                    isFavorite = { rsd -> isFavorite(rsd) ?: false },
                    onClose = { inSaveFavoritesFlow = false },
                    openModal = openModal,
                )
            }
            StopDetailsFilteredHeader(
                lineOrRoute.sortRoute,
                (lineOrRoute as? LineOrRoute.Line)?.line,
                stop,
                isFavorite = isFavorite(rsd),
                onFavorite = { inSaveFavoritesFlow = true },
                navCallbacks,
            )

            ErrorBanner(errorBannerViewModel, Modifier.padding(bottom = 16.dp))
            DebugView(
                content = {
                    Column(Modifier.align(Alignment.Start), horizontalAlignment = Alignment.Start) {
                        Text("stop id: $stopId")
                        Text("trip id: ${tripFilter?.tripId ?: "null"}")
                        Text("vehicle id: ${tripFilter?.vehicleId ?: "null"}")
                    }
                }
            )
        }
    }

    @Composable
    fun LoadingDepartures() {
        CompositionLocalProvider(IsLoadingSheetContents provides true) {
            Column(modifier = Modifier.loading()) {
                val routeData =
                    LoadingPlaceholders.routeCardData(
                        stopFilter.routeId,
                        trips = 10,
                        RouteCardData.Context.StopDetailsFiltered,
                        now,
                    )
                val stopData = routeData.stopData.single()
                val placeholderLeaf = stopData.data.first()

                StopDetailsFilteredDeparturesView(
                    stopId = stopId,
                    stopFilter = stopFilter,
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
            lineOrRoute.directions(global, stop, allPatternsForStop.filter { it.isTypical() })

        val availableDirections = directions.filter {
            !stop.isLastStopForAllPatterns(it.id, allPatternsForStop, global)
        }

        val rsd = RouteStopDirection(lineOrRoute.id, stop.id, stopFilter.directionId)

        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Header(lineOrRoute, stop, availableDirections)
            Box(Modifier.fillMaxSize().background(routeColor)) {
                HorizontalDivider(
                    Modifier.fillMaxWidth().zIndex(1f).border(2.dp, colorResource(R.color.halo))
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
                        selectedDirectionId = stopFilter.directionId,
                        updateDirectionId = { updateStopFilter(stopFilter.copy(directionId = it)) },
                        modifier = Modifier.padding(horizontal = 10.dp),
                    )

                    if (leaf != null) {
                        StopDetailsFilteredDeparturesView(
                            stopId = stopId,
                            stopFilter = stopFilter,
                            tripFilter = tripFilter,
                            leaf = leaf,
                            selectedDirection = directions[stopFilter.directionId],
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
                stopFilter.routeId,
                10,
                RouteCardData.Context.StopDetailsFiltered,
                now,
            )

        CompositionLocalProvider(IsLoadingSheetContents provides true) {
            Column(modifier = Modifier.loading()) {
                Loaded(
                    routeData.lineOrRoute,
                    routeData.stopData.first().stop,
                    GlobalResponse(ObjectCollectionBuilder("StopDetailFilteredViewLoading")),
                )
            }
        }
    }
}
