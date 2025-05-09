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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.RoutePillSpec
import com.mbta.tid.mbta_app.model.RouteResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.TestData

@Composable
fun RouteResultsView(
    shape: RoundedCornerShape,
    routeResult: RouteResult,
    globalResponse: GlobalResponse?,
    handleSearch: (String) -> Unit,
) {
    val route = globalResponse?.getRoute(routeResult.id)

    val line: Line? =
        if (route?.lineId != null) {
            globalResponse.getLine(route.lineId)
        } else {
            null
        }

    val routePillSpec =
        RoutePillSpec(route, line, RoutePillSpec.Type.Fixed, RoutePillSpec.Context.Default)

    Column {
        Row(
            modifier =
                Modifier.clip(shape)
                    .clickable { handleSearch(routeResult.id) }
                    .background(colorResource(id = R.color.fill3))
                    .padding(12.dp)
                    .minimumInteractiveComponentSize()
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoutePill(route = route, spec = routePillSpec)

            Text(text = routeResult.longName, style = Typography.bodySemibold)
        }
    }
}

@Preview
@Composable
private fun RouteResultsViewPreview() {
    val orangeLine = TestData.getRoute("Orange")
    RouteResultsView(
        RoundedCornerShape(10.dp),
        RouteResult(orangeLine),
        GlobalResponse(TestData),
        handleSearch = {},
    )
}
