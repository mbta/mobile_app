package com.mbta.tid.mbta_app.android.component

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
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
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.UpcomingTripAccessibilityFormatters
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.Alert
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

    data class Disruption(val effect: Alert.Effect) : UpcomingTripViewState

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
                            style = MaterialTheme.typography.headlineMedium,
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
                            style = MaterialTheme.typography.headlineMedium,
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
                            style = MaterialTheme.typography.headlineMedium,
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
                            style = MaterialTheme.typography.headlineMedium,
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
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
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
                        style = MaterialTheme.typography.headlineMedium,
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
                            style = MaterialTheme.typography.headlineMedium,
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
                        style = MaterialTheme.typography.headlineMedium,
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
                            style = MaterialTheme.typography.headlineMedium,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            formatTime(state.trip.scheduledTime),
                            color = colorResource(R.color.deemphasized),
                            textAlign = TextAlign.End,
                            style =
                                TextStyle(textDecoration = TextDecoration.LineThrough)
                                    .merge(MaterialTheme.typography.headlineMedium),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
            }
        is UpcomingTripViewState.Disruption ->
            DisruptionView(DisruptionViewEffect.from(state.effect), modifier)
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

enum class DisruptionViewEffect {
    Detour,
    Shuttle,
    StopClosed,
    Suspension,
    Unknown;

    companion object {
        fun from(effect: Alert.Effect) =
            when (effect) {
                Alert.Effect.Detour -> Detour
                Alert.Effect.Shuttle -> Shuttle
                Alert.Effect.StationClosure,
                Alert.Effect.StopClosure -> StopClosed
                Alert.Effect.Suspension -> Suspension
                else -> Unknown
            }
    }
}

@Composable
fun DisruptionView(effect: DisruptionViewEffect, modifier: Modifier = Modifier) {
    val text =
        when (effect) {
            DisruptionViewEffect.Detour -> stringResource(R.string.detour)
            DisruptionViewEffect.Shuttle -> stringResource(R.string.shuttle)
            DisruptionViewEffect.StopClosed -> stringResource(R.string.stop_closed)
            DisruptionViewEffect.Suspension -> stringResource(R.string.suspension)
            DisruptionViewEffect.Unknown -> stringResource(R.string.no_service)
        }
    val icon =
        when (effect) {
            DisruptionViewEffect.Detour -> painterResource(R.drawable.baseline_warning_24)
            DisruptionViewEffect.Shuttle -> painterResource(R.drawable.baseline_directions_bus_24)
            DisruptionViewEffect.StopClosed ->
                painterResource(R.drawable.baseline_report_gmailerrorred_24)
            DisruptionViewEffect.Suspension -> painterResource(R.drawable.baseline_warning_24)
            DisruptionViewEffect.Unknown -> painterResource(R.drawable.baseline_question_mark_24)
        }
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text.uppercase(), fontSize = 12.sp)
        Icon(icon, null)
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
    Column(horizontalAlignment = Alignment.End) {
        DisruptionView(DisruptionViewEffect.Detour)
        DisruptionView(DisruptionViewEffect.Shuttle)
        DisruptionView(DisruptionViewEffect.StopClosed)
        DisruptionView(DisruptionViewEffect.Suspension)
    }
}
