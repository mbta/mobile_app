package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.mbta.tid.mbta_app.android.R

@Composable
fun ColoredRouteLine(color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Canvas(modifier = Modifier.fillMaxHeight()) {
            val height = size.height
            drawRect(
                color = color,
                topLeft = Offset(x = -6f, y = 0f),
                size = Size(12f, height),
            )
        }
    }
}

@Composable
fun RouteLineTwist(color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.weight(1f)) { ColoredRouteLine(color) }

        Box() {
            Icon(
                painterResource(R.drawable.stop_trip_line_twist),
                contentDescription = null,
                tint = color
            )
            Icon(painterResource(R.drawable.stop_trip_line_twist_shadow), contentDescription = null)
        }

        Row(Modifier.weight(1f)) { ColoredRouteLine(color) }
    }
}
