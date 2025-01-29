package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.routeSlashIcon
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.RouteType

@Composable
fun StopDetailsNoTripCard(
    status: RealtimePatterns.NoTripsFormat,
    accentColor: Color,
    routeType: RouteType,
    hideMaps: Boolean
) {

    StopDetailsIconCard(
        accentColor = accentColor,
        details = DetailText(status, routeType, hideMaps),
        header = { modifier -> HeaderText(status, modifier) }
    ) { modifier ->
        HeaderImage(status, routeType, modifier)
    }
}

@Composable
private fun HeaderText(status: RealtimePatterns.NoTripsFormat, modifier: Modifier = Modifier) {
    when (status) {
        is RealtimePatterns.NoTripsFormat.PredictionsUnavailable ->
            Text(stringResource(R.string.no_predictions), modifier = modifier)
        is RealtimePatterns.NoTripsFormat.NoSchedulesToday ->
            Text(stringResource(R.string.no_service_today), modifier = modifier)
        is RealtimePatterns.NoTripsFormat.ServiceEndedToday ->
            Text(stringResource(R.string.service_ended), modifier = modifier)
    }
}

@Composable
private fun DetailText(
    status: RealtimePatterns.NoTripsFormat,
    routeType: RouteType,
    hideMaps: Boolean
): (@Composable() () -> Unit)? {
    val context = LocalContext.current

    var predictionsUnavailableString =
        stringResource(R.string.predictions_unavailable_details, routeType.typeText(context, false))

    var predictionsUnavailableStringNoMap =
        stringResource(R.string.predictions_unavailable_details_hide_maps)

    return when (status) {
        is RealtimePatterns.NoTripsFormat.PredictionsUnavailable -> {
            {
                Text(
                    if (hideMaps) {
                        predictionsUnavailableStringNoMap
                    } else {
                        predictionsUnavailableString
                    }
                )
            }
        }
        else -> null
    }
}

@Composable
private fun HeaderImage(
    status: RealtimePatterns.NoTripsFormat,
    routeType: RouteType,
    modifier: Modifier = Modifier
) {
    when (status) {
        is RealtimePatterns.NoTripsFormat.PredictionsUnavailable ->
            Icon(
                painterResource(R.drawable.live_data_slash),
                null,
                modifier = modifier.testTag("live_data_slash")
            )
        is RealtimePatterns.NoTripsFormat.NoSchedulesToday,
        RealtimePatterns.NoTripsFormat.ServiceEndedToday ->
            Icon(
                routeSlashIcon(routeType = routeType),
                null,
                modifier = modifier.testTag("route_slash_icon")
            )
    }
}
