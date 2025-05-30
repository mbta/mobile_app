package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R

@Composable
fun NavDrilldownRow(
    onClick: () -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(Modifier) -> Unit,
) {
    Row(
        Modifier.background(color = MaterialTheme.colorScheme.background)
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClickLabel = onClickLabel, onClick = onClick)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        content(Modifier.weight(1f))
        Column(modifier = Modifier.padding(start = 8.dp).widthIn(max = 8.dp)) {
            Icon(
                painterResource(id = R.drawable.baseline_chevron_right_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Preview
@Composable
private fun NavDrilldownRowPreview() {
    NavDrilldownRow({}, "") { modifier -> Text("ABC", modifier) }
}
