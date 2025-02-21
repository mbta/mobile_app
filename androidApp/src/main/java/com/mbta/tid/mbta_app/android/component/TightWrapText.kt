package com.mbta.tid.mbta_app.android.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import kotlin.math.ceil
import kotlin.math.floor

// Copied from here: https://issuetracker.google.com/issues/206039942#comment32
// Workaround for that bug, which causes all Text components with line breaks to take up
// the maximum available width, leaving a bunch of empty whitespace after the string.
@Composable
fun TightWrapText(text: String, modifier: Modifier = Modifier, style: TextStyle = TextStyle()) {
    var textLayoutResult: TextLayoutResult? by remember { mutableStateOf(null) }
    Text(
        text = text,
        modifier =
            modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val newTextLayoutResult = textLayoutResult

                if (newTextLayoutResult == null || newTextLayoutResult.lineCount == 0) {
                    // Default behavior if there is no text
                    layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
                } else {
                    val minX =
                        (0 until newTextLayoutResult.lineCount).minOf(
                            newTextLayoutResult::getLineLeft
                        )
                    val maxX =
                        (0 until newTextLayoutResult.lineCount).maxOf(
                            newTextLayoutResult::getLineRight
                        )

                    layout(ceil(maxX - minX).toInt(), placeable.height) {
                        placeable.place(-floor(minX).toInt(), 0)
                    }
                }
            },
        onTextLayout = { textLayoutResult = it },
        style = style
    )
}
