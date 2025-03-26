package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.model.RouteCardData

@Composable
fun StopHeader(data: RouteCardData.RouteStopData, showElevatorAccessibility: Boolean) {

    val isWheelchairAccessible = data.stop.isWheelchairAccessible

    val showAccessible = showElevatorAccessibility && isWheelchairAccessible
    val showInaccessible = showElevatorAccessibility && !isWheelchairAccessible
    val showElevatorAlerts = showElevatorAccessibility && data.hasElevatorAlerts

    Row(
        Modifier.background(colorResource(id = R.color.fill2))
            .fillMaxWidth()
            .padding(top = 11.dp, bottom = 11.dp, start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showElevatorAlerts) {
                    Image(
                        painterResource(R.drawable.elevator_alert),
                        null,
                        modifier =
                            Modifier.height(24.dp)
                                .placeholderIfLoading()
                                .testTag("elevator_alert")
                                .clearAndSetSemantics {}
                    )
                } else if (showAccessible) {
                    Image(
                        painterResource(R.drawable.wheelchair_accessible),
                        null,
                        modifier =
                            Modifier.height(24.dp)
                                .placeholderIfLoading()
                                .testTag("wheelchair_accessible")
                                .clearAndSetSemantics {}
                    )
                }
                Text(
                    text = data.stop.name,
                    modifier =
                        Modifier.placeholderIfLoading()
                            .padding(start = if (showAccessible) 0.dp else 8.dp)
                            .fillMaxWidth(),
                    overflow = TextOverflow.Visible,
                    style = Typography.callout
                )
            }
            if (showInaccessible || showElevatorAlerts) {
                Text(
                    text =
                        if (showInaccessible) stringResource(R.string.not_accessible)
                        else
                            pluralStringResource(
                                R.plurals.elevator_closure_count,
                                data.elevatorAlerts.size,
                                data.elevatorAlerts.size
                            ),
                    modifier =
                        Modifier.placeholderIfLoading()
                            .alpha(0.5f)
                            .padding(start = 8.dp)
                            .fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    overflow = TextOverflow.Visible,
                    style = Typography.footnoteSemibold
                )
            }
        }
    }
}
