package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.UpcomingTripAccessibilityFormatters
import com.mbta.tid.mbta_app.model.Alert
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

    data object None : UpcomingTripViewState

    data object NoSchedulesToday : UpcomingTripViewState

    data object ServiceEndedToday : UpcomingTripViewState

    data class NoService(val effect: Alert.Effect) : UpcomingTripViewState

    data class Some(val trip: TripInstantDisplay) : UpcomingTripViewState
}

val format: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

fun formatTime(time: Instant): String =
    format.format(time.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime())

@Composable
fun UpcomingTripView(
    state: UpcomingTripViewState,
    isFirst: Boolean = true,
    isOnly: Boolean = true,
    hideRealtimeIndicators: Boolean = false
) {
    val modifier = Modifier.widthIn(min = 48.dp).padding(bottom = 4.dp)
    val context = LocalContext.current
    // TODO: actually pull through vehicle type
    val vehicleType = ""
    when (state) {
        is UpcomingTripViewState.Some ->
            when (state.trip) {
                is TripInstantDisplay.Overridden ->
                    WithRealtimeIndicator(modifier, hideRealtimeIndicators) {
                        Text(state.trip.text, fontSize = 13.sp)
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
                            },
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
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
                            },
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
                            },
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
                                        UpcomingTripAccessibilityFormatters.predictedMinutesLabel(
                                            context,
                                            minutes = 1,
                                            isFirst,
                                            vehicleType
                                        )
                                },
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
                            },
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                is TripInstantDisplay.ScheduleTime ->
                    Text(
                        formatTime(state.trip.scheduledTime),
                        modifier.alpha(0.6F).semantics {
                            contentDescription =
                                UpcomingTripAccessibilityFormatters.scheduledTimeLabel(
                                    context,
                                    time = formatTime(state.trip.scheduledTime),
                                    isFirst,
                                    vehicleType
                                )
                        },
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
                                    stringResource(R.string.minutes_abbr, state.trip.minutes)
                                ),
                            modifier =
                                Modifier.semantics {
                                    contentDescription =
                                        UpcomingTripAccessibilityFormatters.predictedMinutesLabel(
                                            context,
                                            minutes = state.trip.minutes,
                                            isFirst,
                                            vehicleType
                                        )
                                },
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
                            modifier.alpha(0.6F).semantics {
                                contentDescription =
                                    UpcomingTripAccessibilityFormatters.scheduledMinutesLabel(
                                        context,
                                        minutes = state.trip.minutes,
                                        isFirst,
                                        vehicleType
                                    )
                            },
                        style = MaterialTheme.typography.headlineMedium,
                    )
                is TripInstantDisplay.Cancelled ->
                    Row(
                        modifier.semantics {
                            contentDescription =
                                UpcomingTripAccessibilityFormatters.cancelledLabel(
                                    context,
                                    scheduledTime = formatTime(state.trip.scheduledTime),
                                    isFirst,
                                    vehicleType
                                )
                        },
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
        is UpcomingTripViewState.NoService ->
            NoServiceView(NoServiceViewEffect.from(state.effect), modifier)
        is UpcomingTripViewState.None ->
            Text(stringResource(R.string.no_predictions), modifier, fontSize = 13.sp)
        is UpcomingTripViewState.ServiceEndedToday ->
            Text(stringResource(R.string.service_ended), modifier, fontSize = 13.sp)
        is UpcomingTripViewState.NoSchedulesToday ->
            Text(stringResource(R.string.no_service_today), modifier, fontSize = 13.sp)
        is UpcomingTripViewState.Loading -> CircularProgressIndicator(modifier)
    }
}

enum class NoServiceViewEffect {
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
fun NoServiceView(effect: NoServiceViewEffect, modifier: Modifier = Modifier) {
    val text =
        when (effect) {
            NoServiceViewEffect.Detour -> stringResource(R.string.detour)
            NoServiceViewEffect.Shuttle -> stringResource(R.string.shuttle)
            NoServiceViewEffect.StopClosed -> stringResource(R.string.stop_closed)
            NoServiceViewEffect.Suspension -> stringResource(R.string.suspension)
            NoServiceViewEffect.Unknown -> stringResource(R.string.no_service)
        }
    val icon =
        when (effect) {
            NoServiceViewEffect.Detour -> painterResource(R.drawable.baseline_circle_24)
            NoServiceViewEffect.Shuttle -> painterResource(R.drawable.baseline_directions_bus_24)
            NoServiceViewEffect.StopClosed ->
                painterResource(R.drawable.baseline_report_gmailerrorred_24)
            NoServiceViewEffect.Suspension -> painterResource(R.drawable.baseline_warning_24)
            NoServiceViewEffect.Unknown -> painterResource(R.drawable.baseline_question_mark_24)
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
    }
}

@Preview
@Composable
fun NoServiceViewPreview() {
    Column(horizontalAlignment = Alignment.End) {
        NoServiceView(NoServiceViewEffect.Detour)
        NoServiceView(NoServiceViewEffect.Shuttle)
        NoServiceView(NoServiceViewEffect.StopClosed)
        NoServiceView(NoServiceViewEffect.Suspension)
    }
}
