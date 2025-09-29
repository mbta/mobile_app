package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun WithRealtimeIndicator(
    modifier: Modifier = Modifier,
    hideIndicator: Boolean = false,
    alignment: Alignment.Horizontal = Alignment.End,
    prediction: @Composable RowScope.() -> Unit,
) {
    val subjectSpacing = 4.dp
    val iconSize = 20.dp

    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(subjectSpacing, alignment),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!hideIndicator) {
            LiveIcon(
                Modifier.padding(4.dp).testTag("realtimeIndicator"),
                size = iconSize,
                alpha = 0.6f,
            )
        }
        prediction()
    }
}
