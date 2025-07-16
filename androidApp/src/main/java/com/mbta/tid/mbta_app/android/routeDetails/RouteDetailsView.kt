package com.mbta.tid.mbta_app.android.routeDetails

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.StarIcon
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.model.RouteDetailsStopList
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
    val lineOrRoute =
        globalData?.let {
            println("KB: selectionId ${selectionId}")
            RouteDetailsStopList.getLineOrRoute(selectionId, it)
        }
    if (lineOrRoute == null) {
        println("KB: lineOrRoute is null for some reason")
        LoadingRouteStopListView(context, errorBannerViewModel)
        return
    }

    RouteStopListView(
        lineOrRoute,
        context,
        globalData,
        onClick = {
            when (it) {
                is RouteDetailsRowContext.Details -> onOpenStopDetails(it.stop.id)
                is RouteDetailsRowContext.Favorites -> it.onTapStar()
            }
        },
        onClickLabel = {
            when (it) {
                is RouteDetailsRowContext.Details -> null
                is RouteDetailsRowContext.Favorites ->
                    if (it.isFavorited) {
                        stringResource(R.string.remove_favorite)
                    } else {
                        stringResource(R.string.add_favorite)
                    }
            }
        },
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
                is RouteDetailsRowContext.Favorites -> {
                    val favoriteCD = stringResource(R.string.favorite_stop)
                    StarIcon(
                        rowContext.isFavorited,
                        colorResource(R.color.text),
                        Modifier.semantics {
                            contentDescription = if (rowContext.isFavorited) favoriteCD else ""
                        },
                    )
                }
            }
        },
    )
}
