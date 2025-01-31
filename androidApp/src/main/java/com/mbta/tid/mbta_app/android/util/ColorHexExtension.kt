package com.mbta.tid.mbta_app.android.util

import androidx.compose.ui.graphics.Color

fun Color.Companion.fromHex(hexColor: String): Color {
    check(hexColor.length == 6)
    val r = hexColor.substring(0, 2)
    val g = hexColor.substring(2, 4)
    val b = hexColor.substring(4, 6)
    return Color(r.toInt(16), g.toInt(16), b.toInt(16))
}
