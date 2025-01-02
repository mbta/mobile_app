package com.mbta.tid.mbta_app.android.search.results

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.model.RoutePillSpec
import com.mbta.tid.mbta_app.model.StopResult

@Composable
fun StopResultsView(stop: StopResult, handleSearch: (String) -> Unit) {
    val globalResponse = getGlobalData()
    val scrollState = rememberScrollState()

    val routes = globalResponse?.getTypicalRoutesFor(stop.id) ?: emptyList()

    Column {
        Row(
            modifier =
                Modifier.clickable { handleSearch(stop.id) }
                    .background(colorResource(id = R.color.fill3))
                    .padding(12.dp)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconModifier = Modifier.height(32.dp).width(32.dp)
            if (stop.isStation) {
                Icon(
                    painter = painterResource(R.drawable.mbta_logo),
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = Color.Unspecified
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.stop_bus),
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = Color.Unspecified
                )
            }
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = stop.name,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp, start = 16.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier =
                        Modifier.horizontalScroll(scrollState)
                            .padding(start = 16.dp, bottom = 12.dp)
                ) {
                    routes.map {
                        RoutePill(
                            route = it,
                            type = RoutePillSpec.Type.FlexCompact,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().height(1.dp),
            color = colorResource(id = R.color.fill1)
        )
    }
}
