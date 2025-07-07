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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.RoundedCornerColumn
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.text
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.viewModel.SearchViewModel

@Composable
fun StopResultsView(
    stops: List<SearchViewModel.StopResult>,
    handleSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    RoundedCornerColumn(stops, modifier) { shape, stopResult ->
        StopResultsView(shape, stopResult, handleSearch)
    }
}

@Composable
fun StopResultsView(
    shape: RoundedCornerShape,
    stop: SearchViewModel.StopResult,
    handleSearch: (String) -> Unit,
) {
    val scrollState = rememberScrollState()

    Column {
        Row(
            modifier =
                Modifier.clickable { handleSearch(stop.id) }
                    .background(colorResource(id = R.color.fill3), shape = shape)
                    .padding(12.dp)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val iconModifier = Modifier.height(32.dp).width(32.dp).placeholderIfLoading()

            val routePillsData = stop.routePills

            @Suppress("SimplifiableCallChain")
            val routesContentDescription =
                stringResource(
                    R.string.serves_route_list,
                    routePillsData
                        .map { spec -> spec.contentDescription?.text ?: "" }
                        .joinToString(","),
                )
            if (stop.isStation) {
                Icon(
                    painter = painterResource(R.drawable.mbta_logo),
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = Color.Unspecified,
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.stop_bus),
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = Color.Unspecified,
                )
            }
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.semantics(mergeDescendants = true) {},
            ) {
                Text(
                    text = stop.name,
                    modifier =
                        Modifier.padding(top = 12.dp, bottom = 8.dp, start = 16.dp)
                            .placeholderIfLoading(),
                    style = Typography.bodySemibold,
                )
                Row(
                    modifier =
                        Modifier.horizontalScroll(scrollState)
                            .padding(start = 16.dp, bottom = 12.dp)
                            .clearAndSetSemantics { contentDescription = routesContentDescription }
                            .placeholderIfLoading()
                ) {
                    routePillsData.map { spec ->
                        RoutePill(
                            route = null,
                            spec = spec,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
