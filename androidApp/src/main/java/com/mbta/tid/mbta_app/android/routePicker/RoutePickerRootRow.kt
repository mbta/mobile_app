package com.mbta.tid.mbta_app.android.routePicker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.routeIcon
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath.Silver.routeType

@Composable
fun RoutePickerRootRow(route: LineOrRoute, onTap: () -> Unit) {
    val routeColor = Color.fromHex(route.backgroundColor)
    val textColor = Color.fromHex(route.textColor)
    RoutePickerRootRow(route.type, routeColor, textColor, onTap) {
        Text(route.name, style = Typography.headlineBold, color = textColor)
    }
}

@Composable
fun RoutePickerRootRow(path: RoutePickerPath, onTap: () -> Unit) {
    RoutePickerRootRow(path.routeType, path.backgroundColor, path.textColor, onTap) { path.Label }
}

@Composable
fun RoutePickerRootRow(
    routeType: RouteType,
    routeColor: Color,
    textColor: Color,
    onTap: () -> Unit,
    label: @Composable () -> Unit,
) {
    val icon = routeIcon(routeType)

    Row(
        Modifier.fillMaxWidth()
            .haloContainer(2.dp)
            .background(routeColor)
            .clickable { onTap() }
            .padding(horizontal = 8.dp, vertical = 12.dp),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
            Image(icon, null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(textColor))
            label()
        }
        Image(
            painterResource(id = R.drawable.baseline_chevron_right_24),
            contentDescription = null,
            modifier = Modifier.padding(horizontal = 8.dp).widthIn(max = 8.dp),
            colorFilter = ColorFilter.tint(textColor.copy(alpha = 0.6f)),
        )
    }
}
