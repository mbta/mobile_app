package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.StopDetailsFilter

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
    filter: StopDetailsFilter? = null,
    onTapRoutePill: (PillFilter) -> Unit,
    onClearFilter: () -> Unit,
) {

    @Composable
    fun pillHint(appliedFilter: StopDetailsFilter?, pillFilter: PillFilter): String {
        val filteredToPill = appliedFilter?.routeId == pillFilter.id
        return if (filteredToPill) {
            stringResource(R.string.stop_filter_pills_remove_hint)
        } else {
            stringResource(R.string.stop_filter_pills_filter_hint)
        }
    }

    Box(Modifier.fillMaxWidth()) {
        val scrollState = rememberScrollState()

        Row(
            Modifier.horizontalScroll(scrollState)
                .padding(horizontal = 15.dp)
                .align(Alignment.CenterStart)
        ) {
            for (filterBy in servedRoutes) {
                val isActive = filter == null || filter.routeId == filterBy.id
                val requester = remember { BringIntoViewRequester() }
                val pillModifier =
                    Modifier.padding(end = 8.dp)
                        .minimumInteractiveComponentSize()
                        .bringIntoViewRequester(requester)
                        .clickable(
                            onClickLabel = pillHint(filter, filterBy),
                            onClick = { onTapRoutePill(filterBy) },
                        )
                        .semantics {
                            role = Role.Button
                            selected = isActive
                        }

                when (filterBy) {
                    is PillFilter.ByRoute -> {
                        RoutePill(
                            filterBy.route,
                            line = filterBy.line,
                            type = RoutePillType.Flex,
                            isActive = isActive,
                            modifier = pillModifier,
                        )
                    }
                    is PillFilter.ByLine -> {
                        RoutePill(
                            route = null,
                            line = filterBy.line,
                            type = RoutePillType.Flex,
                            isActive = isActive,
                            modifier = pillModifier,
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
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = onClearFilter,
                shape = RoundedCornerShape(8.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.contrast),
                        contentColor = colorResource(R.color.fill1),
                    ),
                border = BorderStroke(2.dp, colorResource(R.color.halo)),
            ) {
                Text(
                    stringResource(R.string.filter_show_all),
                    style = Typography.body.merge(colorResource(R.color.fill1)),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }
}
