package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.stopCard.StopHeader
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.isStopBlocklisted
import com.mbta.tid.mbta_app.repositories.Settings

@Composable
fun StopCardHeader(
    stop: Stop,
    elevatorAlerts: List<Alert>,
    includeIcon: Boolean,
    globalData: GlobalResponse?,
) {
    val showStationAccessibility =
        SettingsCache.get(Settings.StationAccessibility) &&
            !globalData.isStopBlocklisted(stop.parentStationId ?: stop.id)
    val isWheelchairAccessible = stop.isWheelchairAccessible
    val showInaccessible = showStationAccessibility && !isWheelchairAccessible
    val showElevatorAlerts = showStationAccessibility && elevatorAlerts.isNotEmpty()

    StopHeader(
        stop = stop,
        subheader = {
            if (showInaccessible || showElevatorAlerts) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (showElevatorAlerts) {
                        Image(
                            painterResource(R.drawable.accessibility_icon_alert),
                            null,
                            modifier =
                                Modifier.height(18.dp)
                                    .placeholderIfLoading()
                                    .testTag("elevator_alert")
                                    .clearAndSetSemantics {},
                        )
                    } else if (showInaccessible) {
                        Image(
                            painterResource(R.drawable.accessibility_icon_not_accessible),
                            null,
                            modifier =
                                Modifier.height(18.dp)
                                    .placeholderIfLoading()
                                    .testTag("wheelchair_not_accessible")
                                    .clearAndSetSemantics {},
                        )
                    }
                    Text(
                        text =
                            if (showInaccessible) stringResource(R.string.not_accessible)
                            else
                                pluralStringResource(
                                    R.plurals.elevator_closure_count,
                                    elevatorAlerts.size,
                                    elevatorAlerts.size,
                                ),
                        modifier = Modifier.placeholderIfLoading().fillMaxWidth(),
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Visible,
                        style = Typography.footnoteSemibold,
                        color = colorResource(R.color.accessibility),
                    )
                }
            }
        },
    )
}

@Composable
fun StopCardHeader(
    data: RouteCardData.RouteStopData,
    includeIcon: Boolean,
    globalData: GlobalResponse?,
) {
    StopCardHeader(data.stop, data.elevatorAlerts, includeIcon, globalData)
}
