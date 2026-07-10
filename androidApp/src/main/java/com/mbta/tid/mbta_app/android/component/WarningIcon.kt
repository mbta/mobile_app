package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.generated.drawableByName
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading

@Composable
fun warningIcon(iconName: String, modifier: Modifier = Modifier) =
    Image(
        painterResource(drawableByName(iconName)),
        stringResource(R.string.alert),
        modifier = modifier.placeholderIfLoading(),
    )
