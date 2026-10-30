package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.generated.drawableByName

@Composable
fun StopIconDot(iconName: String, targeted: Boolean, imageSize: Dp, modifier: Modifier = Modifier) {
    val icon = painterResource(drawableByName(iconName))
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(icon, null, Modifier.size(imageSize))

        if (targeted) {
            Image(
                painterResource(R.drawable.stop_pin_indicator),
                null,
                Modifier.padding(bottom = 32.dp),
            )
        }
    }
}
