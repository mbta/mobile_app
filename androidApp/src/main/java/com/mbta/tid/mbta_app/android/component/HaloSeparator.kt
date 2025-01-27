package com.mbta.tid.mbta_app.android.component

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.mbta.tid.mbta_app.android.R

@Composable
fun HaloSeparator() {
    HorizontalDivider(color = colorResource(R.color.halo))
}
