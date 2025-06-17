package com.mbta.tid.mbta_app.android.routeDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.FavoriteConfirmationDialog
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.component.StopListRow
import com.mbta.tid.mbta_app.android.component.StopPlacement
import com.mbta.tid.mbta_app.android.state.getRouteStops
import com.mbta.tid.mbta_app.android.stopDetails.DirectionPicker
import com.mbta.tid.mbta_app.android.stopDetails.TripRouteAccents
import com.mbta.tid.mbta_app.android.util.manageFavorites
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.android.util.rememberSuspend
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteDetailsStopList
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import kotlinx.coroutines.launch

sealed class RouteDetailsRowContext {
    data class Details(val stop: Stop) : RouteDetailsRowContext()

    data class Favorites(val isFavorited: Boolean, val onTapStar: () -> Unit) :
        RouteDetailsRowContext()
}

@Composable
fun RouteStopListView(
    lineOrRoute: RouteCardData.LineOrRoute,
    context: RouteDetailsContext,
    globalData: GlobalResponse,
    onClick: (RouteDetailsStopList.Entry) -> Unit,
    onClose: () -> Unit,
    errorBannerViewModel: ErrorBannerViewModel,
    defaultSelectedRouteId: String? = null,
    rightSideContent: @Composable RowScope.(RouteDetailsRowContext, Modifier) -> Unit,
) {
    val routes = lineOrRoute.allRoutes.sorted()
    val routeIds = routes.map { it.id }
    val parameters =
        remember(lineOrRoute, globalData) {
            RouteDetailsStopList.RouteParameters(lineOrRoute, globalData)
        }
    var selectedDirection by rememberSaveable {
        mutableIntStateOf(parameters.availableDirections.firstOrNull() ?: 0)
    }
    var selectedRouteId by rememberSaveable {
        mutableStateOf(defaultSelectedRouteId ?: routeIds.first())
    }

    val routeStops =
        getRouteStops(selectedRouteId, selectedDirection, "RouteDetailsView.routeStopIds")

    val stopList =
        rememberSuspend(selectedRouteId, selectedDirection, routeStops, globalData) {
            RouteDetailsStopList.fromPieces(
                selectedRouteId,
                selectedDirection,
                routeStops,
                globalData,
            )
        }
    RouteDetailsContext.Details

    val managedFavorites = manageFavorites()
    val favorites = managedFavorites.favoriteRoutes
    val updateFavorites = managedFavorites.updateFavorites

    fun isFavorite(routeStopDirection: RouteStopDirection): Boolean =
        favorites?.contains(routeStopDirection) ?: false

    val coroutineScope = rememberCoroutineScope()
    fun confirmFavorites(updatedValues: Map<RouteStopDirection, Boolean>) {
        coroutineScope.launch { updateFavorites(updatedValues) }
    }

    var showFavoritesStopConfirmation by rememberSaveable { mutableStateOf<Stop?>(null) }

    fun stopRowContext(stop: Stop) =
        when (context) {
            is RouteDetailsContext.Details -> RouteDetailsRowContext.Details(stop)
            is RouteDetailsContext.Favorites ->
                RouteDetailsRowContext.Favorites(
                    isFavorited =
                        isFavorite(RouteStopDirection(lineOrRoute.id, stop.id, selectedDirection)),
                    onTapStar = { showFavoritesStopConfirmation = stop },
                )
        }

    showFavoritesStopConfirmation?.let { stop ->
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            FavoriteConfirmationDialog(
                lineOrRoute,
                stop,
                parameters.directions.filter { it.id in parameters.availableDirections },
                proposedFavorites =
                    parameters.availableDirections.associateWith {
                        it == selectedDirection ||
                            isFavorite(RouteStopDirection(lineOrRoute.id, stop.id, it))
                    },
                updateFavorites = ::confirmFavorites,
            ) {
                showFavoritesStopConfirmation = null
            }
        }
    }

    Column {
        SheetHeader(onClose = onClose, title = lineOrRoute.name)
        ErrorBanner(errorBannerViewModel)

        DirectionPicker(
            parameters.availableDirections,
            parameters.directions,
            lineOrRoute.sortRoute,
            selectedDirection,
            updateDirectionId = { selectedDirection = it },
        )

        if (lineOrRoute is RouteCardData.LineOrRoute.Line && routes.size > 1) {
            Column {
                for (route in routes) {
                    val selected = route.id == selectedRouteId
                    Row(
                        Modifier.fillMaxWidth()
                            .background(
                                if (selected) colorResource(R.color.fill3) else Color.Transparent
                            )
                            .clickable { selectedRouteId = route.id }
                            .padding(8.dp),
                        Arrangement.spacedBy(8.dp),
                        Alignment.CenterVertically,
                    ) {
                        RoutePill(route, lineOrRoute.line, RoutePillType.Fixed)
                        Text(route.directionDestinations[selectedDirection] ?: "")
                    }
                }
            }
        }

        Box(Modifier.verticalScroll(rememberScrollState())) {
            Box(
                Modifier.matchParentSize()
                    .padding(horizontal = 4.dp)
                    .haloContainer(2.dp, backgroundColor = colorResource(R.color.fill2))
            )
            Column {
                if (stopList != null) {
                    stopList.segments.forEachIndexed { segmentIndex, segment ->
                        if (segment.isTypical) {
                            segment.stops.forEachIndexed { stopIndex, stop ->
                                val stopPlacement =
                                    StopPlacement(
                                        isFirst = segmentIndex == 0 && stopIndex == 0,
                                        isLast =
                                            segmentIndex == stopList.segments.lastIndex &&
                                                stopIndex == segment.stops.lastIndex,
                                        includeLineDiagram = segment.hasRouteLine,
                                    )

                                StopListRow(
                                    stop.stop,
                                    onClick = { onClick(stop) },
                                    routeAccents = TripRouteAccents(lineOrRoute.sortRoute),
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                    connectingRoutes = stop.connectingRoutes,
                                    stopPlacement = stopPlacement,
                                    rightSideContent = { modifier ->
                                        rightSideContent(stopRowContext(stop.stop), modifier)
                                    },
                                )
                            }
                        } else {
                            CollapsableStopList(
                                lineOrRoute,
                                segment,
                                onClick,
                                segmentIndex == 0,
                                segmentIndex == stopList.segments.lastIndex,
                            ) { stop, modifier ->
                                rightSideContent(stopRowContext(stop.stop), modifier)
                            }
                        }
                    }
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
