package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbta.tid.mbta_app.android.R
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

    data class NoService(val effect: Alert.Effect) : UpcomingTripViewState

    data class Some(val trip: TripInstantDisplay) : UpcomingTripViewState
}

val format: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

fun formatTime(time: Instant): String =
    format.format(time.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime())

@Composable
fun UpcomingTripView(state: UpcomingTripViewState) {
    val modifier = Modifier.widthIn(min = 48.dp)
    when (state) {
        is UpcomingTripViewState.Some ->
            when (state.trip) {
                is TripInstantDisplay.Overridden -> Text(state.trip.text, modifier)
                is TripInstantDisplay.Hidden -> {}
                is TripInstantDisplay.Skipped -> {}
                is TripInstantDisplay.Boarding ->
                    Text(stringResource(R.string.boarding_abbr), modifier)
                is TripInstantDisplay.Arriving ->
                    Text(stringResource(R.string.arriving_abbr), modifier)
                is TripInstantDisplay.Approaching ->
                    Text(stringResource(R.string.approaching_abbr), modifier)
                is TripInstantDisplay.AsTime ->
                    Text(formatTime(state.trip.predictionTime), modifier)
                is TripInstantDisplay.Schedule ->
                    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTime(state.trip.scheduleTime))
                        Icon(painterResource(R.drawable.baseline_access_time_24), "Scheduled")
                    }
                is TripInstantDisplay.Minutes ->
                    Text(stringResource(R.string.minutes_abbr, state.trip.minutes), modifier)
            }
        is UpcomingTripViewState.NoService ->
            NoServiceView(NoServiceViewEffect.from(state.effect), modifier)
        is UpcomingTripViewState.None -> Text("No Predictions", modifier)
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
    Row {
        UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Minutes(5)))
        UpcomingTripView(
            UpcomingTripViewState.Some(TripInstantDisplay.Schedule(Clock.System.now() + 10.minutes))
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
