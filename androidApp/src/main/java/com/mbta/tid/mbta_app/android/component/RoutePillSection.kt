package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.Route

@Composable
fun RoutePillSection(route: Route, body: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .background(color = Color.fromHex(route.color))
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        RoutePill(route)
    }
    body()
}
