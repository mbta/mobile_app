package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R

@Composable
fun StopDot(routeAccents: TripRouteAccents, targeted: Boolean, modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier
                .background(routeAccents.color, CircleShape)
                .border(1.dp, colorResource(R.color.stop_dot_halo), CircleShape)
                .size(14.dp)
        ) {}

        if (targeted) {
            Image(
                painterResource(R.drawable.stop_pin_indicator),
                null,
                Modifier.padding(bottom = 32.dp),
            )
        }
    }
}
