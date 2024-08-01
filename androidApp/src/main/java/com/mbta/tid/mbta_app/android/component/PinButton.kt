package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R

@Composable
fun PinButton(pinned: Boolean, action: () -> Unit) {
    IconToggleButton(
        checked = pinned,
        onCheckedChange = { action() },
        modifier = Modifier.size(30.dp)
    ) {
        Icon(
            painter =
                painterResource(
                    id =
                        if (pinned) R.drawable.pinned_route_active
                        else R.drawable.pinned_route_inactive
                ),
            contentDescription = "pin route",
            modifier = Modifier.size(20.dp),
            tint = Color.Unspecified
        )
    }
}
