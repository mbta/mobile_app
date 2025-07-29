package com.mbta.tid.mbta_app.shape

import com.mbta.tid.mbta_app.model.RouteBranchSegment
import kotlin.math.abs

object StickDiagramShapes {
    data class Connection(
        val start: Path.Point,
        val startControl: Path.Point,
        val endControl: Path.Point,
        val end: Path.Point,
    )

    private data class TwistBase(
        val topCenterX: Float,
        val bottomCenterX: Float,
        val topY: Float,
        val bottomY: Float,
        val opensToNothing: Boolean,
    ) {
        val centerY = (topY + bottomY) / 2
    }

    data class NonTwisted(
        val start: Path.Point,
        val startControl: Path.Point,
        val endControl: Path.Point,
        val end: Path.Point,
    )

    data class Twisted(
        val shadow: Shadow,
        val curves: Curves,
        val ends: Ends,
        val opensToNothing: Boolean,
    ) {
        data class Shadow(val start: Path.Point, val end: Path.Point)

        data class Curves(
            val bottom: Path.Point?,
            val bottomCurveStart: Path.Point,
            val bottomCurveStartControl: Path.Point,
            val bottomNearTwistControl: Path.Point,
            val bottomNearTwist: Path.Point,
            val shadowStart: Path.Point,
            val shadowEnd: Path.Point,
            val topNearTwist: Path.Point,
            val topNearTwistControl: Path.Point,
            val topCurveStartControl: Path.Point,
            val topCurveStart: Path.Point,
            val top: Path.Point?,
        )

        data class Ends(
            val bottom: Path.Point?,
            val bottomCurveStart: Path.Point,
            val topCurveStart: Path.Point,
            val top: Path.Point?,
        )
    }

    private fun x(lane: RouteBranchSegment.Lane, rect: Path.Rect): Float =
        when (lane) {
            RouteBranchSegment.Lane.Left -> rect.minX + rect.width / 4
            RouteBranchSegment.Lane.Center -> rect.minX + rect.width / 2
            RouteBranchSegment.Lane.Right -> rect.maxX - rect.width / 4
        }

    private fun y(vPos: RouteBranchSegment.VPos, rect: Path.Rect): Float =
        when (vPos) {
            RouteBranchSegment.VPos.Top -> rect.minY
            RouteBranchSegment.VPos.Center -> rect.minY + rect.height / 2
            RouteBranchSegment.VPos.Bottom -> rect.maxY
        }

    private fun lerp(x1: Float, x2: Float, t: Float) = x1 * (1 - t) + x2 * t

    fun connection(connection: RouteBranchSegment.StickConnection, rect: Path.Rect): Connection {
        val fromX = x(connection.fromLane, rect)
        val toX = x(connection.toLane, rect)
        val fromY = y(connection.fromVPos, rect)
        val toY = y(connection.toVPos, rect)
        val controlY = (fromY + toY) / 2
        return Connection(
            start = Path.Point(fromX, fromY),
            startControl = Path.Point(fromX, controlY),
            endControl = Path.Point(toX, controlY),
            end = Path.Point(toX, toY),
        )
    }

    private fun twistBase(
        connection: RouteBranchSegment.StickConnection,
        rect: Path.Rect,
        proportionClosed: Float,
    ): TwistBase? {
        // when the twist is untwisted, we want to copy the top half and ignore the bottom half
        val openFromVPos =
            if (connection.fromVPos == RouteBranchSegment.VPos.Center)
                RouteBranchSegment.VPos.Bottom
            else connection.fromVPos
        val openToVPos =
            if (connection.toVPos == RouteBranchSegment.VPos.Center) RouteBranchSegment.VPos.Bottom
            else connection.toVPos
        val opensToNothing = openFromVPos == openToVPos
        if (opensToNothing && proportionClosed == 0f) return null
        val topY = lerp(y(openFromVPos, rect), y(connection.fromVPos, rect), proportionClosed)
        val bottomY = lerp(y(openToVPos, rect), y(connection.toVPos, rect), proportionClosed)
        val topCenterX = x(connection.fromLane, rect)
        // when the twist is untwisted, we want to keep using the from lane so that the segment
        // below the toggle lines up right
        val bottomCenterX = lerp(topCenterX, x(connection.toLane, rect), proportionClosed)
        return TwistBase(
            topCenterX = topCenterX,
            bottomCenterX = bottomCenterX,
            topY = topY,
            bottomY = bottomY,
            opensToNothing = opensToNothing,
        )
    }

    fun nonTwisted(
        connection: RouteBranchSegment.StickConnection,
        rect: Path.Rect,
        proportionClosed: Float,
    ): NonTwisted? {
        val base = twistBase(connection, rect, proportionClosed) ?: return null
        return NonTwisted(
            start = Path.Point(base.topCenterX, base.topY),
            startControl = Path.Point(base.topCenterX, base.centerY),
            endControl = Path.Point(base.bottomCenterX, base.centerY),
            end = Path.Point(base.bottomCenterX, base.bottomY),
        )
    }

    fun twisted(
        connection: RouteBranchSegment.StickConnection,
        rect: Path.Rect,
        proportionClosed: Float,
    ): Twisted? {
        val base = twistBase(connection, rect, proportionClosed) ?: return null
        val height = base.bottomY - base.topY
        val twistSlantDY = height / 32 * proportionClosed
        val nearTwistDY = height / 9 * proportionClosed
        val curveStartDY = height / 5
        val curveStartControlDY = rect.height / 13
        val curveEndControlDY = rect.height / 20
        val centerCenterX = (base.topCenterX + base.bottomCenterX) / 2
        val twistSlantDX = rect.width / 8 * proportionClosed
        val nearTwistDX = rect.width / 12 * proportionClosed
        val curveEndControlDX = rect.width / 15 * proportionClosed
        val shadowStart = Path.Point(centerCenterX + twistSlantDX, base.centerY + twistSlantDY)
        val shadowEnd = Path.Point(centerCenterX - twistSlantDX, base.centerY - twistSlantDY)
        val shadow = Twisted.Shadow(start = shadowStart, end = shadowEnd)
        val bottom = Path.Point(base.bottomCenterX, base.bottomY)
        val bottomCurveStart = Path.Point(base.bottomCenterX, base.bottomY - curveStartDY)
        val topCurveStart = Path.Point(base.topCenterX, base.topY + curveStartDY)
        val top = Path.Point(base.topCenterX, base.topY)
        val roundedBottom = abs(rect.maxY - base.bottomY) > 1
        val roundedTop = abs(base.topY - rect.minY) > 1
        val curves =
            Twisted.Curves(
                bottom = bottom.takeIf { roundedBottom },
                bottomCurveStart = bottomCurveStart,
                bottomCurveStartControl =
                    Path.Point(
                        base.bottomCenterX,
                        base.bottomY - curveStartDY - curveStartControlDY,
                    ),
                bottomNearTwistControl =
                    Path.Point(
                        centerCenterX + nearTwistDX - curveEndControlDX,
                        base.centerY + nearTwistDY + curveEndControlDY,
                    ),
                bottomNearTwist =
                    Path.Point(centerCenterX + nearTwistDX, base.centerY + nearTwistDY),
                shadowStart = shadowStart,
                shadowEnd = shadowEnd,
                topNearTwist = Path.Point(centerCenterX - nearTwistDX, base.centerY - nearTwistDY),
                topNearTwistControl =
                    Path.Point(
                        centerCenterX - nearTwistDX + curveEndControlDX,
                        base.centerY - nearTwistDY - curveEndControlDY,
                    ),
                topCurveStartControl =
                    Path.Point(base.topCenterX, base.topY + curveStartDY + curveStartControlDY),
                topCurveStart = topCurveStart,
                top = top.takeIf { roundedTop },
            )
        val ends =
            Twisted.Ends(
                bottom = bottom.takeIf { !roundedBottom },
                bottomCurveStart = bottomCurveStart,
                topCurveStart = topCurveStart,
                top = top.takeIf { !roundedTop },
            )
        return Twisted(shadow, curves, ends, opensToNothing = base.opensToNothing)
    }
}
