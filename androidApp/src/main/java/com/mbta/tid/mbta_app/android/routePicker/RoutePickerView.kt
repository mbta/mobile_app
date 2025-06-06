package com.mbta.tid.mbta_app.android.routePicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.greenRoutes
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
        CircularProgressIndicator()
        return
    }

    val routes = globalData.getRoutesForPicker(path)
    val displayedRoutes =
        routes
            .mapNotNull { route ->
                if (route.id in greenRoutes) {
                    globalData.getLine(route.lineId)?.let { line ->
                        RouteCardData.LineOrRoute.Line(
                            line,
                            routes = routes.filter { it.lineId == line.id }.toSet(),
                        )
                    }
                } else {
                    RouteCardData.LineOrRoute.Route(route)
                }
            }
            .distinct()

    Column(
        Modifier.padding(start = 14.dp, top = 0.dp, end = 14.dp).fillMaxWidth(),
        Arrangement.Top,
        Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.padding(start = 10.dp, top = 0.dp, end = 2.dp, bottom = 14.dp).fillMaxWidth(),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically,
        ) {
            Text(
                when (context) {
                    is RouteDetailsContext.Favorites ->
                        stringResource(R.string.route_picker_header_favorites)
                    is RouteDetailsContext.Details -> TODO("Implement details header")
                },
                style = Typography.title2Bold,
            )
            Button(
                onClose,
                colors =
                    ButtonColors(
                        colorResource(R.color.text).copy(alpha = 0.6f),
                        colorResource(R.color.fill2),
                        colorResource(R.color.text).copy(alpha = 0.6f),
                        colorResource(R.color.fill2),
                    ),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.heightIn(min = 32.dp),
            ) {
                Text(stringResource(R.string.cancel), style = Typography.callout)
            }
        }
        ErrorBanner(errorBannerViewModel)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(bottom = 56.dp),
            Arrangement.spacedBy(4.dp),
        ) {
            if (path == RoutePickerPath.Root) {
                Text(
                    stringResource(R.string.subway),
                    style = Typography.subheadlineSemibold,
                    modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 2.dp),
                )
            }
            for (route in displayedRoutes) {
                RoutePickerRow(route) { onOpenRouteDetails(route.id, context) }
            }
        }
    }
}
