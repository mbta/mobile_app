package com.mbta.tid.mbta_app.android.routeDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.component.StopListContext
import com.mbta.tid.mbta_app.android.component.StopListGroupToggle
import com.mbta.tid.mbta_app.android.component.StopListRow
import com.mbta.tid.mbta_app.android.component.StopListToggleGroup
import com.mbta.tid.mbta_app.android.component.StopPlacement
import com.mbta.tid.mbta_app.android.stopDetails.TripRouteAccents
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.RouteDetailsStopList

@Composable
fun CollapsableStopList(
    lineOrRoute: LineOrRoute,
    segment: RouteDetailsStopList.Segment,
    onClick: (RouteDetailsStopList.Entry) -> Unit,
    onClickLabel: @Composable (RouteDetailsStopList.Entry) -> String? = { null },
    isFirstSegment: Boolean = false,
    isLastSegment: Boolean = false,
    rightSideContent: @Composable RowScope.(RouteDetailsStopList.Entry, Modifier) -> Unit,
) {

    var stopsExpanded by rememberSaveable { mutableStateOf(false) }

    val routeAccents = TripRouteAccents(lineOrRoute.sortRoute)
    if (segment.stops.size == 1) {
        val stop = segment.stops.first()
        StopListRow(
            stop.stop,
            stop.stopLane,
            stop.stickConnections,
            onClick = { onClick(stop) },
            routeAccents = routeAccents,
            stopListContext = StopListContext.RouteDetails,
            modifier =
                Modifier.minimumInteractiveComponentSize().background(colorResource(R.color.fill1)),
            connectingRoutes = stop.connectingRoutes,
            onClickLabel = onClickLabel(stop),
            stopPlacement = StopPlacement(isFirstSegment, isLastSegment),
            descriptor = {
                Text(stringResource(R.string.less_common_stop), style = Typography.footnote)
            },
            rightSideContent = { modifier -> rightSideContent(stop, modifier) },
        )
    } else {
        val twistedConnections = remember(segment) { segment.twistedConnections() }
        Column {
            // Usually, we draw an extra StickDiagram above the HaloSeparator to get it to connect,
            // but here the RouteLineTwist may be in all sorts of wacky intermediate states,
            // so instead we just draw the HaloSeparator underneath the RouteLineTwist.
            Box {
                StopListGroupToggle(
                    stopsExpanded = stopsExpanded,
                    setStopsExpanded = { stopsExpanded = it },
                    contentDescription = null,
                    onClickLabel =
                        stringResource(
                            if (stopsExpanded) R.string.collapse_stops else R.string.expand_stops
                        ),
                    routeAccents = routeAccents,
                    stickConnections = twistedConnections,
                    label = {
                        Column(
                            modifier =
                                Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                AnnotatedString.fromHtml(
                                    stringResource(R.string.less_common_stops, segment.stops.size)
                                ),
                                color = colorResource(R.color.text),
                                style = Typography.body,
                            )
                            Text(
                                stringResource(R.string.only_served_at_certain_times),
                                color = colorResource(R.color.deemphasized),
                                style = Typography.footnote,
                            )
                        }
                    },
                )

                if (!isLastSegment) {
                    HaloSeparator(Modifier.zIndex(-1f).align(Alignment.BottomCenter))
                }
            }

            StopListToggleGroup(stopsExpanded) {
                segment.stops.forEachIndexed { index, stop ->
                    StopListRow(
                        stop.stop,
                        stop.stopLane,
                        stop.stickConnections,
                        onClick = { onClick(stop) },
                        routeAccents = routeAccents,
                        stopListContext = StopListContext.RouteDetails,
                        modifier =
                            Modifier.minimumInteractiveComponentSize()
                                .background(colorResource(R.color.fill1)),
                        connectingRoutes = stop.connectingRoutes,
                        onClickLabel = onClickLabel(stop),
                        stopPlacement =
                            StopPlacement(
                                isFirstSegment && index == 0,
                                isLastSegment && index == segment.stops.lastIndex,
                            ),
                        rightSideContent = { modifier -> rightSideContent(stop, modifier) },
                    )
                }
            }
        }
    }
}
