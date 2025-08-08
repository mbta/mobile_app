package com.mbta.tid.mbta_app.map

/**
 * A set of colors to use, specific to light or dark mode. Expects colors as `#ABCDEF` hex strings.
 */
public data class ColorPalette
internal constructor(val deemphasized: String, val fill3: String, val text: String) {
    public companion object {
        public val light: ColorPalette =
            ColorPalette(deemphasized = "#8A9199", fill3 = "#FFFFFF", text = "#192026")
        public val dark: ColorPalette =
            ColorPalette(deemphasized = "#8A9199", fill3 = "#000000", text = "#E5E5E3")
    }
}
