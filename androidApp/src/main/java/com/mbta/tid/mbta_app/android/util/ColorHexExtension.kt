package com.mbta.tid.mbta_app.android.util

import androidx.compose.ui.graphics.Color

fun Color.Companion.fromHex(hexColor: String): Color {
    var color = hexColor.trimStart('#')
    val alpha =
        if (color.length == 8) {
            val a = color.substring(0, 2)
            color = color.substring(2)
            a.toInt(16)
        } else 255
    check(color.length == 6)
    val r = color.substring(0, 2)
    val g = color.substring(2, 4)
    val b = color.substring(4, 6)
    return Color(r.toInt(16), g.toInt(16), b.toInt(16), alpha)
}
