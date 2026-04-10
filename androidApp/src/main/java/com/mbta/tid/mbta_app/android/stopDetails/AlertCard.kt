package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.AlertIcon
import com.mbta.tid.mbta_app.android.component.routeSlashIcon
import com.mbta.tid.mbta_app.android.util.FormattedAlert
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertCardSpec
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopAlertState
import com.mbta.tid.mbta_app.model.TripSpecificAlertSummary
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.datetime.Month

@Composable
private fun TakeoverAlertCard(
    alert: Alert,
    alertSummary: AlertSummary?,
    routeAccents: TripRouteAccents,
    onViewDetails: (() -> Unit)?,
    modifier: Modifier = Modifier,
    interiorPadding: PaddingValues = PaddingValues(0.dp),
) {

    val alertState = alert.alertState
    val iconSize = 48.dp
    val formattedAlert = FormattedAlert(alert, alertSummary)

    Column(
        modifier =
            modifier
                .haloContainer(borderWidth = 2.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .padding(interiorPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlertIcon(
                alertState = alertState,
                color = routeAccents.color,
                modifier =
                    Modifier.clearAndSetSemantics {}
                        .size(iconSize)
                        .align(Alignment.CenterVertically),
                overrideIcon =
                    if (alert.effect == Alert.Effect.Cancellation) routeSlashIcon(routeAccents.type)
                    else null,
            )
            Text(
                formattedAlert.alertCardHeader(AlertCardSpec.Takeover, routeAccents.type),
                Modifier.weight(1f),
                style = Typography.title2Bold,
            )
        }
        HorizontalDivider(color = routeAccents.color.copy(alpha = 0.25f), thickness = 2.dp)
        Text(formattedAlert.alertCardMajorBody, style = Typography.callout)
        onViewDetails?.let {
            TextButton(
                it,
                shape = RoundedCornerShape(8.dp),
                colors =
                    ButtonColors(
                        routeAccents.color,
                        routeAccents.color,
                        routeAccents.color,
                        routeAccents.color,
                    ),
                modifier = Modifier.heightIn(min = 44.dp).fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.view_details),
                    color = routeAccents.textColor,
                    modifier = Modifier.padding(4.dp),
                    style = Typography.bodySemibold,
                )
            }
        }
    }
}

@Composable
fun AlertCard(
    alert: Alert,
    alertSummary: AlertSummary?,
    cardSpec: AlertCardSpec,
    routeAccents: TripRouteAccents,
    onViewDetails: (() -> Unit)?,
    modifier: Modifier = Modifier,
    interiorPadding: PaddingValues = PaddingValues(0.dp),
) {

    if (cardSpec == AlertCardSpec.Takeover) {
        TakeoverAlertCard(
            alert,
            alertSummary,
            routeAccents,
            onViewDetails,
            modifier,
            interiorPadding,
        )
    } else {
        val formattedAlert = FormattedAlert(alert, alertSummary)

        val iconSize =
            when (cardSpec) {
                AlertCardSpec.Elevator -> 36.dp
                else -> 20.dp
            }

        Column(
            modifier =
                modifier
                    .then(
                        if (onViewDetails != null) Modifier.clickable { onViewDetails() }
                        else Modifier
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .padding(interiorPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val alertState =
                    if (alertSummary is AlertSummary.AllClear) StopAlertState.AllClear
                    else alert.alertState
                val iconSize = if (alertState == StopAlertState.AllClear) 36.dp else iconSize
                AlertIcon(
                    alertState = alertState,
                    color = routeAccents.color,
                    modifier =
                        Modifier.clearAndSetSemantics {}
                            .size(iconSize)
                            .align(Alignment.CenterVertically),
                    overrideIcon =
                        if (alert.effect == Alert.Effect.Cancellation)
                            routeSlashIcon(routeAccents.type)
                        else null,
                )
                Text(
                    formattedAlert.alertCardHeader(cardSpec, routeAccents.type),
                    Modifier.weight(1f),
                    style = Typography.callout,
                )
                Icon(
                    painterResource(R.drawable.fa_chevron_right),
                    contentDescription = null,
                    Modifier.size(16.dp),
                    tint = colorResource(R.color.deemphasized),
                )
            }
        }
    }
}

@Composable
@Preview
fun AlertCardPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.background(Color.Blue),
    ) {
        AlertCard(
            ObjectCollectionBuilder.Single.alert({
                header = "Orange Line suspended from point A to point B"
                effect = Alert.Effect.Suspension
            }),
            null,
            AlertCardSpec.Takeover,
            routeAccents =
                TripRouteAccents(
                    color = Color.fromHex("ED8B00"),
                    textColor = Color.fromHex("FFFFFF"),
                    type = RouteType.HEAVY_RAIL,
                ),
            onViewDetails = {},
        )

        AlertCard(
            ObjectCollectionBuilder.Single.alert { effect = Alert.Effect.Cancellation },
            TripSpecificAlertSummary(
                TripSpecificAlertSummary.TripFrom(
                    EasternTimeInstant(2026, Month.MARCH, 10, 22, 17),
                    "Mansfield",
                ),
                Alert.Effect.Cancellation,
                cause = Alert.Cause.Holiday,
            ),
            AlertCardSpec.Takeover,
            routeAccents =
                TripRouteAccents(
                    color = Color.fromHex("80276C"),
                    textColor = Color.fromHex("FFFFFF"),
                    type = RouteType.COMMUTER_RAIL,
                ),
            onViewDetails = {},
        )

        AlertCard(
            ObjectCollectionBuilder.Single.alert({ effect = Alert.Effect.ServiceChange }),
            null,
            AlertCardSpec.Basic,
            routeAccents =
                TripRouteAccents(
                    color = Color.fromHex("80276C"),
                    textColor = Color.fromHex("FFFFFF"),
                    type = RouteType.COMMUTER_RAIL,
                ),
            onViewDetails = {},
        )

        AlertCard(
            ObjectCollectionBuilder.Single.alert({
                effect = Alert.Effect.ElevatorClosure
                header =
                    "Ruggles Elevator 848 (Lobby to lower busway side platform) unavailable due to maintenance"
            }),
            null,
            AlertCardSpec.Elevator,
            routeAccents =
                TripRouteAccents(
                    color = Color.fromHex("ED8B00"),
                    textColor = Color.fromHex("FFFFFF"),
                    type = RouteType.HEAVY_RAIL,
                ),
            onViewDetails = {},
        )

        AlertListContainer(
            highPriority =
                listOf({ modifier ->
                    AlertCard(
                        ObjectCollectionBuilder.Single.alert({
                            effect = Alert.Effect.Delay
                            cause = Alert.Cause.DrawbridgeIssue
                        }),
                        AlertSummary.Standard(effect = Alert.Effect.Delay),
                        AlertCardSpec.Delay,
                        routeAccents =
                            TripRouteAccents(
                                color = Color.fromHex("ED8B00"),
                                textColor = Color.fromHex("FFFFFF"),
                                type = RouteType.HEAVY_RAIL,
                            ),
                        onViewDetails = {},
                        modifier = modifier,
                    )
                })
        )
    }
}
