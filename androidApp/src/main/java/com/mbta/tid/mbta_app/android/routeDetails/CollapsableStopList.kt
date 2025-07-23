package com.mbta.tid.mbta_app.android.routeDetails

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.component.StopListContext
import com.mbta.tid.mbta_app.android.component.StopListRow
import com.mbta.tid.mbta_app.android.component.StopPlacement
import com.mbta.tid.mbta_app.android.stopDetails.TripRouteAccents
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteDetailsStopList

@Composable
fun CollapsableStopList(
    lineOrRoute: RouteCardData.LineOrRoute,
    segment: RouteDetailsStopList.Segment,
    onClick: (RouteDetailsStopList.Entry) -> Unit,
    onClickLabel: @Composable (RouteDetailsStopList.Entry) -> String? = { null },
    isFirstSegment: Boolean = false,
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
            stopListContext = StopListContext.RouteDetails,
            modifier =
                Modifier.minimumInteractiveComponentSize().background(colorResource(R.color.fill1)),
            connectingRoutes = stop.connectingRoutes,
            onClickLabel = onClickLabel(stop),
            stopPlacement = StopPlacement(isFirstSegment, isLastSegment, false),
            descriptor = {
                Text(stringResource(R.string.less_common_stop), style = Typography.footnote)
            },
            rightSideContent = { modifier -> rightSideContent(stop, modifier) },
        )
    } else {
        Column {
            Row(
                Modifier.height(IntrinsicSize.Min)
                    .clickable(
                        onClickLabel =
                            stringResource(
                                if (stopsExpanded) R.string.collapse_stops
                                else R.string.expand_stops
                            ),
                        role = Role.Button,
                    ) {
                        stopsExpanded = !stopsExpanded
                    }
                    .padding(horizontal = 16.dp)
                    .defaultMinSize(minHeight = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedContent(
                    stopsExpanded,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(500)) togetherWith
                            fadeOut(animationSpec = tween(500))
                    },
                ) { stopsExpanded ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(start = 22.dp).width(20.dp),
                    ) {
                        if (stopsExpanded) {
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
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 12.dp),
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
            }

            if (!isLastSegment) {
                HaloSeparator()
            }

            if (stopsExpanded) {
                segment.stops.forEachIndexed { index, stop ->
                    StopListRow(
                        stop.stop,
                        onClick = { onClick(stop) },
                        routeAccents = TripRouteAccents(lineOrRoute.sortRoute),
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
                                false,
                            ),
                        rightSideContent = { modifier -> rightSideContent(stop, modifier) },
                    )
                }
            }
        }
    }
}
