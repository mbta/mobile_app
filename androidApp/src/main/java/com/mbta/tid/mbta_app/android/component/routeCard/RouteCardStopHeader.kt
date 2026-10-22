package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.StopAccessibilitySubheader
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.response.GlobalResponse

@Composable
fun RouteCardStopHeader(
    data: RouteCardData.RouteStopData,
    globalData: GlobalResponse?,
) {
    Column(
        modifier =
            Modifier.background(colorResource(R.color.fill2))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = data.stop.name,
            modifier = Modifier.placeholderIfLoading().fillMaxWidth(),
            overflow = TextOverflow.Visible,
            style = Typography.callout,
        )
        StopAccessibilitySubheader(
            stop = data.stop,
            elevatorAlerts = data.elevatorAlerts,
            globalData = globalData,
        )
    }
}
