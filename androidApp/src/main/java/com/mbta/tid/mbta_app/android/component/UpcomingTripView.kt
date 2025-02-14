package com.mbta.tid.mbta_app.android.component

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.generated.drawableByName
import com.mbta.tid.mbta_app.android.util.FormattedAlert
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.UpcomingTripAccessibilityFormatters
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime

sealed interface UpcomingTripViewState {
    data object Loading : UpcomingTripViewState

    data class NoTrips(val format: RealtimePatterns.NoTripsFormat) : UpcomingTripViewState

    data class Disruption(val formattedAlert: FormattedAlert, val iconName: String) :
        UpcomingTripViewState

    data class Some(val trip: TripInstantDisplay) : UpcomingTripViewState
}

val format: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

fun formatTime(time: Instant): String =
    format.format(time.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime())

@Composable
fun UpcomingTripView(
    state: UpcomingTripViewState,
    modifier: Modifier = Modifier,
    routeType: RouteType? = null,
    isFirst: Boolean = true,
    isOnly: Boolean = true,
    hideRealtimeIndicators: Boolean = false
) {
    val modifier = modifier.widthIn(min = 48.dp).padding(vertical = 2.dp)
    val context = LocalContext.current
    // TODO: actually pull through vehicle type
    val vehicleType = routeType?.typeText(context, isOnly) ?: ""
    when (state) {
        is UpcomingTripViewState.Some ->
            when (state.trip) {
                is TripInstantDisplay.Overridden ->
                    WithRealtimeIndicator(modifier, hideRealtimeIndicators) {
                        Text(
                            state.trip.text,
                            fontSize = 13.sp,
                            modifier = Modifier.placeholderIfLoading(),
                            textAlign = TextAlign.End
                        )
                    }
                is TripInstantDisplay.Hidden -> {}
                is TripInstantDisplay.Skipped -> {}
                is TripInstantDisplay.Boarding ->
                    WithRealtimeIndicator(modifier, hideRealtimeIndicators) {
                        Text(
                            stringResource(R.string.boarding_abbr),
                            Modifier.semantics {
                                    contentDescription =
                                        UpcomingTripAccessibilityFormatters.boardingLabel(
                                            context = context,
                                            isFirst = isFirst,
                                            vehicleType = vehicleType
                                        )
                                }
                                .placeholderIfLoading(),
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Bold
                        )
                    }
                is TripInstantDisplay.Arriving ->
                    WithRealtimeIndicator(modifier, hideRealtimeIndicators) {
                        Text(
                            stringResource(R.string.arriving_abbr),
                            Modifier.semantics {
                                    contentDescription =
                                        UpcomingTripAccessibilityFormatters.arrivingLabel(
                                            context,
                                            isFirst,
                                            vehicleType
                                        )
                                }
                                .placeholderIfLoading(),
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Bold
                        )
                    }
                is TripInstantDisplay.Now ->
                    WithRealtimeIndicator(modifier, hideRealtimeIndicators) {
                        Text(
                            stringResource(R.string.now),
                            Modifier.semantics {
                                    contentDescription =
                                        UpcomingTripAccessibilityFormatters.arrivingLabel(
                                            context,
                                            isFirst,
                                            vehicleType
                                        )
                                }
                                .placeholderIfLoading(),
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Bold
                        )
                    }
                is TripInstantDisplay.Approaching ->
                    WithRealtimeIndicator(modifier, hideRealtimeIndicators) {
                        Text(
                            text =
                                AnnotatedString.fromHtml(stringResource(R.string.minutes_abbr, 1)),
                            modifier =
                                Modifier.semantics {
                                        contentDescription =
                                            UpcomingTripAccessibilityFormatters
                                                .predictedMinutesLabel(
                                                    context,
                                                    minutes = 1,
                                                    isFirst,
                                                    vehicleType
                                                )
                                    }
                                    .placeholderIfLoading(),
                        )
                    }
                is TripInstantDisplay.Time ->
                    WithRealtimeIndicator(modifier, hideRealtimeIndicators) {
                        Text(
                            formatTime(state.trip.predictionTime),
                            Modifier.semantics {
                                    contentDescription =
                                        UpcomingTripAccessibilityFormatters.predictedTimeLabel(
                                            context,
                                            time = formatTime(state.trip.predictionTime),
                                            isFirst,
                                            vehicleType
                                        )
                                }
                                .placeholderIfLoading(),
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Bold
                        )
                    }
                is TripInstantDisplay.TimeWithStatus ->
                    Column(modifier, horizontalAlignment = Alignment.End) {
                        WithRealtimeIndicator(modifier, hideRealtimeIndicators) {
                            Text(
                                formatTime(state.trip.predictionTime),
                                Modifier.semantics {
                                        contentDescription =
                                            UpcomingTripAccessibilityFormatters.predictedTimeLabel(
                                                context,
                                                time = formatTime(state.trip.predictionTime),
                                                isFirst,
                                                vehicleType
                                            )
                                    }
                                    .placeholderIfLoading(),
                                textAlign = TextAlign.End,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            state.trip.status,
                            color = LocalContentColor.current.copy(alpha = 0.6f),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                is TripInstantDisplay.ScheduleTime ->
                    Text(
                        formatTime(state.trip.scheduledTime),
                        modifier
                            .alpha(0.6F)
                            .semantics {
                                contentDescription =
                                    UpcomingTripAccessibilityFormatters.scheduledTimeLabel(
                                        context,
                                        time = formatTime(state.trip.scheduledTime),
                                        isFirst,
                                        vehicleType
                                    )
                            }
                            .placeholderIfLoading(),
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.Bold,
                        fontSize =
                            if (state.trip.headline) {
                                TextUnit.Unspecified
                            } else {
                                13.sp
                            }
                    )
                is TripInstantDisplay.Minutes ->
                    WithRealtimeIndicator(modifier, hideRealtimeIndicators) {
                        Text(
                            text =
                                AnnotatedString.fromHtml(
                                    predictionTextMinutes(context, state.trip.minutes)
                                ),
                            modifier =
                                Modifier.semantics {
                                        contentDescription =
                                            UpcomingTripAccessibilityFormatters
                                                .predictedMinutesLabel(
                                                    context,
                                                    minutes = state.trip.minutes,
                                                    isFirst,
                                                    vehicleType
                                                )
                                    }
                                    .placeholderIfLoading(),
                        )
                    }
                is TripInstantDisplay.ScheduleMinutes ->
                    Text(
                        text =
                            AnnotatedString.fromHtml(
                                stringResource(R.string.minutes_abbr, state.trip.minutes)
                            ),
                        modifier =
                            modifier
                                .alpha(0.6F)
                                .semantics {
                                    contentDescription =
                                        UpcomingTripAccessibilityFormatters.scheduledMinutesLabel(
                                            context,
                                            minutes = state.trip.minutes,
                                            isFirst,
                                            vehicleType
                                        )
                                }
                                .placeholderIfLoading(),
                        textAlign = TextAlign.End,
                    )
                is TripInstantDisplay.Cancelled ->
                    Row(
                        modifier
                            .semantics {
                                contentDescription =
                                    UpcomingTripAccessibilityFormatters.cancelledLabel(
                                        context,
                                        scheduledTime = formatTime(state.trip.scheduledTime),
                                        isFirst,
                                        vehicleType
                                    )
                            }
                            .placeholderIfLoading(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.cancelled),
                            color = colorResource(R.color.deemphasized),
                            textAlign = TextAlign.End,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            formatTime(state.trip.scheduledTime),
                            color = colorResource(R.color.deemphasized),
                            textAlign = TextAlign.End,
                            style =
                                TextStyle(textDecoration = TextDecoration.LineThrough)
                                    .merge(LocalTextStyle.current),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
            }
        is UpcomingTripViewState.Disruption ->
            DisruptionView(
                state.formattedAlert.predictionReplacement,
                iconName = state.iconName,
                modifier
            )
        is UpcomingTripViewState.NoTrips ->
            when (state.format) {
                is RealtimePatterns.NoTripsFormat.PredictionsUnavailable ->
                    Text(
                        stringResource(R.string.no_predictions),
                        modifier,
                        fontSize = 13.sp,
                        textAlign = TextAlign.End
                    )
                is RealtimePatterns.NoTripsFormat.ServiceEndedToday ->
                    Text(
                        stringResource(R.string.service_ended),
                        modifier,
                        fontSize = 13.sp,
                        textAlign = TextAlign.End
                    )
                is RealtimePatterns.NoTripsFormat.NoSchedulesToday ->
                    Text(
                        stringResource(R.string.no_service_today),
                        modifier,
                        fontSize = 13.sp,
                        textAlign = TextAlign.End
                    )
            }
        is UpcomingTripViewState.Loading ->
            CompositionLocalProvider(IsLoadingSheetContents provides true) {
                UpcomingTripView(
                    UpcomingTripViewState.Some(TripInstantDisplay.Minutes(10)),
                    modifier.loadingShimmer().placeholderIfLoading(),
                    routeType,
                    isFirst,
                    isOnly,
                    hideRealtimeIndicators
                )
            }
    }
}

fun predictionTextMinutes(context: Context, minutes: Int): String {
    val hours = Math.floorDiv(minutes, 60)
    val remainingMinutes = minutes - (hours * 60)

    return if (hours >= 1) {
        if (remainingMinutes == 0) {
            context.getString(R.string.exact_hours_format_abbr, hours)
        } else {
            context.getString(R.string.hr_min_abbr, hours, remainingMinutes)
        }
    } else {
        context.getString(R.string.minutes_abbr, minutes)
    }
}

@Composable
fun DisruptionView(
    spec: FormattedAlert.PredictionReplacement,
    iconName: String,
    modifier: Modifier = Modifier
) {
    val icon = painterResource(drawableByName(iconName))
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            spec.text,
            modifier =
                Modifier.alpha(0.6f)
                    .then(
                        spec.contentDescription?.let {
                            Modifier.semantics { contentDescription = it }
                        }
                            ?: Modifier
                    ),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge
        )
        Image(icon, null, Modifier.size(20.dp))
    }
}

@Preview
@Composable
fun UpcomingTripViewPreview() {
    MyApplicationTheme {
        Column(horizontalAlignment = Alignment.End) {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Now))
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Minutes(5)))
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.ScheduleTime(Clock.System.now() + 10.minutes, true)
                )
            )
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.ScheduleTime(Clock.System.now() + 10.minutes)
                )
            )
            UpcomingTripView(UpcomingTripViewState.Loading)
        }
    }
}

@Preview
@Composable
fun DisruptionViewPreview() {
    val route = MapStopRoute.ORANGE

    fun disruption(effect: Alert.Effect): UpcomingTripViewState {
        val alert = Single.alert { this.effect = effect }
        val format = RealtimePatterns.Format.Disruption(alert, mapStopRoute = route)
        return UpcomingTripViewState.Disruption(FormattedAlert(alert), iconName = format.iconName)
    }

    MyApplicationTheme {
        Column(
            Modifier.background(colorResource(R.color.fill3)).padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            UpcomingTripView(disruption(Alert.Effect.Suspension))
            UpcomingTripView(disruption(Alert.Effect.StopClosure))
            UpcomingTripView(disruption(Alert.Effect.StationClosure))
            UpcomingTripView(disruption(Alert.Effect.DockClosure))
            UpcomingTripView(disruption(Alert.Effect.Detour))
            UpcomingTripView(disruption(Alert.Effect.SnowRoute))
            UpcomingTripView(disruption(Alert.Effect.Shuttle))
        }
    }
}
