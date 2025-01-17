package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mapbox.maps.extension.style.expressions.dsl.generated.color
import com.mbta.tid.mbta_app.android.R

@Composable
fun InfoCircle(modifier: Modifier = Modifier) {
    Icon(
        painterResource(R.drawable.fa_circle_info),
        contentDescription = "More Info",
        Modifier.size(16.dp).then(modifier),
        tint = colorResource(R.color.deemphasized)
    )
}
