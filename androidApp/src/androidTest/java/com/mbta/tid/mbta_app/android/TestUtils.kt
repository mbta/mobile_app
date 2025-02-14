package com.mbta.tid.mbta_app.android

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import kotlin.math.abs
import org.junit.Assert.fail

fun hasClickActionLabel(expected: String?) =
    SemanticsMatcher("has click action label $expected") { node ->
        node.config[SemanticsActions.OnClick].label == expected
    }

@OptIn(ExperimentalStdlibApi::class)
private fun colorToHex(color: Color): String {
    fun valToHex(value: Float): String {
        val byteValue = (value * 255).toInt().toByte()
        return byteValue.toHexString(HexFormat.UpperCase)
    }
    return "#${valToHex(color.red)}${valToHex(color.green)}${valToHex(color.blue)}"
}

private fun Color.isGrey(): Boolean =
    abs(this.red - this.green) < 0.01 && abs(this.green - this.blue) < 0.01

/**
 * It's difficult to determine if a view hierarchy contains the correct image when the image is
 * semantically invisible, but it's easy to capture a screenshot of the view and check it pixel by
 * pixel.
 */
fun SemanticsNodeInteraction.assertHasColor(targetColor: Color) {
    val pixelMap = this.captureToImage().toPixelMap()
    val pixelCounts = mutableMapOf<Color, Int>()
    for (x in 0.rangeUntil(pixelMap.width)) {
        for (y in 0.rangeUntil(pixelMap.height)) {
            val colorHere = pixelMap[x, y]
            if (colorHere == targetColor) {
                return
            }
            pixelCounts[colorHere] = pixelCounts.getOrDefault(colorHere, 0) + 1
        }
    }
    // never returned so did not find the target. hopefully the color that is present instead of the
    // target is either the most common non-grey color or among the most common grey colors (the
    // most common grey will likely be a background, though)
    val legibleResult =
        pixelCounts.entries
            .groupBy(
                { if (it.key.isGrey()) "grey" else "not grey" },
                { colorToHex(it.key) to it.value }
            )
            .mapValues { it.value.sortedByDescending { it.second } }
            .toSortedMap(
                compareBy {
                    when (it) {
                        "not grey" -> 1
                        "grey" -> 2
                        else -> 3
                    }
                }
            )

    fail("Did not contain specified color, but did have $legibleResult")
}
