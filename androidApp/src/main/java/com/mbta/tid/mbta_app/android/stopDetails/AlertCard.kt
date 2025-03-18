package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.AlertIcon
import com.mbta.tid.mbta_app.android.component.InfoCircle
import com.mbta.tid.mbta_app.android.util.FormattedAlert
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder

enum class AlertCardSpec {
    Major,
    Downstream,
    Secondary,
    Elevator,
    Delay
}

@Composable
fun AlertCard(
    alert: Alert,
    spec: AlertCardSpec,
    color: Color,
    textColor: Color,
    onViewDetails: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val formattedAlert = FormattedAlert(alert)
    val headerText: String =
        when (spec) {
            AlertCardSpec.Downstream -> formattedAlert.downstreamEffect
            AlertCardSpec.Elevator -> alert.header ?: formattedAlert.effect
            AlertCardSpec.Delay ->
                formattedAlert.cause?.let { stringResource(R.string.delays_due_to_cause, it) }
                    ?: stringResource(R.string.delays_unknown_reason)
            else -> formattedAlert.effect
        }
    val iconSize =
        when (spec) {
            AlertCardSpec.Major -> 48.dp
            AlertCardSpec.Elevator -> 28.dp
            else -> 20.dp
        }
    Column(
        modifier =
            modifier
                .haloContainer(2.dp)
                .clickable {
                    if (spec != AlertCardSpec.Major && onViewDetails != null) onViewDetails()
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlertIcon(
                alertState = alert.alertState,
                color = color,
                modifier =
                    Modifier.size(iconSize)
                        .align(
                            if (spec == AlertCardSpec.Elevator) Alignment.Top
                            else Alignment.CenterVertically
                        )
            )
            Text(
                headerText,
                Modifier.weight(1f),
                style =
                    if (spec == AlertCardSpec.Major) Typography.title2Bold
                    else Typography.bodySemibold
            )
            if (spec != AlertCardSpec.Major) {
                InfoCircle()
            }
        }
        if (spec == AlertCardSpec.Major) {
            HorizontalDivider(color = color.copy(alpha = 0.25f), thickness = 2.dp)
            Text(alert.header ?: "", style = Typography.callout)
            onViewDetails?.let {
                TextButton(
                    it,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonColors(color, color, color, color),
                    modifier = Modifier.heightIn(min = 44.dp).fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.view_details),
                        color = textColor,
                        modifier = Modifier.padding(4.dp),
                        style = Typography.bodySemibold
                    )
                }
            }
        }
    }
}

@Composable
@Preview
fun AlertCardPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AlertCard(
            ObjectCollectionBuilder.Single.alert({
                header = "Orange Line suspended from point A to point B"
                effect = Alert.Effect.Suspension
            }),
            AlertCardSpec.Major,
            textColor = Color.fromHex("FFFFFF"),
            color = Color.fromHex("ED8B00"),
            onViewDetails = {}
        )
        AlertCard(
            ObjectCollectionBuilder.Single.alert({ effect = Alert.Effect.ServiceChange }),
            AlertCardSpec.Secondary,
            textColor = Color.fromHex("FFFFFF"),
            color = Color.fromHex("80276C"),
            onViewDetails = {}
        )
        AlertCard(
            ObjectCollectionBuilder.Single.alert({
                effect = Alert.Effect.ElevatorClosure
                header =
                    "Ruggles Elevator 848 (Lobby to lower busway side platform) unavailable due to maintenance"
            }),
            AlertCardSpec.Elevator,
            textColor = Color.fromHex("FFFFFF"),
            color = Color.fromHex("ED8B00"),
            onViewDetails = {}
        )
    }
}
