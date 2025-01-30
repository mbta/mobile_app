package com.mbta.tid.mbta_app.android.util

import androidx.compose.ui.graphics.Color

fun Color.Companion.fromHex(hexColor: String): Color {
    var rawHex = hexColor.removePrefix("#").lowercase()
    var alpha = "ff"
    if (rawHex.length == 8) {
        alpha = rawHex.substring(0, 2)
        rawHex = rawHex.substring(2)
    }
    check(rawHex.length == 6)
    val r = rawHex.substring(0, 2)
    val g = rawHex.substring(2, 4)
    val b = rawHex.substring(4, 6)
    return Color(r.toInt(16), g.toInt(16), b.toInt(16), alpha.toInt(16))
}

fun Color.toHex(): String {
    val alpha = this.alpha * 255
    val r = this.red * 255
    val g = this.green * 255
    val b = this.blue * 255
    return String.format("#%02x%02x%02x%02x", alpha.toInt(), r.toInt(), g.toInt(), b.toInt())
}
