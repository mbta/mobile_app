package com.mbta.tid.mbta_app.shape

import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

public class FavoriteStarShape(rect: Path.Rect, scale: Float = 1f) {
    private val starPoints = 5
    private val phaseShift = 0.25f
    private val diameter = min(rect.width, rect.height) * scale
    private val centerX = (rect.minX + rect.maxX) / 2
    private val centerY = (rect.minY + rect.maxY) / 2
    // painstakingly reverse engineered from the original SVG
    private val innerRadius = 0.27618116f
    private val outerInnerRadius = 0.45414436f
    private val outerInnerAngleOffset = 0.09f
    private val outerInnerControlRadius = 0.47204426f
    private val outerInnerControlAngleOffset = 0.07f
    private val outerOuterControlRadius = 0.4828321f
    private val outerOuterControlAngleOffset = 0.03f
    private val outerOuterRadius = 0.48234966f

    private fun point(relativeRadius: Float, turn: Float): Path.Point {
        val absoluteRadius = relativeRadius * diameter
        val angle = turn * 2 * PI
        return Path.Point(
            centerX + absoluteRadius * cos(angle),
            centerY + absoluteRadius * sin(angle),
        )
    }

    public data class Arm(
        val `in`: Path.Point,
        val outStart: Path.Point,
        val outStartControl: Path.Point,
        val outMidStartControl: Path.Point,
        val outMid: Path.Point,
        val outMidEndControl: Path.Point,
        val outEndControl: Path.Point,
        val outEnd: Path.Point,
    )

    public val arms: List<Arm> =
        (1..5).map {
            val i = it.toFloat()
            val midI = i + 0.5f
            Arm(
                `in` = point(innerRadius, (phaseShift + i) / starPoints),
                outStart =
                    point(
                        outerInnerRadius,
                        (phaseShift + midI - outerInnerAngleOffset) / starPoints,
                    ),
                outStartControl =
                    point(
                        outerInnerControlRadius,
                        (phaseShift + midI - outerInnerControlAngleOffset) / starPoints,
                    ),
                outMidStartControl =
                    point(
                        outerOuterControlRadius,
                        (phaseShift + midI - outerOuterControlAngleOffset) / starPoints,
                    ),
                outMid = point(outerOuterRadius, (phaseShift + midI) / starPoints),
                outMidEndControl =
                    point(
                        outerOuterControlRadius,
                        (phaseShift + midI + outerOuterControlAngleOffset) / starPoints,
                    ),
                outEndControl =
                    point(
                        outerInnerControlRadius,
                        (phaseShift + midI + outerInnerControlAngleOffset) / starPoints,
                    ),
                outEnd =
                    point(
                        outerInnerRadius,
                        (phaseShift + midI + outerInnerAngleOffset) / starPoints,
                    ),
            )
        }

    internal companion object {
        private const val PI = kotlin.math.PI.toFloat()
    }
}
