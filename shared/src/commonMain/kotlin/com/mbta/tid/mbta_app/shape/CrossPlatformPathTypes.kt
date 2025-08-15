package com.mbta.tid.mbta_app.shape

public object Path {
    public data class Point(val x: Float, val y: Float)

    public data class Rect(
        internal val minX: Float,
        internal val maxX: Float,
        internal val minY: Float,
        internal val maxY: Float,
    ) {
        internal val width: Float = maxX - minX
        internal val height: Float = maxY - minY
    }
}

internal fun lerp(x1: Float, x2: Float, t: Float) = x1 * (1 - t) + x2 * t
