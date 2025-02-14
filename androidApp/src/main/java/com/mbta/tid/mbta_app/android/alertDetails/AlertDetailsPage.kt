package com.mbta.tid.mbta_app.android.alertDetails

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ActionButton
import com.mbta.tid.mbta_app.android.component.ActionButtonKind
import com.mbta.tid.mbta_app.android.component.routeIcon
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.stateJsonSaver
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import kotlin.time.Duration.Companion.seconds

@Composable
fun AlertDetailsPage(
    alertId: String,
    lineId: String?,
    routeIds: List<String>?,
    stopId: String?,
    alerts: AlertsStreamDataResponse?,
    goBack: () -> Unit
) {
    val alert = getAlert(alerts, alertId, goBack)
    val globalResponse = getGlobalData("AlertDetailsPage.loadGlobal")
    val now = timer(5.seconds)

    val line = globalResponse?.getLine(lineId)
    val routes = routeIds?.mapNotNull { globalResponse?.routes?.get(it) }
    val stop = globalResponse?.stops?.get(stopId)

    val firstRoute = routes?.firstOrNull()

    val affectedStops =
        if (globalResponse == null || alert == null || routes == null) emptyList()
        else {
            val routeEntities =
                alert.matchingEntities { entity ->
                    routes.any { route -> entity.route == null || entity.route == route.id }
                }
            val parentStops =
                routeEntities.mapNotNull {
                    globalResponse.stops[it.stop]?.resolveParent(globalResponse.stops)
                }
            parentStops.distinct()
        }

    val headerColor =
        listOfNotNull(
                line?.color,
                firstRoute?.color,
            )
            .firstOrNull()
            ?.let { Color.fromHex(it) }
            ?: colorResource(R.color.fill1)

    val headerTextColor =
        listOfNotNull(
                line?.textColor,
                firstRoute?.textColor,
            )
            .firstOrNull()
            ?.let { Color.fromHex(it) }
            ?: colorResource(R.color.text)

    Column(Modifier.background(colorResource(R.color.fill2))) {
        Row(
            Modifier.background(headerColor).padding(16.dp).safeDrawingPadding(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (alert?.effect == Alert.Effect.ElevatorClosure) {
                Image(
                    painterResource(R.drawable.elevator_alert_monochrome),
                    null,
                    Modifier.height(24.dp),
                    colorFilter = ColorFilter.tint(headerTextColor)
                )
            } else if (firstRoute != null) {
                val (icon, description) = routeIcon(firstRoute)
                Image(
                    icon,
                    description,
                    Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(headerTextColor)
                )
            }
            Text(
                stringResource(R.string.alert_details),
                color = headerTextColor,
                fontWeight = FontWeight.Bold,
                style = Typography.headline
            )
            Spacer(Modifier.weight(1f))
            ActionButton(ActionButtonKind.Close) { goBack() }
        }
        if (alert != null) {
            AlertDetails(alert, line, routes, stop, affectedStops, now)
        } else {
            CircularProgressIndicator()
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun getAlert(
    alerts: AlertsStreamDataResponse?,
    alertId: String,
    goBack: () -> Unit
): Alert? {
    var result by rememberSaveable(saver = stateJsonSaver()) { mutableStateOf<Alert?>(null) }
    if (alerts == null) return result
    val newAlertData = alerts.alerts[alertId]
    // If no alert is already set, and no alert was found with the provided ID,
    // something went wrong, and the alert didn't exist in the data to begin with,
    // navigate back to the previous page.
    if (result == null && newAlertData == null) {
        goBack()
    }
    // If an alert is already set on the page, but doesn't exist in the data,
    // it probably expired while the page was open, so don't change anything and
    // keep displaying the expired alert as is.
    if (newAlertData != null) {
        result = newAlertData
    }
    return result
}
