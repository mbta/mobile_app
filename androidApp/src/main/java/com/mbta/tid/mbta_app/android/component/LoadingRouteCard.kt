package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.nearbyTransit.NearbyRouteView
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.valentinilk.shimmer.shimmer
import kotlinx.datetime.Clock

@Composable
fun LoadingRouteCard() {
    val placeholderRouteData = LoadingPlaceholders.nearbyRoute()

    val contentDesc = stringResource(R.string.loading)

    Column(
        modifier = Modifier.shimmer().clearAndSetSemantics { contentDescription = contentDesc }
    ) {
        NearbyRouteView(placeholderRouteData, false, {}, Clock.System.now(), { _, _ -> })
    }
}
