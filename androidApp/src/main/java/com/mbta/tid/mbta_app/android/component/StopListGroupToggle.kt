package com.mbta.tid.mbta_app.android.component

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.stopDetails.TripRouteAccents
import com.mbta.tid.mbta_app.model.RouteBranchSegment

@Composable
fun StopListGroupToggle(
    stopsExpanded: Boolean,
    setStopsExpanded: (Boolean) -> Unit,
    contentDescription: String?,
    onClickLabel: String,
    routeAccents: TripRouteAccents,
    modifier: Modifier = Modifier,
    stickConnections: List<Pair<RouteBranchSegment.StickConnection, Boolean>> =
        listOf(
            Pair(
                RouteBranchSegment.StickConnection(
                    fromStop = "",
                    toStop = "",
                    fromLane = RouteBranchSegment.Lane.Center,
                    toLane = RouteBranchSegment.Lane.Center,
                    fromVPos = RouteBranchSegment.VPos.Top,
                    toVPos = RouteBranchSegment.VPos.Bottom,
                ),
                true,
            )
        ),
    label: @Composable RowScope.() -> Unit,
) {
    val hideTwist =
        stickConnections.any { (connection, twisted) ->
            twisted &&
                (connection.fromVPos != RouteBranchSegment.VPos.Top ||
                    connection.toVPos != RouteBranchSegment.VPos.Bottom)
        }
    Row(
        modifier
            .height(IntrinsicSize.Min)
            .clickable(onClickLabel = onClickLabel) { setStopsExpanded(!stopsExpanded) }
            .then(
                if (contentDescription != null)
                    Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
                else Modifier
            )
            .padding(horizontal = 8.dp)
            .defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val stopsExpandedTransition = updateTransition(stopsExpanded, label = "stops expanded")
        val twistFactor by
            stopsExpandedTransition.animateFloat(
                transitionSpec = { tween(500) },
                label = "twistFactor",
            ) {
                if (it) 0f else 1f
            }
        val iconAngle by
            stopsExpandedTransition.animateFloat(
                transitionSpec = { tween(500) },
                label = "iconAngle",
            ) {
                if (it) 90f else 0f
            }
        if (hideTwist) {
            Box(modifier = modifier.size(34.dp), contentAlignment = Alignment.Center) {
                if (stopsExpanded) {
                    Box(
                        modifier =
                            modifier
                                .semantics(mergeDescendants = true) {}
                                .size(34.dp)
                                .background(colorResource(R.color.halo), CircleShape)
                                .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(32.dp)
                                .background(
                                    color = colorResource(R.color.fill2),
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {}
                    }
                }
                Icon(
                    painterResource(R.drawable.fa_caret_right),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp).rotate(iconAngle),
                    tint = colorResource(R.color.deemphasized),
                )
            }
        } else {
            Column(Modifier.fillMaxHeight()) {
                RouteLineTwist(
                    routeAccents.color,
                    Modifier.weight(1f),
                    proportionClosed = twistFactor,
                    connections = stickConnections,
                )
            }
            Icon(
                painterResource(R.drawable.fa_caret_right),
                contentDescription = null,
                modifier = Modifier.size(12.dp).rotate(iconAngle),
                tint = colorResource(R.color.deemphasized),
            )
        }
        label()
    }
}
