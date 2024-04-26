package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.model.Route

@Composable
fun RoutePillSection(route: Route, body: @Composable () -> Unit) {
    Column {
        RoutePill(route)
        body()
    }
}
