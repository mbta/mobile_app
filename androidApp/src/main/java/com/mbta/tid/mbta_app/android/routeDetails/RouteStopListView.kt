package com.mbta.tid.mbta_app.android.routeDetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.component.SaveFavoritesContext
import com.mbta.tid.mbta_app.android.component.SaveFavoritesFlow
import com.mbta.tid.mbta_app.android.component.ScrollSeparatorColumn
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.component.StopListContext
import com.mbta.tid.mbta_app.android.component.StopListRow
import com.mbta.tid.mbta_app.android.component.StopPlacement
import com.mbta.tid.mbta_app.android.state.getRouteStops
import com.mbta.tid.mbta_app.android.stopDetails.DirectionPicker
import com.mbta.tid.mbta_app.android.stopDetails.TripRouteAccents
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.contrastTranslucent
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.manageFavorites
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.android.util.rememberSuspend
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.mbta.tid.mbta_app.model.Route
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
    onClick: (RouteDetailsRowContext) -> Unit,
    onClickLabel: @Composable (RouteDetailsRowContext) -> String? = { null },
    onBack: () -> Unit,
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
        val allPatternsForStop = globalData.getPatternsFor(stop.id, lineOrRoute)
        val stopDirections =
            lineOrRoute.directions(globalData, stop, allPatternsForStop.filter { it.isTypical() })
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            SaveFavoritesFlow(
                lineOrRoute,
                stop,
                stopDirections.filter {
                    it.id in parameters.availableDirections &&
                        !stop.isLastStopForAllPatterns(it.id, allPatternsForStop, globalData)
                },
                selectedDirection = selectedDirection,
                context = SaveFavoritesContext.Favorites,
                isFavorite = ::isFavorite,
                updateFavorites = ::confirmFavorites,
            ) {
                showFavoritesStopConfirmation = null
            }
        }
    }

    Column {
        SheetHeader(
            title = lineOrRoute.name,
            closeText =
                if (context is RouteDetailsContext.Favorites) stringResource(R.string.done)
                else null,
            onBack = onBack,
            onClose = onClose,
            buttonColors = ButtonDefaults.contrastTranslucent(),
        )
        ErrorBanner(errorBannerViewModel)

        DirectionPicker(
            parameters.availableDirections,
            parameters.directions,
            lineOrRoute.sortRoute,
            selectedDirection,
            updateDirectionId = { selectedDirection = it },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )

        if (lineOrRoute is RouteCardData.LineOrRoute.Line && routes.size > 1) {
            LineRoutePicker(lineOrRoute.line, routes, selectedRouteId, selectedDirection) {
                selectedRouteId = it
            }
        }

        RouteStops(
            lineOrRoute,
            stopList,
            selectedDirection,
            context,
            onTapStop = onClick,
            onClickLabel = onClickLabel,
            stopRowContext = ::stopRowContext,
            rightSideContent = rightSideContent,
        )
    }
}

@Composable
private fun RouteStops(
    lineOrRoute: RouteCardData.LineOrRoute,
    stopList: RouteDetailsStopList?,
    selectedDirection: Int,
    context: RouteDetailsContext,
    onTapStop: (RouteDetailsRowContext) -> Unit,
    onClickLabel: @Composable (RouteDetailsRowContext) -> String?,
    stopRowContext: (Stop) -> RouteDetailsRowContext,
    rightSideContent: @Composable RowScope.(RouteDetailsRowContext, Modifier) -> Unit,
    loading: Boolean = false,
) {
    if (stopList == null || stopList.directionId != selectedDirection) {
        LoadingRouteStops(lineOrRoute, selectedDirection, context, rightSideContent)
        return
    }

    ScrollSeparatorColumn(
        Modifier.padding(horizontal = 14.dp)
            .padding(top = 8.dp, bottom = 40.dp)
            .haloContainer(2.dp, backgroundColor = colorResource(R.color.fill2))
            .then(if (loading) Modifier.loadingShimmer() else Modifier),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
        scrollEnabled = !loading,
    ) {
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

                    val stopRowContext = stopRowContext(stop.stop)
                    StopListRow(
                        stop.stop,
                        onClick = { onTapStop(stopRowContext) },
                        routeAccents = TripRouteAccents(lineOrRoute.sortRoute),
                        stopListContext = StopListContext.RouteDetails,
                        modifier = Modifier.minimumInteractiveComponentSize().fillMaxWidth(),
                        connectingRoutes = stop.connectingRoutes,
                        onClickLabel = onClickLabel(stopRowContext),
                        stopPlacement = stopPlacement,
                        rightSideContent = { modifier ->
                            rightSideContent(stopRowContext, modifier)
                        },
                    )
                }
            } else {
                CollapsableStopList(
                    lineOrRoute,
                    segment,
                    onClick = { onTapStop(stopRowContext(it.stop)) },
                    onClickLabel = { onClickLabel(stopRowContext(it.stop)) },
                    segmentIndex == 0,
                    segmentIndex == stopList.segments.lastIndex,
                ) { stop, modifier ->
                    rightSideContent(stopRowContext(stop.stop), modifier)
                }
            }
        }
    }
}

@Composable
private fun LineRoutePicker(
    line: Line,
    routes: List<Route>,
    selectedRouteId: String,
    selectedDirection: Int,
    onSelect: (String) -> Unit,
) {
    val backgroundColor =
        colorResource(R.color.deselected_toggle_2)
            .copy(alpha = 0.6f)
            .compositeOver(Color.fromHex(line.color))

    Column(
        Modifier.padding(horizontal = 14.dp)
            .padding(top = 6.dp, bottom = 4.dp)
            .haloContainer(1.dp, backgroundColor, backgroundColor)
    ) {
        for (route in routes) {
            val selected = route.id == selectedRouteId
            val haloColor = if (selected) colorResource(R.color.halo) else Color.Transparent
            val rowColor = if (selected) colorResource(R.color.fill3) else Color.Transparent
            val textColor =
                if (selected) colorResource(R.color.text) else Color.fromHex(line.textColor)
            Row(
                Modifier.fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .haloContainer(1.dp, haloColor, rowColor, 7.dp)
                    .clickable { onSelect(route.id) }
                    .padding(8.dp),
                Arrangement.spacedBy(8.dp),
                Alignment.CenterVertically,
            ) {
                RoutePill(route, line, RoutePillType.Fixed)
                Text(
                    route.directionDestinations[selectedDirection] ?: "",
                    color = textColor,
                    style = Typography.title3Semibold,
                )
            }
        }
    }
}

@Composable
private fun LoadingRouteStops(
    lineOrRoute: RouteCardData.LineOrRoute,
    selectedDirection: Int,
    context: RouteDetailsContext,
    rightSideContent: @Composable RowScope.(RouteDetailsRowContext, Modifier) -> Unit,
) {
    val loadingStops =
        remember(lineOrRoute, selectedDirection) {
            LoadingPlaceholders.routeDetailsStops(lineOrRoute, selectedDirection)
        }
    CompositionLocalProvider(IsLoadingSheetContents provides true) {
        RouteStops(
            lineOrRoute,
            stopList = loadingStops,
            selectedDirection = selectedDirection,
            context = context,
            onTapStop = { _ -> },
            onClickLabel = { null },
            stopRowContext = { stop ->
                when (context) {
                    is RouteDetailsContext.Details -> RouteDetailsRowContext.Details(stop)
                    is RouteDetailsContext.Favorites ->
                        RouteDetailsRowContext.Favorites(isFavorited = false, onTapStar = {})
                }
            },
            rightSideContent = rightSideContent,
            loading = true,
        )
    }
}
