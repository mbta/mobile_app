package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.mbta.tid.mbta_app.utils.EasternTimeInstant

@Composable
fun LoadingRouteCard() {
    val placeholderRouteData = LoadingPlaceholders.nearbyRoute()
    Column(modifier = Modifier.loadingShimmer()) {
        RouteCard(
            placeholderRouteData,
            null,
            EasternTimeInstant.now(),
            { false },
            showStopHeader = true,
            { _, _ -> },
        )
    }
}
