package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.PillDecoration
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.TripStatus
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.RoutePillSpec
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripAndFormat

@Composable
fun StopDetailsDepartureTile(
    tileData: TripAndFormat,
    onTap: () -> Unit,
    pillDecoration: PillDecoration? = null,
    showHeadsign: Boolean = true,
    isSelected: Boolean = false
) {
    Button(
        onClick = onTap,
        modifier = Modifier.padding(10.dp).defaultMinSize(minHeight = 56.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor =
                    if (isSelected) {
                        colorResource(R.color.fill3)
                    } else {
                        if (
                            tileData.formatted.routeType == RouteType.COMMUTER_RAIL ||
                                tileData.upcoming.trip.routeId == "Blue"
                        ) {
                            colorResource(R.color.deselected_toggle_2)
                        } else {
                            colorResource(R.color.deselected_toggle_1)
                        }
                    },
                contentColor =
                    if (isSelected) colorResource(R.color.text)
                    else colorResource(R.color.deselected_toggle_text)
            ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            if (showHeadsign) {
                Text(
                    text = tileData.upcoming.trip.headsign,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start
                )
            }
            Row {
                if (pillDecoration is PillDecoration.OnPrediction) {
                    val route = pillDecoration.routesByTrip[tileData.upcoming.trip.id]
                    RoutePill(route = route, type = RoutePillSpec.Type.Flex)
                    Spacer(Modifier.width(8.dp))
                }
                TripStatus(
                    predictions =
                        tileData.formatted.let {
                            RealtimePatterns.Format.Some(listOf(tileData.formatted), null)
                        }
                )
            }
        }
    }
}
