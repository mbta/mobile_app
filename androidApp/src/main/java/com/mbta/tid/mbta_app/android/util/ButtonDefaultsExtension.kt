package com.mbta.tid.mbta_app.android.util

import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.mbta.tid.mbta_app.android.R

@Composable
fun ButtonDefaults.contrastTranslucent() =
    buttonColors(
        containerColor = colorResource(R.color.text).copy(alpha = 0.6f),
        contentColor = colorResource(R.color.fill2),
    )

@Composable
fun ButtonDefaults.contrast() =
    buttonColors(
        containerColor = colorResource(R.color.contrast),
        contentColor = colorResource(R.color.fill2),
    )

@Composable
fun ButtonDefaults.key() =
    buttonColors(
        containerColor = colorResource(R.color.key),
        contentColor = colorResource(R.color.fill3),
    )
