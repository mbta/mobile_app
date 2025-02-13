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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePillSpec
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.silverRoutes

@Composable
fun StopResultsView(
    shape: RoundedCornerShape,
    stop: StopResult,
    globalResponse: GlobalResponse?,
    handleSearch: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    val routes = globalResponse?.getTypicalRoutesFor(stop.id) ?: emptyList()

    Column {
        Row(
            modifier =
                Modifier.clickable { handleSearch(stop.id) }
                    .background(colorResource(id = R.color.fill3), shape = shape)
                    .padding(12.dp)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconModifier = Modifier.height(32.dp).width(32.dp)

            val routePillsData =
                routes
                    .sortedBy { it.sortOrder }
                    .map<Route, Triple<Route, RoutePillSpec, String>> { route ->
                        val line: Line? =
                            if (route.lineId != null) {
                                globalResponse?.lines?.get(route.lineId)
                            } else {
                                null
                            }

                        val context: RoutePillSpec.Context =
                            if (stop.isStation) {
                                RoutePillSpec.Context.SearchStation
                            } else {
                                RoutePillSpec.Context.Default
                            }

                        val contentDescription =
                            if (silverRoutes.contains(route.id) && stop.isStation) {
                                stringResource(
                                    id = R.string.route_with_type,
                                    "Silver Line",
                                    route?.type?.typeText(LocalContext.current, isOnly = false)
                                        ?: ""
                                )
                            } else if (route.type == RouteType.COMMUTER_RAIL && stop.isStation) {
                                stringResource(
                                    id = R.string.route_with_type,
                                    "Commuter Rail",
                                    route?.type?.typeText(LocalContext.current, isOnly = false)
                                        ?: ""
                                )
                            } else {
                                stringResource(
                                    id = R.string.route_with_type,
                                    route.label,
                                    route?.type?.typeText(LocalContext.current, isOnly = true) ?: ""
                                )
                            }

                        Triple(
                            route,
                            RoutePillSpec(route, line, RoutePillSpec.Type.FlexCompact, context),
                            contentDescription
                        )
                    }
                    .distinctBy { (_, spec, _) -> spec }

            val routesContentDescription =
                stringResource(
                    R.string.serves_route_list,
                    routePillsData.joinToString(",") { (_, _, contentDescription) ->
                        contentDescription
                    }
                )
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
                            .semantics(mergeDescendants = true) {}
                            .clearAndSetSemantics { contentDescription = routesContentDescription }
                ) {
                    routePillsData.map { (route, spec, _) ->
                        RoutePill(
                            route = route,
                            spec = spec,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
