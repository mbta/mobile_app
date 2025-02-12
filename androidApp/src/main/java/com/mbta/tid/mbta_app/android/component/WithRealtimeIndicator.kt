package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R

@Composable
fun WithRealtimeIndicator(
    modifier: Modifier = Modifier,
    hideIndicator: Boolean = false,
    prediction: @Composable RowScope.() -> Unit
) {
    val subjectSpacing = 4.dp
    val iconSize = 20.dp

    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(subjectSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!hideIndicator) {
            Image(
                painterResource(R.drawable.live_data),
                contentDescription = null,
                modifier = Modifier.size(iconSize).padding(4.dp).testTag("realtimeIndicator"),
                alpha = 0.6f,
                colorFilter = ColorFilter.tint(LocalContentColor.current)
            )
        }
        prediction()
    }
}
