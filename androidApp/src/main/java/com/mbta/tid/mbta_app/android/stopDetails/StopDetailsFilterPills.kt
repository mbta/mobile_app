package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.util.StopDetailsFilter
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.Route

sealed interface PillFilter {
    val id: String

    data class ByRoute(val route: Route, val line: Line?) : PillFilter {
        override val id: String
            get() = route.id
    }

    data class ByLine(val line: Line) : PillFilter {
        override val id: String
            get() = line.id
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StopDetailsFilterPills(
    servedRoutes: List<PillFilter>,
    filterState: MutableState<StopDetailsFilter?>,
    onTapRoutePill: (PillFilter) -> Unit
) {
    var filter by filterState

    Row {
        val scrollState = rememberScrollState()

        Row(Modifier.horizontalScroll(scrollState).padding(horizontal = 15.dp)) {
            for (filterBy in servedRoutes) {
                val isActive = filter == null || filter?.routeId == filterBy.id
                val requester = remember { BringIntoViewRequester() }
                when (filterBy) {
                    is PillFilter.ByRoute -> {
                        RoutePill(
                            filterBy.route,
                            line = filterBy.line,
                            type = RoutePillType.Flex,
                            isActive = isActive,
                            modifier =
                                Modifier.minimumInteractiveComponentSize()
                                    .bringIntoViewRequester(requester)
                                    .toggleable(isActive) { onTapRoutePill(filterBy) }
                        )
                    }
                    is PillFilter.ByLine -> {
                        RoutePill(
                            route = null,
                            line = filterBy.line,
                            type = RoutePillType.Flex,
                            isActive = isActive,
                            modifier =
                                Modifier.minimumInteractiveComponentSize()
                                    .bringIntoViewRequester(requester)
                                    .toggleable(isActive) { onTapRoutePill(filterBy) }
                        )
                    }
                }

                LaunchedEffect(filter) {
                    if (filter?.routeId == filterBy.id) {
                        requester.bringIntoView()
                    }
                }
            }
        }

        if (filter != null) {
            Button(
                onClick = { filter = null },
                modifier = Modifier.padding(end = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.contrast),
                        contentColor = colorResource(R.color.fill1)
                    ),
                border = BorderStroke(2.dp, colorResource(R.color.halo))
            ) {
                Text(
                    stringResource(R.string.filterShowAll),
                    Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                )
            }
        }
    }
}
