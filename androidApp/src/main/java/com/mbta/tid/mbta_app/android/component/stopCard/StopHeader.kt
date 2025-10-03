package com.mbta.tid.mbta_app.android.component.stopCard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.Stop

@Composable
fun StopHeader(stop: Stop, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(colorResource(R.color.fill1))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(
                if (stop.locationType == LocationType.STATION) R.drawable.mbta_logo
                else R.drawable.stop_bus
            ),
            null,
            Modifier.width(24.dp),
        )
        Text(stop.name, Modifier.weight(1f), style = Typography.bodySemibold)
    }
}
