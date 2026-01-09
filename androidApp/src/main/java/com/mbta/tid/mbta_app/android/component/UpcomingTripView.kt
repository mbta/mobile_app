package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalResources
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
    val dimmedAlpha = min(maxTextAlpha, 0.6f)

    val showRealtimeIcon = !hideRealtimeIndicators

    when (state) {
        is UpcomingTripViewState.Some -> {
            // TODO: actually pull through vehicle type
            val vehicleType = routeType?.typeText(LocalResources.current, isOnly) ?: ""
            val tripDescription = state.trip.contentDescription(isFirst, vehicleType)

            when (state.trip) {
                is TripInstantDisplay.Overridden ->
                    WithStatusIndicators(
                        modifier,
                        realtime = showRealtimeIcon,
                        lastTrip = state.trip.last,
                        alpha = maxTextAlpha,
                    ) {
                        TightWrapText(
                            state.trip.text,
                            modifier =
                                Modifier.semantics { contentDescription = tripDescription }
                                    .alignByBaseline()
                                    .placeholderIfLoading()
                                    .alpha(dimmedAlpha),
                            style = Typography.footnoteSemibold.merge(textAlign = TextAlign.End),
                        )
                    }

                is TripInstantDisplay.Hidden -> {}
                is TripInstantDisplay.Skipped -> {}
                is TripInstantDisplay.Boarding ->
                    WithStatusIndicators(
                        modifier,
                        realtime = showRealtimeIcon,
                        lastTrip = state.trip.last,
                        alpha = maxTextAlpha,
                    ) {
                        Text(
                            stringResource(R.string.boarding_abbr),
                            Modifier.semantics { contentDescription = tripDescription }
                                .alignByBaseline()
                                .placeholderIfLoading(),
                            textAlign = TextAlign.End,
                            style = Typography.headlineBold,
                        )
                    }

                is TripInstantDisplay.Arriving ->
                    WithStatusIndicators(
                        modifier,
                        realtime = showRealtimeIcon,
                        lastTrip = state.trip.last,
                        alpha = maxTextAlpha,
                    ) {
                        Text(
                            stringResource(R.string.arriving_abbr),
                            Modifier.semantics { contentDescription = tripDescription }
                                .alignByBaseline()
                                .placeholderIfLoading(),
                            textAlign = TextAlign.End,
                            style = Typography.headlineBold,
                        )
                    }

                is TripInstantDisplay.Now ->
                    WithStatusIndicators(
                        modifier,
                        realtime = showRealtimeIcon,
                        lastTrip = state.trip.last,
                        alpha = maxTextAlpha,
                    ) {
                        Text(
                            stringResource(R.string.now),
                            Modifier.semantics { contentDescription = tripDescription }
                                .alignByBaseline()
                                .placeholderIfLoading(),
                            textAlign = TextAlign.End,
                            style = Typography.headlineBold,
                        )
                    }

                is TripInstantDisplay.Approaching ->
                    WithStatusIndicators(
                        modifier,
                        realtime = showRealtimeIcon,
                        lastTrip = state.trip.last,
                        alpha = maxTextAlpha,
                    ) {
                        Text(
                            text =
                                AnnotatedString.fromHtml(stringResource(R.string.minutes_abbr, 1)),
                            modifier =
                                Modifier.semantics { contentDescription = tripDescription }
                                    .alignByBaseline()
                                    .placeholderIfLoading(),
                        )
                    }

                is TripInstantDisplay.Time ->
                    WithStatusIndicators(
                        modifier,
                        realtime = showRealtimeIcon,
                        lastTrip = state.trip.last,
                        alpha = maxTextAlpha,
                    ) {
                        Text(
                            state.trip.predictionTime.formattedTime(),
                            Modifier.semantics { contentDescription = tripDescription }
                                .alignByBaseline()
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
                        WithStatusIndicators(
                            modifier,
                            realtime = showRealtimeIcon,
                            lastTrip = state.trip.last,
                            alpha = maxTextAlpha,
                        ) {
                            Text(
                                state.trip.predictionTime.formattedTime(),
                                Modifier.alignByBaseline().placeholderIfLoading(),
                                textAlign = TextAlign.End,
                                style =
                                    if (state.trip.headline) Typography.headlineSemibold
                                    else Typography.footnoteSemibold,
                            )
                        }
                        Text(
                            state.trip.status,
                            color = LocalContentColor.current.copy(alpha = dimmedAlpha),
                            textAlign = TextAlign.End,
                            style = Typography.footnoteSemibold,
                        )
                    }

                is TripInstantDisplay.TimeWithSchedule ->
                    Column(
                        modifier.clearAndSetSemantics { contentDescription = tripDescription },
                        horizontalAlignment = Alignment.End,
                    ) {
                        WithStatusIndicators(
                            modifier,
                            realtime = showRealtimeIcon,
                            lastTrip = state.trip.last,
                            alpha = maxTextAlpha,
                        ) {
                            Text(
                                state.trip.predictionTime.formattedTime(),
                                Modifier.alignByBaseline().placeholderIfLoading(),
                                textAlign = TextAlign.End,
                                style =
                                    if (state.trip.headline) Typography.headlineSemibold
                                    else Typography.footnoteSemibold,
                            )
                        }
                        Text(
                            state.trip.scheduledTime.formattedTime(),
                            color = LocalContentColor.current.copy(alpha = dimmedAlpha),
                            textAlign = TextAlign.End,
                            textDecoration = TextDecoration.LineThrough,
                            style = Typography.footnoteSemibold,
                        )
                    }

                is TripInstantDisplay.ScheduleTime ->
                    WithStatusIndicators(
                        modifier,
                        lastTrip = state.trip.last,
                        scheduleClock = true,
                        alpha = dimmedAlpha,
                    ) {
                        Text(
                            state.trip.scheduledTime.formattedTime(),
                            modifier =
                                Modifier.semantics { contentDescription = tripDescription }
                                    .alignByBaseline()
                                    .placeholderIfLoading(),
                            textAlign = TextAlign.End,
                            style =
                                if (state.trip.headline) Typography.headlineSemibold
                                else Typography.footnoteSemibold,
                        )
                    }

                is TripInstantDisplay.ScheduleTimeWithStatusColumn ->
                    Column(
                        modifier.clearAndSetSemantics { contentDescription = tripDescription },
                        horizontalAlignment = Alignment.End,
                    ) {
                        WithStatusIndicators(
                            modifier,
                            lastTrip = state.trip.last,
                            scheduleClock = true,
                            alpha = dimmedAlpha,
                        ) {
                            Text(
                                state.trip.scheduledTime.formattedTime(),
                                modifier = Modifier.alignByBaseline().placeholderIfLoading(),
                                textAlign = TextAlign.End,
                                style =
                                    if (state.trip.headline) Typography.headlineSemibold
                                    else Typography.footnoteSemibold,
                            )
                        }
                        Text(
                            state.trip.status,
                            color = LocalContentColor.current.copy(alpha = dimmedAlpha),
                            textAlign = TextAlign.End,
                            style = Typography.footnoteSemibold,
                        )
                    }

                is TripInstantDisplay.ScheduleTimeWithStatusRow ->
                    Row(
                        modifier.clearAndSetSemantics { contentDescription = tripDescription },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                    ) {
                        Text(
                            state.trip.status,
                            color = LocalContentColor.current.copy(alpha = dimmedAlpha),
                            textAlign = TextAlign.End,
                            style = Typography.footnote,
                        )
                        Text(
                            state.trip.scheduledTime.formattedTime(),
                            modifier =
                                Modifier.alpha(dimmedAlpha)
                                    .semantics { contentDescription = tripDescription }
                                    .placeholderIfLoading(),
                            textAlign = TextAlign.End,
                            style = Typography.footnoteSemibold,
                        )
                    }
                is TripInstantDisplay.Minutes ->
                    WithStatusIndicators(
                        modifier,
                        realtime = showRealtimeIcon,
                        lastTrip = state.trip.last,
                        alpha = maxTextAlpha,
                    ) {
                        Text(
                            text =
                                AnnotatedString.fromHtml(predictionTextMinutes(state.trip.minutes)),
                            modifier =
                                Modifier.semantics { contentDescription = tripDescription }
                                    .alignByBaseline()
                                    .placeholderIfLoading(),
                        )
                    }

                is TripInstantDisplay.ScheduleMinutes ->
                    WithStatusIndicators(
                        modifier,
                        lastTrip = state.trip.last,
                        scheduleClock = true,
                        alpha = dimmedAlpha,
                    ) {
                        Text(
                            text =
                                AnnotatedString.fromHtml(predictionTextMinutes(state.trip.minutes)),
                            modifier =
                                Modifier.semantics { contentDescription = tripDescription }
                                    .alignByBaseline()
                                    .placeholderIfLoading(),
                            textAlign = TextAlign.End,
                        )
                    }

                is TripInstantDisplay.Cancelled ->
                    Row(
                        modifier
                            .alpha(dimmedAlpha)
                            .semantics { contentDescription = tripDescription }
                            .placeholderIfLoading(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                    ) {
                        Text(
                            stringResource(R.string.cancelled),
                            color = colorResource(R.color.deemphasized),
                            textAlign = TextAlign.End,
                            style = Typography.footnote,
                        )
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
                is UpcomingFormat.NoTripsFormat.SubwayEarlyMorning ->
                    Row(
                        modifier.alpha(dimmedAlpha),
                        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painterResource(R.drawable.fa_clock),
                            null,
                            Modifier.padding(4.dp).size(12.dp),
                        )
                        Text(
                            AnnotatedString.fromHtml(
                                stringResource(
                                    R.string.subway_early_am_first_time,
                                    state.format.scheduledTime.formattedTime(),
                                )
                            ),
                            textAlign = TextAlign.End,
                            style = Typography.footnote,
                        )
                    }
                is UpcomingFormat.NoTripsFormat.PredictionsUnavailable ->
                    Text(
                        stringResource(R.string.no_predictions),
                        modifier.alpha(maxTextAlpha),
                        textAlign = TextAlign.End,
                        style = Typography.footnote,
                    )
                is UpcomingFormat.NoTripsFormat.ServiceEndedToday ->
                    Text(
                        stringResource(R.string.service_ended),
                        modifier.alpha(maxTextAlpha),
                        textAlign = TextAlign.End,
                        style = Typography.footnote,
                    )
                is UpcomingFormat.NoTripsFormat.NoSchedulesToday ->
                    Text(
                        stringResource(R.string.no_service_today),
                        modifier.alpha(maxTextAlpha),
                        textAlign = TextAlign.End,
                        style = Typography.footnote,
                    )
            }
        is UpcomingTripViewState.Loading ->
            CompositionLocalProvider(IsLoadingSheetContents provides true) {
                UpcomingTripView(
                    UpcomingTripViewState.Some(TripInstantDisplay.Minutes(10, false)),
                    modifier.loadingShimmer().placeholderIfLoading(),
                    routeType,
                    isFirst,
                    isOnly,
                    hideRealtimeIndicators,
                    maxTextAlpha = maxTextAlpha,
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
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
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
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Now(false)))
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Minutes(5, false)))
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.ScheduleTimeWithStatusRow(
                        EasternTimeInstant.now() + 10.minutes,
                        "All aboard",
                    )
                )
            )
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.ScheduleTime(
                        EasternTimeInstant.now() + 10.minutes,
                        last = false,
                        headline = true,
                    )
                )
            )
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.ScheduleTime(EasternTimeInstant.now() + 10.minutes, false)
                )
            )
            UpcomingTripView(
                UpcomingTripViewState.NoTrips(
                    UpcomingFormat.NoTripsFormat.SubwayEarlyMorning(
                        EasternTimeInstant.now() + 10.minutes
                    )
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

@Preview
@Composable
fun LastTripsPreview() {
    MyApplicationTheme {
        Column(
            Modifier.background(colorResource(R.color.fill3)).widthIn(max = 150.dp).padding(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Now(true)))
            UpcomingTripView(UpcomingTripViewState.Some(TripInstantDisplay.Minutes(5, true)))
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.TimeWithStatus(
                        EasternTimeInstant.now() + 7.minutes,
                        "Now boarding",
                        true,
                    )
                )
            )
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.TimeWithSchedule(
                        EasternTimeInstant.now() + 7.minutes,
                        EasternTimeInstant.now() + 10.minutes,
                        true,
                    )
                )
            )
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.Overridden("Stopped 10 stops away", true)
                )
            )
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.ScheduleTime(
                        EasternTimeInstant.now() + 10.minutes,
                        last = true,
                        headline = true,
                    )
                )
            )
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.ScheduleTime(EasternTimeInstant.now() + 10.minutes, true)
                )
            )
            UpcomingTripView(
                UpcomingTripViewState.Some(
                    TripInstantDisplay.ScheduleTimeWithStatusColumn(
                        EasternTimeInstant.now() + 10.minutes,
                        "All aboard",
                        true,
                    )
                )
            )
            UpcomingTripView(
                UpcomingTripViewState.Some(TripInstantDisplay.ScheduleMinutes(18, true))
            )
        }
    }
}
