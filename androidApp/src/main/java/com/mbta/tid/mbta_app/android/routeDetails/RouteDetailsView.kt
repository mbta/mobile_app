package com.mbta.tid.mbta_app.android.routeDetails

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.StarIcon
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel

@Composable
fun RouteDetailsView(
    selectionId: String,
    context: RouteDetailsContext,
    onOpenStopDetails: (String) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    openModal: (ModalRoutes) -> Unit,
    errorBannerViewModel: IErrorBannerViewModel,
) {
    val globalData = getGlobalData("RouteDetailsView")
    val lineOrRoute = globalData?.getLineOrRoute(selectionId)
    if (lineOrRoute == null) {
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
                        stringResource(R.string.add_favorite_label)
                    }
            }
        },
        onBack = onBack,
        onClose = onClose,
        openModal = openModal,
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
                        Color.fromHex(lineOrRoute.backgroundColor),
                        contentDescription = if (rowContext.isFavorited) favoriteCD else "",
                    )
                }
            }
        },
    )
}
