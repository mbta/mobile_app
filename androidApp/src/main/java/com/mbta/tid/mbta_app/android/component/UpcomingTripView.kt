package com.mbta.tid.mbta_app.android.component

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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.generated.drawableByName
import com.mbta.tid.mbta_app.android.util.FormattedAlert
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.contentDescription
import com.mbta.tid.mbta_app.android.util.formattedTime
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.MinutesFormat
import kotlin.math.min
import kotlin.time.Duration.Companion.minutes

sealed interface UpcomingTripViewState {
    data object Loading : UpcomingTripViewState

    data class NoTrips(val format: UpcomingFormat.NoTripsFormat) : UpcomingTripViewState

    data class Disruption(val formattedAlert: FormattedAlert, val iconName: String) :
        UpcomingTripViewState

    data class Some(val trip: TripInstantDisplay) : UpcomingTripViewState
}

@Composable
fun UpcomingTripView(
    state: UpcomingTripViewState,
    modifier: Modifier = Modifier,
    routeType: RouteType? = null,
    isFirst: Boolean = true,
    isOnly: Boolean = true,
    hideRealtimeIndicators: Boolean = false,
    /**
     * The most opaque that text within this view is allowed to be. Useful for dimming normal times
     * without double-dimming disruptions and scheduled times.
     */
    maxTextAlpha: Float = 1.0f,
) {
    val modifier = modifier.widthIn(min = 48.dp).padding(vertical = 2.dp)
    val maxAlphaModifier = if (maxTextAlpha < 1.0f) Modifier.alpha(maxTextAlpha) else Modifier

    when (state) {
        is UpcomingTripViewState.Some -> {
            // TODO: actually pull through vehicle type
            val vehicleType = routeType?.typeText(LocalContext.current, isOnly) ?: ""
            val tripDescription = state.trip.contentDescription(isFirst, vehicleType)
            when (state.trip) {
                is TripInstantDisplay.Overridden ->
                    WithRealtimeIndicator(modifier.then(maxAlphaModifier), hideRealtimeIndicators) {
                        TightWrapText(
                            state.trip.text,
                            modifier = Modifier.placeholderIfLoading(),
                            style = Typography.footnote.merge(textAlign = TextAlign.End),
                        )
                    }

                is TripInstantDisplay.Hidden -> {}
                is TripInstantDisplay.Skipped -> {}
                is TripInstantDisplay.Boarding ->
                    WithRealtimeIndicator(modifier.then(maxAlphaModifier), hideRealtimeIndicators) {
                        Text(
                            stringResource(R.string.boarding_abbr),
                            Modifier.semantics { contentDescription = tripDescription }
                                .placeholderIfLoading(),
                            textAlign = TextAlign.End,
                            style = Typography.headlineBold,
                        )
                    }

                is TripInstantDisplay.Arriving ->
                    WithRealtimeIndicator(modifier.then(maxAlphaModifier), hideRealtimeIndicators) {
                        Text(
                            stringResource(R.string.arriving_abbr),
                            Modifier.semantics { contentDescription = tripDescription }
                                .placeholderIfLoading(),
                            textAlign = TextAlign.End,
                            style = Typography.headlineBold,
                        )
                    }

                is TripInstantDisplay.Now ->
                    WithRealtimeIndicator(modifier.then(maxAlphaModifier), hideRealtimeIndicators) {
                        Text(
                            stringResource(R.string.now),
                            Modifier.semantics { contentDescription = tripDescription }
                                .placeholderIfLoading(),
                            textAlign = TextAlign.End,
                            style = Typography.headlineBold,
                        )
                    }

                is TripInstantDisplay.Approaching ->
                    WithRealtimeIndicator(modifier.then(maxAlphaModifier), hideRealtimeIndicators) {
                        Text(
                            text =
                                AnnotatedString.fromHtml(stringResource(R.string.minutes_abbr, 1)),
                            modifier =
                                Modifier.semantics { contentDescription = tripDescription }
                                    .placeholderIfLoading(),
                        )
                    }

                is TripInstantDisplay.Time ->
                    WithRealtimeIndicator(modifier.then(maxAlphaModifier), hideRealtimeIndicators) {
                        Text(
                            state.trip.predictionTime.formattedTime(),
                            Modifier.semantics { contentDescription = tripDescription }
                                .placeholderIfLoading(),
                            textAlign = TextAlign.End,
                            style =
                                if (state.trip.headline) Typography.headlineSemibold
                                else Typography.footnoteSemibold,
                        )
                    }

                is TripInstantDisplay.TimeWithStatus ->
                    Column(
                        modifier.clearAndSetSemantics { contentDescription = tripDescription },
                        horizontalAlignment = Alignment.End,
                    ) {
                        WithRealtimeIndicator(
                            modifier.then(maxAlphaModifier),
                            hideRealtimeIndicators,
                        ) {
                            Text(
                                state.trip.predictionTime.formattedTime(),
                                Modifier.placeholderIfLoading(),
                                textAlign = TextAlign.End,
                                style =
                                    if (state.trip.headline) Typography.headlineSemibold
                                    else Typography.footnoteSemibold,
                            )
                        }
                        Text(
                            state.trip.status,
                            color = LocalContentColor.current.copy(alpha = min(maxTextAlpha, 0.6f)),
                            textAlign = TextAlign.End,
                            style = Typography.footnoteSemibold,
                        )
                    }

                is TripInstantDisplay.TimeWithSchedule ->
                    Column(
                        modifier.clearAndSetSemantics { contentDescription = tripDescription },
                        horizontalAlignment = Alignment.End,
                    ) {
                        WithRealtimeIndicator(
                            modifier.then(maxAlphaModifier),
                            hideRealtimeIndicators,
                        ) {
                            Text(
                                state.trip.predictionTime.formattedTime(),
                                Modifier.placeholderIfLoading(),
                                textAlign = TextAlign.End,
                                style =
                                    if (state.trip.headline) Typography.headlineSemibold
                                    else Typography.footnoteSemibold,
                            )
                        }
                        Text(
                            state.trip.scheduledTime.formattedTime(),
                            color = LocalContentColor.current.copy(alpha = min(maxTextAlpha, 0.6f)),
                            textAlign = TextAlign.End,
                            textDecoration = TextDecoration.LineThrough,
                            style = Typography.footnoteSemibold,
                        )
                    }

                is TripInstantDisplay.ScheduleTime ->
                    Text(
                        state.trip.scheduledTime.formattedTime(),
                        modifier
                            .alpha(min(maxTextAlpha, 0.6F))
                            .semantics { contentDescription = tripDescription }
                            .placeholderIfLoading(),
                        textAlign = TextAlign.End,
                        style =
                            if (state.trip.headline) Typography.headlineSemibold
                            else Typography.footnoteSemibold,
                    )

                is TripInstantDisplay.ScheduleTimeWithStatusColumn ->
                    Column(
                        modifier.clearAndSetSemantics { contentDescription = tripDescription },
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            state.trip.scheduledTime.formattedTime(),
                            modifier
                                .alpha(min(maxTextAlpha, 0.6F))
                                .semantics { contentDescription = tripDescription }
                                .placeholderIfLoading(),
                            textAlign = TextAlign.End,
                            style =
                                if (state.trip.headline) Typography.headlineSemibold
                                else Typography.footnoteSemibold,
                        )
                        Text(
                            state.trip.status,
                            color = LocalContentColor.current.copy(alpha = min(maxTextAlpha, 0.6f)),
                            textAlign = TextAlign.End,
                            style = Typography.footnoteSemibold,
                        )
                    }

                is TripInstantDisplay.ScheduleTimeWithStatusRow ->
                    Row(
                        modifier.clearAndSetSemantics { contentDescription = tripDescription },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            state.trip.status,
                            color = LocalContentColor.current.copy(alpha = min(maxTextAlpha, 0.6f)),
                            textAlign = TextAlign.End,
                            style = Typography.footnote,
                        )
                        Text(
                            state.trip.scheduledTime.formattedTime(),
                            modifier
                                .alpha(min(maxTextAlpha, 0.6F))
                                .semantics { contentDescription = tripDescription }
                                .placeholderIfLoading(),
                            textAlign = TextAlign.End,
                            style = Typography.footnoteSemibold,
                        )
                    }
                is TripInstantDisplay.Minutes ->
                    WithRealtimeIndicator(modifier.then(maxAlphaModifier), hideRealtimeIndicators) {
                        Text(
                            text =
                                AnnotatedString.fromHtml(predictionTextMinutes(state.trip.minutes)),
                            modifier =
                                Modifier.semantics { contentDescription = tripDescription }
                                    .placeholderIfLoading(),
                        )
                    }

                is TripInstantDisplay.ScheduleMinutes ->
                    Text(
                        text = AnnotatedString.fromHtml(predictionTextMinutes(state.trip.minutes)),
                        modifier =
                            modifier
                                .alpha(min(maxTextAlpha, 0.6F))
                                .semantics { contentDescription = tripDescription }
                                .placeholderIfLoading(),
                        textAlign = TextAlign.End,
                    )

                is TripInstantDisplay.Cancelled ->
                    Row(
                        modifier
                            .alpha(min(maxTextAlpha, 0.6f))
                            .semantics { contentDescription = tripDescription }
                            .placeholderIfLoading(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.cancelled),
                            color = colorResource(R.color.deemphasized),
                            textAlign = TextAlign.End,
                            style = Typography.footnote,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            state.trip.scheduledTime.formattedTime(),
                            color = colorResource(R.color.deemphasized),
                            textDecoration = TextDecoration.LineThrough,
                            textAlign = TextAlign.End,
                            style = Typography.footnoteSemibold,
                        )
                    }
            }
        }
        is UpcomingTripViewState.Disruption ->
            DisruptionView(
                state.formattedAlert.predictionReplacement,
                iconName = state.iconName,
                modifier,
                maxTextAlpha,
            )
        is UpcomingTripViewState.NoTrips ->
            when (state.format) {
                is UpcomingFormat.NoTripsFormat.PredictionsUnavailable ->
                    Text(
                        stringResource(R.string.no_predictions),
                        modifier.then(maxAlphaModifier),
                        textAlign = TextAlign.End,
                        style = Typography.footnote,
                    )
                is UpcomingFormat.NoTripsFormat.ServiceEndedToday ->
                    Text(
                        stringResource(R.string.service_ended),
                        modifier.then(maxAlphaModifier),
                        textAlign = TextAlign.End,
                        style = Typography.footnote,
                    )
                is UpcomingFormat.NoTripsFormat.NoSchedulesToday ->
                    Text(
                        stringResource(R.string.no_service_today),
                        modifier.then(maxAlphaModifier),
                        textAlign = TextAlign.End,
                        style = Typography.footnote,
                    )
            }
        is UpcomingTripViewState.Loading ->
            CompositionLocalProvider(IsLoadingSheetContents provides true) {
                UpcomingTripView(
                    UpcomingTripViewState.Some(TripInstantDisplay.Minutes(10)),
                    modifier.then(maxAlphaModifier).loadingShimmer().placeholderIfLoading(),
                    routeType,
                    isFirst,
                    isOnly,
                    hideRealtimeIndicators,
                )
            }
    }
}

@Composable
fun predictionTextMinutes(minutes: Int): String =
    when (val format = MinutesFormat.from(minutes)) {
        is MinutesFormat.Hour -> stringResource(R.string.exact_hours_format_abbr, format.hours)
        is MinutesFormat.HourMinute ->
            stringResource(R.string.hr_min_abbr, format.hours, format.minutes)
        is MinutesFormat.Minute -> stringResource(R.string.minutes_abbr, format.minutes)
    }

@Composable
fun DisruptionView(
    spec: FormattedAlert.PredictionReplacement,
    iconName: String,
    modifier: Modifier = Modifier,
    maxTextAlpha: Float = 1.0f,
) {
    val icon = painterResource(drawableByName(iconName))
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            spec.text,
            modifier =
                Modifier.alpha(min(maxTextAlpha, 0.6f))
                    .then(
                        spec.contentDescription?.let {
                            Modifier.semantics { contentDescription = it }
                        } ?: Modifier
                    ),
            style = Typography.footnoteSemibold,
        )
        Image(icon, null, Modifier.size(20.dp))
    }
}

@Preview
@Composable
fun UpcomingTripViewPreview() {
    MyApplicationTheme {
        Column(
            Modifier.background(colorResource(R.color.fill3)).padding(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Now))
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Minutes(5)))
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.ScheduleTime(EasternTimeInstant.now() + 10.minutes, true)
                )
            )
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.ScheduleTime(EasternTimeInstant.now() + 10.minutes)
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
        val format = UpcomingFormat.Disruption(alert, mapStopRoute = route)
        return UpcomingTripViewState.Disruption(FormattedAlert(alert), iconName = format.iconName)
    }

    MyApplicationTheme {
        Column(
            Modifier.background(colorResource(R.color.fill3)).padding(8.dp),
            horizontalAlignment = Alignment.End,
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
