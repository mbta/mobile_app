package com.mbta.tid.mbta_app.android.routeDetails

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.component.StopListRow
import com.mbta.tid.mbta_app.android.component.StopRowStyle
import com.mbta.tid.mbta_app.android.state.getRouteStops
import com.mbta.tid.mbta_app.android.stopDetails.DirectionPicker
import com.mbta.tid.mbta_app.android.stopDetails.TripRouteAccents
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.android.util.rememberSuspend
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteDetailsStopList
import com.mbta.tid.mbta_app.model.response.GlobalResponse

@Composable
fun RouteStopListView(
    lineOrRoute: RouteCardData.LineOrRoute,
    globalData: GlobalResponse,
    onClick: (RouteDetailsStopList.Entry) -> Unit,
    onClose: () -> Unit,
    errorBannerViewModel: ErrorBannerViewModel,
    defaultSelectedRouteId: String? = null,
    rightSideContent: @Composable RowScope.(RouteDetailsStopList.Entry, Modifier) -> Unit,
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
        rememberSuspend(selectedRouteId, routeStops, globalData) {
            RouteDetailsStopList.fromPieces(selectedRouteId, routeStops, globalData)
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
                    stopList.segments.withIndex().map { (segmentIndex, segment) ->
                        if (segment.isTypical) {

                            segment.stops.withIndex().map { (stopIndex, stop) ->
                                val stopRowStyle =
                                    if (segmentIndex == 0 && stopIndex == 0) {
                                        StopRowStyle.FirstLineStop
                                    } else if (
                                        segmentIndex == stopList.segments.lastIndex &&
                                            stopIndex == segment.stops.lastIndex
                                    ) {
                                        StopRowStyle.LastLineStop
                                    } else {
                                        StopRowStyle.MidLineStop
                                    }

                                StopListRow(
                                    stop.stop,
                                    onClick = { onClick(stop) },
                                    routeAccents = TripRouteAccents(lineOrRoute.sortRoute),
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                    connectingRoutes = stop.connectingRoutes,
                                    stopRowStyle = stopRowStyle,
                                    rightSideContent = { modifier ->
                                        rightSideContent(stop, modifier)
                                    },
                                )
                            }
                        } else {

                            CollapsableStopList(
                                lineOrRoute,
                                segment,
                                onClick,
                                segmentIndex == stopList.segments.lastIndex,
                            ) { stop, modifier ->
                                rightSideContent(stop, modifier)
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

@Composable
fun CollapsableStopList(
    lineOrRoute: RouteCardData.LineOrRoute,
    segment: RouteDetailsStopList.Segment,
    onClick: (RouteDetailsStopList.Entry) -> Unit,
    isLastSegment: Boolean = false,
    rightSideContent: @Composable RowScope.(RouteDetailsStopList.Entry, Modifier) -> Unit,
) {

    var stopsExpanded by rememberSaveable { mutableStateOf(false) }

    if (segment.stops.size == 1) {
        val stop = segment.stops.first()
        StopListRow(
            stop.stop,
            onClick = { onClick(stop) },
            routeAccents = TripRouteAccents(lineOrRoute.sortRoute),
            modifier =
                Modifier.minimumInteractiveComponentSize().background(colorResource(R.color.fill1)),
            connectingRoutes = stop.connectingRoutes,
            stopRowStyle = StopRowStyle.StandaloneStop,
            descriptor = { Text("Less common stop", style = Typography.footnote) },
            rightSideContent = { modifier -> rightSideContent(stop, modifier) },
        )
    } else {
        Column(Modifier.padding(horizontal = 6.dp)) {
            Row(
                Modifier.height(IntrinsicSize.Min)
                    // TODO: Click label
                    .clickable() { stopsExpanded = !stopsExpanded }
                    // TODO: Content description
                    .padding(horizontal = 10.dp)
                    .defaultMinSize(minHeight = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedContent(
                    stopsExpanded,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(500)) togetherWith
                            fadeOut(animationSpec = tween(500))
                    },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(start = 22.dp).width(20.dp),
                    ) {
                        if (it) {
                            Icon(
                                painterResource(R.drawable.fa_caret_right),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp).rotate(90f),
                                tint = colorResource(R.color.deemphasized),
                            )
                        } else {
                            Icon(
                                painterResource(R.drawable.fa_caret_right),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = colorResource(R.color.deemphasized),
                            )
                        }
                    }
                }
                Column(
                    modifier =
                        Modifier.weight(1f).padding(vertical = 12.dp).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "${segment.stops.size} less common stops",
                        color = colorResource(R.color.text),
                        style = Typography.body,
                    )
                    Text(
                        "Only served at certain times of day",
                        color = colorResource(R.color.deemphasized),
                        style = Typography.footnote,
                    )
                }
            }

            if (!isLastSegment) {
                HaloSeparator()
            }
        }

        if (stopsExpanded) {
            segment.stops.map { stop ->
                StopListRow(
                    stop.stop,
                    onClick = { onClick(stop) },
                    routeAccents = TripRouteAccents(lineOrRoute.sortRoute),
                    modifier =
                        Modifier.minimumInteractiveComponentSize()
                            .background(colorResource(R.color.fill1)),
                    connectingRoutes = stop.connectingRoutes,
                    stopRowStyle = StopRowStyle.StandaloneStop,
                    rightSideContent = { modifier -> rightSideContent(stop, modifier) },
                )
            }
        }
    }
}
