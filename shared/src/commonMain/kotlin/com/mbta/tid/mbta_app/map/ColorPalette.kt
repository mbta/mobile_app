package com.mbta.tid.mbta_app.map

/**
 * A set of colors to use, specific to light or dark mode. Expects colors as `#ABCDEF` hex strings.
 */
data class ColorPalette(val deemphasized: String, val fill3: String, val text: String) {
    companion object {
        val light = ColorPalette(deemphasized = "#8A9199", fill3 = "#FFFFFF", text = "#192026")
        val dark = ColorPalette(deemphasized = "#8A9199", fill3 = "#000000", text = "#E5E5E3")
    }
}
