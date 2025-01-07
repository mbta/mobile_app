package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R

@Composable
fun NoNearbyStopsView(
    hideMaps: Boolean,
    onOpenSearch: () -> Unit,
    onPanToDefaultCenter: () -> Unit
) {
    Column(
        modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
                .background(colorResource(R.color.fill3))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(R.drawable.mbta_logo),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.Unspecified
            )
            Text(
                stringResource(R.string.no_stops_nearby_title),
                style = MaterialTheme.typography.titleLarge
            )
        }
        Text(stringResource(R.string.no_stops_nearby), style = MaterialTheme.typography.bodyMedium)
        Button(
            onClick = onOpenSearch,
            modifier = Modifier.requiredHeightIn(min = 48.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.no_stops_nearby_search),
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(
                    painterResource(R.drawable.fa_magnifying_glass_solid),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (!hideMaps) {
            OutlinedButton(
                onClick = onPanToDefaultCenter,
                modifier = Modifier.requiredHeightIn(min = 48.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, colorResource(R.color.key))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.no_stops_nearby_pan),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(
                        painterResource(R.drawable.fa_map),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun NoNearbyStopsViewPreview() {
    MyApplicationTheme { NoNearbyStopsView(hideMaps = false, {}, {}) }
}
