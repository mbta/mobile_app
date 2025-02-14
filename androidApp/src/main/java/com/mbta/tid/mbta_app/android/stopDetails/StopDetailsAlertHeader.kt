package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.component.AlertIcon
import com.mbta.tid.mbta_app.android.component.InfoCircle
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.StopAlertState

@Composable
fun StopDetailsAlertHeader(
    alert: Alert,
    routeColor: Color?,
    modifier: Modifier = Modifier,
    showInfoIcon: Boolean = false
) {
    Row(
        modifier = modifier.padding(start = 12.dp, end = 8.dp).padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AlertIcon(
            alertState = alert.alertState,
            color = routeColor,
            modifier =
                if (alert.alertState == StopAlertState.Elevator)
                    Modifier.height(24.dp).width(36.dp).padding(end = 8.dp).fillMaxHeight()
                else Modifier.size(36.dp).padding(6.dp)
        )
        Text(
            alert.header ?: "",
            Modifier.padding(vertical = 3.dp).weight(1f),
            style = Typography.callout
        )
        if (showInfoIcon) {
            Box(
                Modifier.padding(4.dp).height(IntrinsicSize.Max).align(Alignment.CenterVertically)
            ) {
                InfoCircle()
            }
        }
    }
}
