package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.repositories.Settings

@Composable
fun PinButton(pinned: Boolean, color: Color, action: () -> Unit) {
    val enhancedFavorites = SettingsCache.get(Settings.EnhancedFavorites)
    val onClickLabel =
        if (enhancedFavorites) {
            if (pinned) {
                stringResource(R.string.remove_favorite)
            } else {
                stringResource(R.string.add_favorite)
            }
        } else {
            if (pinned) {
                stringResource(id = R.string.unstar_route_hint)
            } else {
                stringResource(id = R.string.star_route_hint)
            }
        }
    IconToggleButton(
        checked = pinned,
        onCheckedChange = { action() },
        modifier =
            Modifier.size(30.dp).clickable(onClickLabel = onClickLabel, onClick = { action() }),
    ) {
        StarIcon(pinned, color)
    }
}

@Composable
fun StarIcon(pinned: Boolean, color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Icon(
        painter =
            painterResource(
                id =
                    if (pinned) R.drawable.pinned_route_active else R.drawable.pinned_route_inactive
            ),
        contentDescription = stringResource(R.string.star_route),
        modifier = modifier.requiredSize(size).placeholderIfLoading(),
        tint = color,
    )
}
