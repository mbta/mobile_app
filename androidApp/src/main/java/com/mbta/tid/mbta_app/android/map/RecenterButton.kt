package com.mbta.tid.mbta_app.android.map

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.mbta.tid.mbta_app.android.R

@Composable
fun RecenterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier.semantics(mergeDescendants = true) {},
        colors =
            IconButtonDefaults.iconButtonColors(
                containerColor = colorResource(R.color.fill3),
                contentColor = colorResource(R.color.key),
            ),
    ) {
        Icon(Icons.Default.LocationOn, stringResource(R.string.recenter))
    }
}

@Preview
@Composable
fun RecenterButtonPreview() {
    RecenterButton(onClick = {})
}
