package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyRouteView
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import kotlinx.datetime.Clock

@Composable
fun LoadingRouteCard() {
    val placeholderRouteData = LoadingPlaceholders.nearbyRoute()
    Column(modifier = Modifier.loadingShimmer()) {
        NearbyRouteView(placeholderRouteData, false, {}, Clock.System.now(), { _, _ -> })
    }
}
