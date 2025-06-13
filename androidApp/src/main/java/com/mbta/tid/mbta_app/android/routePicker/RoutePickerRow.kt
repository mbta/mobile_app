package com.mbta.tid.mbta_app.android.routePicker

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.model.RouteCardData

@Composable
fun RoutePickerRow(route: RouteCardData.LineOrRoute, onTap: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onTap() }.padding(horizontal = 8.dp, vertical = 12.dp),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).padding(start = 4.dp),
            Arrangement.spacedBy(8.dp),
            Alignment.CenterVertically,
        ) {
            RoutePill(route.sortRoute, type = RoutePillType.Fixed)
            Text(route.sortRoute.longName)
        }
        Image(
            painterResource(id = R.drawable.baseline_chevron_right_24),
            contentDescription = null,
            modifier = Modifier.padding(horizontal = 8.dp).widthIn(max = 8.dp),
            colorFilter = ColorFilter.tint(colorResource(R.color.text).copy(alpha = 0.6f)),
        )
    }
}
