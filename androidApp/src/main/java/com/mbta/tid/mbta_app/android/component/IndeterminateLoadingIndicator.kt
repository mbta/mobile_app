package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R

@Composable
fun IndeterminateLoadingIndicator(modifier: Modifier = Modifier.width(20.dp)) {
    CircularProgressIndicator(
        modifier = modifier,
        color = colorResource(R.color.contrast),
        trackColor = colorResource(R.color.contrast).copy(alpha = 0.25f),
    )
}
