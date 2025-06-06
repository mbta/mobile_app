package com.mbta.tid.mbta_app.android.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.Typography

@Composable
fun NoFavoritesView(onAddStops: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Text(
            stringResource(R.string.no_stops_added),
            textAlign = TextAlign.Center,
            style = Typography.title3,
            color = colorResource(R.color.deemphasized),
        )
        Icon(
            painterResource(R.drawable.pinned_route_inactive),
            null,
            modifier = Modifier.size(56.dp).clearAndSetSemantics {},
            tint = colorResource(R.color.deemphasized),
        )
        Row(
            modifier =
                Modifier.clickable { onAddStops() }
                    .clip(MaterialTheme.shapes.medium)
                    .background(color = colorResource(R.color.key))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.add_stops),
                    style = Typography.bodySemibold,
                    color = colorResource(R.color.fill3),
                )
                Icon(
                    painterResource(R.drawable.plus),
                    null,
                    modifier = Modifier.size(24.dp),
                    tint = colorResource(R.color.fill3),
                )
            }
        }
    }
}
