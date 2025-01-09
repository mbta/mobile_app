package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.placeholderIfLoading

@Composable
fun PinButton(pinned: Boolean, color: Color, action: () -> Unit) {
    val onClickLabel =
        if (pinned) {
            stringResource(id = R.string.unstar_route_hint)
        } else {
            stringResource(id = R.string.star_route_hint)
        }
    IconToggleButton(
        checked = pinned,
        onCheckedChange = { action() },
        modifier =
            Modifier.size(30.dp).clickable(onClickLabel = onClickLabel, onClick = { action() })
    ) {
        Icon(
            painter =
                painterResource(
                    id =
                        if (pinned) R.drawable.pinned_route_active
                        else R.drawable.pinned_route_inactive
                ),
            contentDescription = stringResource(R.string.star_route),
            modifier = Modifier.size(20.dp).placeholderIfLoading(),
            tint = color
        )
    }
}
