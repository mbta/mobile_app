package com.mbta.tid.mbta_app.android.search.results

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.StopResult

@Composable
fun StopResultsView(stop: StopResult, handleSearch: (String) -> Unit) {
    Row(
        modifier =
            Modifier.clickable { handleSearch(stop.id) }
                .background(colorResource(id = R.color.fill2))
                .fillMaxWidth()
    ) {
        Text(
            text = stop.name,
            modifier = Modifier.padding(top = 11.dp, bottom = 11.dp, start = 16.dp, end = 8.dp),
            style = MaterialTheme.typography.headlineSmall
        )
    }
}
