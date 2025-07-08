package com.mbta.tid.mbta_app.android.search.results

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.RoundedCornerColumn
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.model.RoutePillSpec
import com.mbta.tid.mbta_app.utils.TestData
import com.mbta.tid.mbta_app.viewModel.SearchViewModel

@Composable
fun RouteResultsView(routes: List<SearchViewModel.RouteResult>, handleSearch: (String) -> Unit) {
    Text(
        stringResource(R.string.routes),
        Modifier.padding(vertical = 8.dp),
        style = Typography.subheadlineSemibold,
    )
    RoundedCornerColumn(routes) { shape, routeResult ->
        RouteResultsView(shape, routeResult, handleSearch)
    }
}

@Composable
fun RouteResultsView(
    shape: RoundedCornerShape,
    routeResult: SearchViewModel.RouteResult,
    handleSearch: (String) -> Unit,
) {
    val resultId =
        when {
            routeResult.id == "Green" -> "line-Green"
            else -> routeResult.id
        }

    Column {
        Row(
            modifier =
                Modifier.clip(shape)
                    .clickable { handleSearch(resultId) }
                    .background(colorResource(id = R.color.fill3))
                    .padding(12.dp)
                    .minimumInteractiveComponentSize()
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoutePill(route = null, spec = routeResult.routePill)

            Text(text = routeResult.name, style = Typography.bodySemibold)
        }
    }
}

@Preview
@Composable
private fun RouteResultsViewPreview() {
    val orangeLine = TestData.getRoute("Orange")
    RouteResultsView(
        RoundedCornerShape(10.dp),
        SearchViewModel.RouteResult(
            orangeLine.id,
            orangeLine.longName,
            RoutePillSpec(orangeLine, null, RoutePillSpec.Type.Fixed),
        ),
        handleSearch = {},
    )
}
