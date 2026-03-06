package com.mbta.tid.mbta_app.android.component

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.StopAlertState

@Composable
fun AlertIcon(
    alertState: StopAlertState,
    color: Color?,
    modifier: Modifier = Modifier,
    overrideIcon: Painter? = null,
) {
    val icon =
        overrideIcon
            ?: painterResource(
                when (alertState) {
                    StopAlertState.Elevator -> R.drawable.accessibility_icon_alert
                    StopAlertState.Issue -> R.drawable.alert_borderless_issue
                    StopAlertState.Shuttle -> R.drawable.alert_borderless_shuttle
                    StopAlertState.Suspension -> R.drawable.alert_borderless_suspension
                    StopAlertState.AllClear -> R.drawable.alert_borderless_allclear
                    else -> return
                }
            )

    Icon(
        icon,
        contentDescription = stringResource(R.string.alert),
        modifier,
        tint =
            if (alertState == StopAlertState.Elevator) Color.Unspecified
            else color ?: colorResource(R.color.text),
    )
}
