package com.mbta.tid.mbta_app.android.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R

@Composable
fun RecenterButton(
    painter: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .semantics(mergeDescendants = true) {}
                .size(44.dp)
                .background(colorResource(R.color.halo), CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(40.dp)
                .background(color = colorResource(R.color.fill3), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painter, contentDescription, tint = colorResource(R.color.key))
        }
    }
}

@Composable
fun RecenterButton(
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    RecenterButton(rememberVectorPainter(imageVector), contentDescription, modifier, onClick)
}

@Preview
@Composable
fun RecenterButtonPreview() {
    RecenterButton(Icons.Default.LocationOn, stringResource(R.string.recenter), onClick = {})
}
