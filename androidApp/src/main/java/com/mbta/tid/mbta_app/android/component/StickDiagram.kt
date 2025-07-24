package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.RouteBranchSegment
import com.mbta.tid.mbta_app.model.SegmentAlertState

@Composable
fun StickDiagram(
    color: Color,
    stickConnections: List<RouteBranchSegment.StickConnection>,
    modifier: Modifier = Modifier.Companion,
    getAlertState: (fromStop: String, toStop: String) -> SegmentAlertState = { _, _ ->
        SegmentAlertState.Normal
    },
) {
    val deemphasized = colorResource(R.color.deemphasized)
    Box(
        modifier.width(40.dp).height(IntrinsicSize.Max).drawWithCache {
            onDrawBehind {
                for (connection in stickConnections) {
                    val (color, pathEffect) =
                        when (getAlertState(connection.fromStop, connection.toStop)) {
                            SegmentAlertState.Normal -> Pair(color, null)
                            SegmentAlertState.Shuttle ->
                                Pair(
                                    color,
                                    PathEffect.Companion.dashPathEffect(
                                        floatArrayOf(8.dp.toPx(), 8.dp.toPx()),
                                        phase = 14.dp.toPx(),
                                    ),
                                )

                            SegmentAlertState.Suspension ->
                                Pair(
                                    deemphasized,
                                    PathEffect.Companion.dashPathEffect(
                                        floatArrayOf(8.dp.toPx(), 8.dp.toPx()),
                                        phase = 14.dp.toPx(),
                                    ),
                                )
                        }
                    drawPath(
                        connectionPath(connection),
                        color,
                        style = Stroke(stickWidth, pathEffect = pathEffect),
                    )
                }
            }
        }
    )
}

private fun DrawScope.connectionPath(connection: RouteBranchSegment.StickConnection) =
    Path().apply {
        val fromX = x(connection.fromLane)
        val toX = x(connection.toLane)
        val controlY = yMidpoint(connection.fromVPos, connection.toVPos)
        moveTo(fromX, y(connection.fromVPos))
        cubicTo(fromX, controlY, toX, controlY, toX, y(connection.toVPos))
    }

private fun DrawScope.x(lane: RouteBranchSegment.Lane) =
    when (lane) {
        RouteBranchSegment.Lane.Left -> 10 * density
        RouteBranchSegment.Lane.Center -> size.width / 2
        RouteBranchSegment.Lane.Right -> size.width - 10 * density
    }

private fun DrawScope.y(vPos: RouteBranchSegment.VPos) =
    when (vPos) {
        RouteBranchSegment.VPos.Top -> 0f
        RouteBranchSegment.VPos.Center -> size.height / 2
        RouteBranchSegment.VPos.Bottom -> size.height
    }

private fun DrawScope.yMidpoint(
    fromVPos: RouteBranchSegment.VPos,
    toVPos: RouteBranchSegment.VPos,
) = (y(fromVPos) + y(toVPos)) / 2

private val DrawScope.stickWidth
    get() = 4 * density

@Composable
fun RouteLineTwist(
    color: Color,
    modifier: Modifier = Modifier.Companion,
    proportionClosed: Float = 1.0f,
    connections: List<Pair<RouteBranchSegment.StickConnection, Boolean>>,
) {
    Box(
        modifier.width(40.dp).height(IntrinsicSize.Max).drawWithCache {
            onDrawBehind {
                val shadowColor = lerp(color, Color.Companion.Black, 0.15f)
                for ((connection, isTwisted) in connections) {
                    // when the twist is untwisted, we want to copy the top half and ignore the
                    // bottom half
                    val openFromVPos =
                        if (connection.fromVPos == RouteBranchSegment.VPos.Center)
                            RouteBranchSegment.VPos.Bottom
                        else connection.fromVPos
                    val openToVPos =
                        if (connection.toVPos == RouteBranchSegment.VPos.Center)
                            RouteBranchSegment.VPos.Bottom
                        else connection.toVPos
                    val opensToNothing = openFromVPos == openToVPos
                    if (opensToNothing && proportionClosed == 0f) continue
                    val topY = lerp(y(openFromVPos), y(connection.fromVPos), proportionClosed)
                    val bottomY = lerp(y(openToVPos), y(connection.toVPos), proportionClosed)
                    val centerY = (topY + bottomY) / 2
                    val topCenterX = x(connection.fromLane)
                    // when the twist is untwisted, we want to keep using the from lane so that
                    // the segment below the toggle lines up right
                    val bottomCenterX = lerp(topCenterX, x(connection.toLane), proportionClosed)
                    if (isTwisted) {
                        // we need to draw the shadow, then the curves with round caps, then the
                        // lines with default caps
                        val height = bottomY - topY
                        val twistSlantDY = height / 32 * proportionClosed
                        val nearTwistDY = height / 9 * proportionClosed
                        val curveStartDY = height / 5
                        val curveStartControlDY = size.height / 13
                        val curveEndControlDY = size.height / 20
                        val centerCenterX = (topCenterX + bottomCenterX) / 2
                        val twistSlantDX = size.width / 8 * proportionClosed
                        val nearTwistDX = size.width / 12 * proportionClosed
                        val curveEndControlDX = size.width / 15 * proportionClosed
                        val stickWidth =
                            if (opensToNothing) stickWidth * proportionClosed else stickWidth
                        drawPath(
                            Path().apply {
                                moveTo(centerCenterX + twistSlantDX, centerY + twistSlantDY)
                                lineTo(centerCenterX - twistSlantDX, centerY - twistSlantDY)
                            },
                            shadowColor,
                            style =
                                Stroke(
                                    stickWidth,
                                    cap = StrokeCap.Companion.Round,
                                    join = StrokeJoin.Companion.Round,
                                    pathEffect = null,
                                ),
                        )
                        drawPath(
                            Path().apply {
                                if (bottomY != size.height) {
                                    moveTo(bottomCenterX, bottomY)
                                    lineTo(bottomCenterX, bottomY - curveStartDY)
                                } else {
                                    moveTo(bottomCenterX, bottomY - curveStartDY)
                                }
                                cubicTo(
                                    bottomCenterX,
                                    bottomY - curveStartDY - curveStartControlDY,
                                    centerCenterX + nearTwistDX - curveEndControlDX,
                                    centerY + nearTwistDY + curveEndControlDY,
                                    centerCenterX + nearTwistDX,
                                    centerY + nearTwistDY,
                                )
                                lineTo(centerCenterX + twistSlantDX, centerY + twistSlantDY)
                                moveTo(centerCenterX - twistSlantDX, centerY - twistSlantDY)
                                lineTo(centerCenterX - nearTwistDX, centerY - nearTwistDY)
                                cubicTo(
                                    centerCenterX - nearTwistDX + curveEndControlDX,
                                    centerY - nearTwistDY - curveEndControlDY,
                                    topCenterX,
                                    topY + curveStartDY + curveStartControlDY,
                                    topCenterX,
                                    topY + curveStartDY,
                                )
                                if (topY != 0f) {
                                    lineTo(topCenterX, topY)
                                }
                            },
                            color,
                            style =
                                Stroke(
                                    stickWidth,
                                    cap = StrokeCap.Companion.Round,
                                    join = StrokeJoin.Companion.Round,
                                    pathEffect = null,
                                ),
                        )
                        drawPath(
                            Path().apply {
                                if (bottomY == size.height) {
                                    moveTo(bottomCenterX, bottomY)
                                    lineTo(bottomCenterX, bottomY - curveStartDY)
                                }
                                if (topY == 0f) {
                                    moveTo(topCenterX, topY + curveStartDY)
                                    lineTo(topCenterX, topY)
                                }
                            },
                            color,
                            style = Stroke(stickWidth, pathEffect = null),
                        )
                    } else {
                        drawPath(
                            Path().apply {
                                moveTo(topCenterX, topY)
                                cubicTo(
                                    topCenterX,
                                    centerY,
                                    bottomCenterX,
                                    centerY,
                                    bottomCenterX,
                                    bottomY,
                                )
                            },
                            color,
                            style = Stroke(stickWidth, pathEffect = null),
                        )
                    }
                }
            }
        }
    )
}
