package com.mbta.tid.mbta_app.android.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private operator fun LayoutDirection.unaryMinus() =
    when (this) {
        LayoutDirection.Ltr -> LayoutDirection.Rtl
        LayoutDirection.Rtl -> LayoutDirection.Ltr
    }

@Composable
fun ReverseRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable (RowScope.() -> Unit),
) {
    val originalLayoutDirection = LocalLayoutDirection.current
    val reverseLayoutDirection = -originalLayoutDirection
    CompositionLocalProvider(LocalLayoutDirection provides reverseLayoutDirection) {
        Row(modifier, horizontalArrangement, verticalAlignment) {
            CompositionLocalProvider(LocalLayoutDirection provides originalLayoutDirection) {
                content()
            }
        }
    }
}
