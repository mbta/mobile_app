package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading

@Composable
fun LiveIcon(modifier: Modifier = Modifier, size: Dp = 16.dp, alpha: Float = 1f) {
    Image(
        painterResource(R.drawable.live_data),
        null,
        Modifier.placeholderIfLoading().size(size).then(modifier),
        alpha = alpha,
        colorFilter = ColorFilter.tint(LocalContentColor.current),
    )
}
