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
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.RouteBranchSegment
import com.mbta.tid.mbta_app.model.SegmentAlertState
import com.mbta.tid.mbta_app.shape.Path as SharedPath
import com.mbta.tid.mbta_app.shape.StickDiagramShapes

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
                    val shape = StickDiagramShapes.connection(connection, rect)
                    drawPath(
                        Path().apply {
                            moveTo(shape.start)
                            cubicTo(shape.startControl, shape.endControl, shape.end)
                        },
                        color,
                        style = Stroke(stickWidth, pathEffect = pathEffect),
                    )
                }
            }
        }
    )
}

private val DrawScope.rect: SharedPath.Rect
    get() = SharedPath.Rect(0f, size.width, 0f, size.height)

private fun Path.moveTo(point: SharedPath.Point) {
    moveTo(point.x, point.y)
}

private fun Path.lineTo(point: SharedPath.Point) {
    lineTo(point.x, point.y)
}

private fun Path.quadraticTo(control: SharedPath.Point, to: SharedPath.Point) {
    quadraticTo(control.x, control.y, to.x, to.y)
}

private fun Path.cubicTo(
    control1: SharedPath.Point,
    control2: SharedPath.Point,
    to: SharedPath.Point,
) {
    cubicTo(control1.x, control1.y, control2.x, control2.y, to.x, to.y)
}

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
                val shadowColor = lerp(color, Color.Companion.Black, 0.15f * proportionClosed)
                for ((connection, isTwisted) in connections) {
                    if (isTwisted) {
                        // we need to draw the shadow, then the curves with round caps, then the
                        // lines with default caps
                        val shape =
                            StickDiagramShapes.twisted(connection, rect, proportionClosed)
                                ?: continue
                        val stickWidth =
                            if (shape.opensToNothing) stickWidth * proportionClosed else stickWidth
                        drawPath(
                            Path().apply {
                                moveTo(shape.shadow.start)
                                lineTo(shape.shadow.end)
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
                                when (val bottom = shape.curves.bottom) {
                                    null -> moveTo(shape.curves.bottomCurveStart)
                                    else -> {
                                        moveTo(bottom)
                                        lineTo(shape.curves.bottomCurveStart)
                                    }
                                }
                                quadraticTo(
                                    shape.curves.bottomCurveControl,
                                    shape.curves.shadowStart,
                                )
                                lineTo(shape.curves.shadowStart)
                                moveTo(shape.curves.shadowEnd)
                                quadraticTo(
                                    shape.curves.topCurveControl,
                                    shape.curves.topCurveStart,
                                )
                                when (val top = shape.curves.top) {
                                    null -> {}
                                    else -> lineTo(top)
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
                                when (val bottom = shape.ends.bottom) {
                                    null -> {}
                                    else -> {
                                        moveTo(bottom)
                                        lineTo(shape.ends.bottomCurveStart)
                                    }
                                }
                                when (val top = shape.ends.top) {
                                    null -> {}
                                    else -> {
                                        moveTo(shape.ends.topCurveStart)
                                        lineTo(top)
                                    }
                                }
                            },
                            color,
                            style = Stroke(stickWidth, pathEffect = null),
                        )
                    } else {
                        val shape =
                            StickDiagramShapes.nonTwisted(connection, rect, proportionClosed)
                                ?: continue
                        drawPath(
                            Path().apply {
                                moveTo(shape.start)
                                cubicTo(shape.startControl, shape.endControl, shape.end)
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
