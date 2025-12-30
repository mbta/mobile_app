package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.routeSlashIcon
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.formattedServiceDayAndDate
import com.mbta.tid.mbta_app.android.util.formattedTime
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.response.NextScheduleResponse
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

@Composable
fun StopDetailsNoTripCard(
    status: UpcomingFormat.NoTripsFormat,
    accentColor: Color,
    directionLabel: String,
    routeType: RouteType,
    now: EasternTimeInstant,
    nextScheduleResponse: NextScheduleResponse?,
) {
    StopDetailsIconCard(
        accentColor = accentColor,
        details = detailText(status, directionLabel, routeType, now, nextScheduleResponse),
        header = { modifier -> HeaderText(status, modifier) },
    ) { modifier ->
        HeaderImage(status, routeType, modifier)
    }
}

@Composable
private fun HeaderText(status: UpcomingFormat.NoTripsFormat, modifier: Modifier = Modifier) {
    when (status) {
        is UpcomingFormat.NoTripsFormat.SubwayEarlyMorning ->
            Text(stringResource(R.string.good_morning), modifier = modifier)
        is UpcomingFormat.NoTripsFormat.PredictionsUnavailable ->
            Text(stringResource(R.string.no_predictions), modifier = modifier)
        is UpcomingFormat.NoTripsFormat.NoSchedulesToday ->
            Text(stringResource(R.string.no_service_today), modifier = modifier)
        is UpcomingFormat.NoTripsFormat.ServiceEndedToday ->
            Text(stringResource(R.string.service_ended), modifier = modifier)
    }
}

@Composable
private fun detailText(
    status: UpcomingFormat.NoTripsFormat,
    directionLabel: String,
    routeType: RouteType,
    now: EasternTimeInstant,
    nextScheduleResponse: NextScheduleResponse?,
): (@Composable () -> Unit)? {
    val resources = LocalResources.current
    val hideMaps = SettingsCache.get(Settings.HideMaps)

    var predictionsUnavailableString =
        stringResource(
            R.string.predictions_unavailable_details,
            routeType.typeText(resources, false),
        )

    var predictionsUnavailableStringNoMap =
        stringResource(R.string.predictions_unavailable_details_hide_maps)

    return when (status) {
        is UpcomingFormat.NoTripsFormat.PredictionsUnavailable -> {
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
        is UpcomingFormat.NoTripsFormat.SubwayEarlyMorning -> {
            {
                Text(
                    AnnotatedString.fromHtml(
                        stringResource(
                            R.string.subway_early_am_detail,
                            directionLabel,
                            status.scheduledTime.formattedTime(),
                        )
                    )
                )
            }
        }
        UpcomingFormat.NoTripsFormat.ServiceEndedToday,
        UpcomingFormat.NoTripsFormat.NoSchedulesToday -> {
            val nextScheduleTime = nextScheduleResponse?.nextSchedule?.stopTime
            if (nextScheduleTime != null) {
                {
                    when {
                        nextScheduleTime.serviceDate > now.serviceDate.plus(1, DateTimeUnit.DAY) ->
                            Text(
                                AnnotatedString.fromHtml(
                                    stringResource(
                                        R.string.next_trip_on,
                                        nextScheduleTime.formattedServiceDayAndDate(),
                                    )
                                )
                            )
                        nextScheduleTime.local.date > now.local.date ->
                            Text(
                                AnnotatedString.fromHtml(
                                    stringResource(
                                        R.string.next_trip_at_tomorrow,
                                        nextScheduleTime.formattedTime(),
                                    )
                                )
                            )
                        else ->
                            Text(
                                AnnotatedString.fromHtml(
                                    stringResource(
                                        R.string.next_trip_at,
                                        nextScheduleTime.formattedTime(),
                                    )
                                )
                            )
                    }
                }
            } else null
        }
    }
}

@Composable
private fun HeaderImage(
    status: UpcomingFormat.NoTripsFormat,
    routeType: RouteType,
    modifier: Modifier = Modifier,
) {
    when (status) {
        is UpcomingFormat.NoTripsFormat.SubwayEarlyMorning ->
            Icon(painterResource(R.drawable.sunrise), null, modifier = modifier.testTag("sunrise"))
        is UpcomingFormat.NoTripsFormat.PredictionsUnavailable ->
            Icon(
                painterResource(R.drawable.live_data_slash),
                null,
                modifier = modifier.testTag("live_data_slash"),
            )
        is UpcomingFormat.NoTripsFormat.NoSchedulesToday,
        UpcomingFormat.NoTripsFormat.ServiceEndedToday ->
            Icon(
                routeSlashIcon(routeType = routeType),
                null,
                modifier = modifier.testTag("route_slash_icon"),
            )
    }
}
