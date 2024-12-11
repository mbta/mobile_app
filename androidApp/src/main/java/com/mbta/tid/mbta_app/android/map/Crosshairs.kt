package com.mbta.tid.mbta_app.android.map

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.mbta.tid.mbta_app.android.R

@Composable
fun Crosshairs() {
    Column {
        Icon(
            painterResource(R.drawable.map_nearby_location_cursor),
            contentDescription = null,
            tint = Color.Unspecified
        )
    }
}
