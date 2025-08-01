package com.mbta.tid.mbta_app.android.util

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.mbta.tid.mbta_app.shape.Path as SharedPath

val DrawScope.rect: SharedPath.Rect
    get() = SharedPath.Rect(0f, size.width, 0f, size.height)

fun Path.moveTo(point: SharedPath.Point) {
    moveTo(point.x, point.y)
}

fun Path.lineTo(point: SharedPath.Point) {
    lineTo(point.x, point.y)
}

fun Path.quadraticTo(control: SharedPath.Point, to: SharedPath.Point) {
    quadraticTo(control.x, control.y, to.x, to.y)
}

fun Path.cubicTo(control1: SharedPath.Point, control2: SharedPath.Point, to: SharedPath.Point) {
    cubicTo(control1.x, control1.y, control2.x, control2.y, to.x, to.y)
}
