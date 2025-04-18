package com.mbta.tid.mbta_app.android.component

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import com.mbta.tid.mbta_app.android.R

@Composable
fun HaloSeparator(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, color = colorResource(R.color.halo))
}
