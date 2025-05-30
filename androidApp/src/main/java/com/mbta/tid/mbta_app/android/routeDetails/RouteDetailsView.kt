package com.mbta.tid.mbta_app.android.routeDetails

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.model.RouteDetailsStopList

@Composable
fun RouteDetailsView(
    selectionId: String,
    onOpenStopDetails: (String) -> Unit,
    onClose: () -> Unit,
    errorBannerViewModel: ErrorBannerViewModel,
) {
    val globalData = getGlobalData("RouteDetailsView.globalData")
    val lineOrRoute = globalData?.let { RouteDetailsStopList.getLineOrRoute(selectionId, it) }
    if (lineOrRoute == null) {
        CircularProgressIndicator()
        return
    }

    RouteStopListView(
        lineOrRoute,
        globalData,
        onClick = { onOpenStopDetails(it.stop.id) },
        onClose = onClose,
        errorBannerViewModel,
        defaultSelectedRouteId = selectionId.takeUnless { it == lineOrRoute.id },
        rightSideContent = { _, modifier ->
            Image(
                painterResource(id = R.drawable.baseline_chevron_right_24),
                contentDescription = null,
                modifier = modifier.width(8.dp),
                colorFilter = ColorFilter.tint(colorResource(R.color.deemphasized)),
            )
        },
    )
}
