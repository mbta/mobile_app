package com.mbta.tid.mbta_app.android.routeDetails

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.PinButton
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteDetailsStopList
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext

@Composable
fun RouteDetailsView(
    selectionId: String,
    context: RouteDetailsContext,
    onOpenStopDetails: (String) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    errorBannerViewModel: ErrorBannerViewModel,
) {
    val globalData = getGlobalData("RouteDetailsView.globalData")
    val lineOrRoute = globalData?.let { RouteDetailsStopList.getLineOrRoute(selectionId, it) }
    if (lineOrRoute == null) {
        LoadingRouteStopListView(context, errorBannerViewModel)
        return
    }

    RouteStopListView(
        lineOrRoute,
        context,
        globalData,
        onClick = { onOpenStopDetails(it.stop.id) },
        onBack = onBack,
        onClose = onClose,
        errorBannerViewModel,
        defaultSelectedRouteId = selectionId.takeUnless { it == lineOrRoute.id },
        rightSideContent = { rowContext, modifier ->
            when (rowContext) {
                is RouteDetailsRowContext.Details ->
                    Image(
                        painterResource(id = R.drawable.baseline_chevron_right_24),
                        contentDescription = null,
                        modifier = modifier.width(8.dp),
                        colorFilter = ColorFilter.tint(colorResource(R.color.deemphasized)),
                    )
                is RouteDetailsRowContext.Favorites ->
                    PinButton(
                        rowContext.isFavorited,
                        colorResource(R.color.text),
                        rowContext.onTapStar,
                    )
            }
        },
    )
}

@Composable
private fun LoadingRouteStopListView(
    context: RouteDetailsContext,
    errorBannerViewModel: ErrorBannerViewModel,
) {
    CompositionLocalProvider(IsLoadingSheetContents provides true) {
        val mockRoute = RouteCardData.LineOrRoute.Route(ObjectCollectionBuilder.Single.route {})
        RouteStopListView(
            mockRoute,
            context,
            GlobalResponse(ObjectCollectionBuilder()),
            onClick = {},
            onBack = {},
            onClose = {},
            errorBannerViewModel,
            rightSideContent = { rowContext, modifier ->
                when (rowContext) {
                    is RouteDetailsRowContext.Details ->
                        Image(
                            painterResource(id = R.drawable.baseline_chevron_right_24),
                            contentDescription = null,
                            modifier = modifier.width(8.dp),
                            colorFilter = ColorFilter.tint(colorResource(R.color.deemphasized)),
                        )

                    is RouteDetailsRowContext.Favorites ->
                        PinButton(
                            rowContext.isFavorited,
                            colorResource(R.color.text),
                            rowContext.onTapStar,
                        )
                }
            },
        )
    }
}
