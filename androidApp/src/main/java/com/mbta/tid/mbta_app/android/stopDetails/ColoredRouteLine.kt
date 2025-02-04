package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R

@Composable
fun ColoredRouteLine(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.width(4.dp).height(IntrinsicSize.Max).background(color))
}

@Composable
fun RouteLineTwist(color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ColoredRouteLine(color, Modifier.weight(1f))

        Box {
            Icon(
                painterResource(R.drawable.stop_trip_line_twist),
                contentDescription = null,
                tint = color
            )
            Icon(painterResource(R.drawable.stop_trip_line_twist_shadow), contentDescription = null)
        }

        ColoredRouteLine(color, Modifier.weight(1f))
    }
}
