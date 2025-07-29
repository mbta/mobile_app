package com.mbta.tid.mbta_app.shape

object Path {
    data class Point(val x: Float, val y: Float)

    data class Rect(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float) {
        val width = maxX - minX
        val height = maxY - minY
    }
}
