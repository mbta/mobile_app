package com.mbta.tid.mbta_app.android.component.stopCard

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.android.util.modifiers.loading
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.mbta.tid.mbta_app.model.StopCardData
import com.mbta.tid.mbta_app.utils.EasternTimeInstant

@Composable
fun LoadingStopCard() {
    val placeholderRouteData = LoadingPlaceholders.nearbyRoute()
    val placeholderStopData =
        StopCardData.fromRouteCardData(listOf(placeholderRouteData), sortByDistanceFrom = null)
    Column(modifier = Modifier.loading(withShimmer = false)) {
        StopCard(
            placeholderStopData.first(),
            null,
            EasternTimeInstant.now(),
            { false },
            { _, _ -> },
        )
    }
}
