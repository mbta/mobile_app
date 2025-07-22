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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.SaveFavoritesFlow
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.model.FavoriteBridge
import com.mbta.tid.mbta_app.model.FavoriteUpdateBridge
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.SheetRoutes
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import kotlin.time.Instant

@Composable
fun StopDetailsFilteredPickerView(
    stopId: String,
    stopFilter: StopDetailsFilter,
    tripFilter: TripDetailsFilter?,
    routeStopData: RouteCardData.RouteStopData,
    allAlerts: AlertsStreamDataResponse?,
    global: GlobalResponse?,
    now: Instant,
    viewModel: StopDetailsViewModel,
    errorBannerViewModel: ErrorBannerViewModel,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    updateTripFilter: (TripDetailsFilter?) -> Unit,
    tileScrollState: ScrollState,
    isFavorite: (FavoriteBridge) -> Boolean,
    updateFavorites: (FavoriteUpdateBridge) -> Unit,
    openModal: (ModalRoutes) -> Unit,
    openSheetRoute: (SheetRoutes) -> Unit,
    onClose: () -> Unit,
) {
    val leaf = routeStopData.data.find { it.directionId == stopFilter.directionId }

    val lineOrRoute = routeStopData.lineOrRoute
    val stop = routeStopData.stop

    val availableDirections = routeStopData.availableDirections.sorted()
    val directions = routeStopData.directions

    val routeHex: String = lineOrRoute.backgroundColor
    val routeColor: Color = Color.fromHex(routeHex)

    val enhancedFavorites = SettingsCache.get(Settings.EnhancedFavorites)
    val favoriteBridge =
        if (enhancedFavorites) {
            FavoriteBridge.Favorite(
                RouteStopDirection(lineOrRoute.id, stop.id, stopFilter.directionId)
            )
        } else {
            FavoriteBridge.Pinned(lineOrRoute.id)
        }

    var inSaveFavoritesFlow by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        if (inSaveFavoritesFlow) {
            SaveFavoritesFlow(
                lineOrRoute,
                stop,
                directions.filter { it.id in availableDirections },
                selectedDirection = stopFilter.directionId,
                context = EditFavoritesContext.StopDetails,
                updateFavorites = { newValues ->
                    updateFavorites(FavoriteUpdateBridge.Favorites(newValues))
                },
                isFavorite = { rsd -> isFavorite(FavoriteBridge.Favorite(rsd)) },
            ) {
                inSaveFavoritesFlow = false
            }
        }
        StopDetailsFilteredHeader(
            lineOrRoute.sortRoute,
            (lineOrRoute as? RouteCardData.LineOrRoute.Line)?.line,
            stop,
            pinned = isFavorite(favoriteBridge),
            onPin = {
                if (favoriteBridge is FavoriteBridge.Pinned) {
                    updateFavorites(FavoriteUpdateBridge.Pinned(favoriteBridge.routeId))
                } else {
                    inSaveFavoritesFlow = true
                }
            },
            onClose = onClose,
        )

        ErrorBanner(errorBannerViewModel, Modifier.padding(vertical = 16.dp))

        Box(Modifier.fillMaxSize().background(routeColor)) {
            HorizontalDivider(
                Modifier.fillMaxWidth().zIndex(1f).border(2.dp, colorResource(R.color.halo))
            )
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DirectionPicker(
                    availableDirections = availableDirections,
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
                        global = global,
                        now = now,
                        viewModel = viewModel,
                        updateTripFilter = updateTripFilter,
                        tileScrollState = tileScrollState,
                        isFavorite = isFavorite(favoriteBridge),
                        openModal = openModal,
                        openSheetRoute = openSheetRoute,
                    )
                } else {
                    CompositionLocalProvider(IsLoadingSheetContents provides true) {
                        Column(modifier = Modifier.loadingShimmer()) {
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
                                selectedDirection = stopData.directions.first(),
                                allAlerts = AlertsStreamDataResponse(emptyMap()),
                                global = global,
                                now = now,
                                viewModel = viewModel,
                                updateTripFilter = {},
                                tileScrollState = rememberScrollState(),
                                isFavorite = false,
                                openModal = {},
                                openSheetRoute = {},
                            )
                        }
                    }
                }
            }
        }
    }
}
