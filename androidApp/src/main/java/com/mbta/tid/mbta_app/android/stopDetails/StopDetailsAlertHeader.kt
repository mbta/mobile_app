package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbta.tid.mbta_app.android.component.AlertIcon
import com.mbta.tid.mbta_app.android.component.InfoCircle
import com.mbta.tid.mbta_app.model.Alert

@Composable
fun StopDetailsAlertHeader(alert: Alert, routeColor: Color?, showInfoIcon: Boolean = false) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AlertIcon(
            alertState = alert.alertState,
            color = routeColor,
            modifier = Modifier.size(36.dp).padding(6.dp)
        )
        Text(
            alert.header ?: "",
            Modifier.padding(vertical = 3.dp).weight(1f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (showInfoIcon) {
            Box(Modifier.height(IntrinsicSize.Max).align(Alignment.CenterVertically)) {
                InfoCircle()
            }
        }
    }
}
