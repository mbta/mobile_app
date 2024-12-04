package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R

@Composable
fun MorePage(bottomBar: @Composable () -> Unit) {
    Scaffold(bottomBar = bottomBar) { outerSheetPadding ->
        Column(Modifier.padding(outerSheetPadding)) {
            Text(
                text = stringResource(R.string.more_title),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
