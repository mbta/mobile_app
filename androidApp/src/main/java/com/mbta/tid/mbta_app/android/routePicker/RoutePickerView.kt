package com.mbta.tid.mbta_app.android.routePicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.component.NavTextButton
import com.mbta.tid.mbta_app.android.component.ScrollSeparatorColumn
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath

@Composable
fun RoutePickerView(
    path: RoutePickerPath,
    context: RouteDetailsContext,
    onOpenPickerPath: (RoutePickerPath, RouteDetailsContext) -> Unit,
    onOpenRouteDetails: (String, RouteDetailsContext) -> Unit,
    onClose: () -> Unit,
    errorBannerViewModel: ErrorBannerViewModel,
) {
    val globalData = getGlobalData("RoutePickerView.globalData")

    if (globalData == null) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = "Loading" })
        return
    }

    val modes =
        listOf(
            RoutePickerPath.Bus,
            RoutePickerPath.Silver,
            RoutePickerPath.CommuterRail,
            RoutePickerPath.Ferry,
        )
    val routes = remember(globalData, path) { globalData.getRoutesForPicker(path) }

    Column(Modifier.fillMaxWidth(), Arrangement.Top, Alignment.CenterHorizontally) {
        Row(
            Modifier.padding(start = 24.dp, top = 0.dp, end = 16.dp, bottom = 8.dp).fillMaxWidth(),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically,
        ) {
            Text(
                when (path) {
                    is RoutePickerPath.Root ->
                        when (context) {
                            is RouteDetailsContext.Favorites ->
                                stringResource(R.string.route_picker_header_favorites)
                            is RouteDetailsContext.Details -> TODO("Implement details header")
                        }
                    is RoutePickerPath.Bus -> stringResource(R.string.bus)
                    is RoutePickerPath.Silver -> "Silver Line"
                    is RoutePickerPath.CommuterRail -> "Commuter Rail"
                    is RoutePickerPath.Ferry -> stringResource(R.string.ferry)
                },
                style = Typography.title2Bold,
                color = path.textColor,
            )
            NavTextButton(stringResource(R.string.cancel), onClose)
        }
        ErrorBanner(errorBannerViewModel, Modifier.padding(start = 14.dp, top = 6.dp, end = 14.dp))
        ScrollSeparatorColumn(
            Modifier.padding(start = 14.dp, top = 6.dp, end = 14.dp, bottom = 40.dp),
            Arrangement.spacedBy(4.dp),
            haloColor = path.haloColor,
        ) {
            if (path == RoutePickerPath.Root) {
                for (mode in modes) {
                    RoutePickerRootRow(mode) { onOpenPickerPath(mode, context) }
                }
                Text(
                    stringResource(R.string.subway),
                    style = Typography.subheadlineSemibold,
                    modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 2.dp),
                )
                for (route in routes) {
                    RoutePickerRootRow(route) { onOpenRouteDetails(route.id, context) }
                }
            } else {
                Column(Modifier.haloContainer(2.dp, outlineColor = path.haloColor)) {
                    for ((index, route) in routes.withIndex()) {
                        RoutePickerRow(route) { onOpenRouteDetails(route.id, context) }
                        if (index < routes.lastIndex) {
                            HaloSeparator()
                        }
                    }
                }
            }
        }
    }
}
