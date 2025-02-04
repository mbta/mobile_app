package com.mbta.tid.mbta_app.android.util.modifiers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.haloContainer(
    borderWidth: Dp,
    outlineColor: Color = MaterialTheme.colorScheme.outline,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    borderRadius: Dp = 8.dp
): Modifier {
    return border(borderWidth, outlineColor, RoundedCornerShape(borderWidth + borderRadius))
        .padding(borderWidth)
        .background(backgroundColor, RoundedCornerShape(borderRadius))
        .clip(RoundedCornerShape(borderRadius))
}
