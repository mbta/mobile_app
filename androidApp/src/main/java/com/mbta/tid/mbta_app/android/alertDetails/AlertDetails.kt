package com.mbta.tid.mbta_app.android.alertDetails

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.icu.text.DateFormat
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.FormattedAlert
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.toJavaDate
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.Stop
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.compose.koinInject

@Composable
fun AlertDetails(
    alert: Alert,
    line: Line?,
    routes: List<Route>?,
    stop: Stop?,
    affectedStops: List<Stop>,
    now: Instant,
    analytics: Analytics = koinInject()
) {
    val routeId = line?.id ?: routes?.firstOrNull()?.id ?: ""
    val routeLabel = line?.longName ?: routes?.firstOrNull()?.label
    val stopLabel = stop?.name
    val effectLabel = FormattedAlert(alert).effect
    val causeLabel =
        when (alert.cause) {
            Alert.Cause.Accident -> stringResource(R.string.accident)
            Alert.Cause.Amtrak -> stringResource(R.string.amtrak)
            Alert.Cause.AnEarlierMechanicalProblem ->
                stringResource(R.string.an_earlier_mechanical_problem)
            Alert.Cause.AnEarlierSignalProblem -> stringResource(R.string.an_earlier_signal_problem)
            Alert.Cause.AutosImpedingService -> stringResource(R.string.autos_impeding_service)
            Alert.Cause.CoastGuardRestriction -> stringResource(R.string.coast_guard_restriction)
            Alert.Cause.Congestion -> stringResource(R.string.congestion)
            Alert.Cause.Construction -> stringResource(R.string.construction)
            Alert.Cause.CrossingMalfunction -> stringResource(R.string.crossing_malfunction)
            Alert.Cause.Demonstration -> stringResource(R.string.demonstration)
            Alert.Cause.DisabledBus -> stringResource(R.string.disabled_bus)
            Alert.Cause.DisabledTrain -> stringResource(R.string.disabled_train)
            Alert.Cause.DrawbridgeBeingRaised -> stringResource(R.string.drawbridge_being_raised)
            Alert.Cause.ElectricalWork -> stringResource(R.string.electrical_work)
            Alert.Cause.Fire -> stringResource(R.string.fire)
            Alert.Cause.FireDepartmentActivity -> stringResource(R.string.fire_department_activity)
            Alert.Cause.Flooding -> stringResource(R.string.flooding)
            Alert.Cause.Fog -> stringResource(R.string.fog)
            Alert.Cause.FreightTrainInterference ->
                stringResource(R.string.freight_train_interference)
            Alert.Cause.HazmatCondition -> stringResource(R.string.hazmat_condition)
            Alert.Cause.HeavyRidership -> stringResource(R.string.heavy_ridership)
            Alert.Cause.HighWinds -> stringResource(R.string.high_winds)
            Alert.Cause.Holiday -> stringResource(R.string.holiday)
            Alert.Cause.Hurricane -> stringResource(R.string.hurricane)
            Alert.Cause.IceInHarbor -> stringResource(R.string.ice_in_harbor)
            Alert.Cause.Maintenance -> stringResource(R.string.maintenance)
            Alert.Cause.MechanicalProblem -> stringResource(R.string.mechanical_problem)
            Alert.Cause.MedicalEmergency -> stringResource(R.string.medical_emergency)
            Alert.Cause.Parade -> stringResource(R.string.parade)
            Alert.Cause.PoliceAction -> stringResource(R.string.police_action)
            Alert.Cause.PoliceActivity -> stringResource(R.string.police_activity)
            Alert.Cause.PowerProblem -> stringResource(R.string.power_problem)
            Alert.Cause.SevereWeather -> stringResource(R.string.severe_weather)
            Alert.Cause.SignalProblem -> stringResource(R.string.signal_problem)
            Alert.Cause.SlipperyRail -> stringResource(R.string.slippery_rail)
            Alert.Cause.Snow -> stringResource(R.string.snow)
            Alert.Cause.SpecialEvent -> stringResource(R.string.special_event)
            Alert.Cause.SpeedRestriction -> stringResource(R.string.speed_restriction)
            Alert.Cause.Strike -> stringResource(R.string.strike)
            Alert.Cause.SwitchProblem -> stringResource(R.string.switch_problem)
            Alert.Cause.TechnicalProblem -> stringResource(R.string.technical_problem)
            Alert.Cause.TieReplacement -> stringResource(R.string.tie_replacement)
            Alert.Cause.TrackProblem -> stringResource(R.string.track_problem)
            Alert.Cause.TrackWork -> stringResource(R.string.track_work)
            Alert.Cause.Traffic -> stringResource(R.string.traffic)
            Alert.Cause.UnrulyPassenger -> stringResource(R.string.unruly_passenger)
            Alert.Cause.Weather -> stringResource(R.string.weather)
            else -> null
        }
    val currentPeriod = alert.activePeriod.firstOrNull { it.activeAt(now) }
    val isElevatorAlert = alert.effect == Alert.Effect.ElevatorClosure
    val elevatorSubtitle = if (isElevatorAlert) alert.header else null

    Column(
        Modifier.verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        AlertTitle(
            routeLabel,
            stopLabel,
            effectLabel,
            causeLabel,
            elevatorSubtitle,
            isElevatorAlert
        )
        if (!isElevatorAlert) {
            AlertPeriod(currentPeriod)

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AffectedStopCollapsible(
                    affectedStops,
                    onTappedAffectedStops = {
                        analytics.tappedAffectedStops(routeId, stop?.id ?: "", alert.id)
                    }
                )
                TripPlannerLink(
                    onTappedTripPlanner = {
                        analytics.tappedTripPlanner(routeId, stop?.id ?: "", alert.id)
                    }
                )
            }
        }
        AlertDescription(alert, affectedStopsKnown = affectedStops.isNotEmpty())
        if (isElevatorAlert) {
            AlertPeriod(currentPeriod)
        }
        AlertFooter(alert.updatedAt)
    }
}

@Composable
private fun AlertTitle(
    routeLabel: String?,
    stopLabel: String?,
    effectLabel: String?,
    causeLabel: String?,
    elevatorSubtitle: String?,
    isElevatorAlert: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (effectLabel != null) {
            if (routeLabel != null) {
                Text(
                    stringResource(R.string.route_effect, routeLabel, effectLabel),
                    style = Typography.title2Bold
                )
            } else if (stopLabel != null) {
                Text(
                    stringResource(R.string.route_effect, stopLabel, effectLabel),
                    style = Typography.title2Bold
                )
            } else {
                Text(effectLabel, style = Typography.title2Bold)
            }
        }
        if (!isElevatorAlert && causeLabel != null) {
            Text(causeLabel, style = Typography.bodySemibold)
        } else if (isElevatorAlert && elevatorSubtitle != null) {
            Text(elevatorSubtitle, style = Typography.bodySemibold)
        }
    }
}

@Composable
private fun AlertPeriod(currentPeriod: Alert.ActivePeriod?) {
    if (currentPeriod != null) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier.widthIn(min = 48.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(stringResource(R.string.start), style = Typography.callout)
                Text(stringResource(R.string.end), style = Typography.callout)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(currentPeriod.formatStart(), style = Typography.callout)
                Text(currentPeriod.formatEnd(), style = Typography.callout)
            }
        }
    } else {
        Text(stringResource(R.string.alert_ended))
    }
}

@Composable
private fun AffectedStopCollapsible(affectedStops: List<Stop>, onTappedAffectedStops: () -> Unit) {
    var areStopsExpanded by remember { mutableStateOf(false) }
    val affectedStopsLabel =
        AnnotatedString.fromHtml(
            pluralStringResource(R.plurals.affected_stops, affectedStops.size, affectedStops.size)
        )
    if (affectedStops.isNotEmpty()) {
        Column(Modifier.tile()) {
            Row(
                Modifier.clickable(onClickLabel = null) {
                        areStopsExpanded = !areStopsExpanded
                        if (areStopsExpanded) {
                            onTappedAffectedStops()
                        }
                    }
                    .padding(16.dp)
            ) {
                Text(
                    affectedStopsLabel,
                    color = colorResource(R.color.text),
                    textAlign = TextAlign.Start,
                    style = Typography.callout
                )
                Spacer(Modifier.weight(1f))
                val degrees by
                    animateFloatAsState(if (areStopsExpanded) 90f else 0f, label = "rotation")
                Icon(
                    painterResource(R.drawable.fa_chevron_right),
                    null,
                    Modifier.rotate(degrees).size(16.dp),
                    colorResource(R.color.text)
                )
            }
            AnimatedVisibility(
                areStopsExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    for (stop in affectedStops) {
                        HorizontalDivider(color = colorResource(R.color.halo))
                        Text(
                            stop.name,
                            modifier = Modifier.padding(16.dp),
                            style = Typography.bodySemibold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TripPlannerLink(onTappedTripPlanner: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier.clickable {
                onTappedTripPlanner()
                val intent =
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.mbta.com/trip-planner"))
                try {
                    context.startActivity(intent)
                } catch (ex: ActivityNotFoundException) {
                    // ignore the error if the user has no web browser installed
                }
            }
            .tile()
            .padding(16.dp)
    ) {
        Text(
            "Plan a route with Trip Planner",
            color = colorResource(R.color.text),
            textAlign = TextAlign.Start,
            style = Typography.callout
        )
        Spacer(Modifier.weight(1f))
        Icon(
            painterResource(R.drawable.fa_route),
            null,
            Modifier.size(16.dp),
            colorResource(R.color.text)
        )
    }
}

@Composable
private fun Modifier.tile() =
    this.background(colorResource(R.color.fill3), RoundedCornerShape(8.dp))
        .border(1.dp, colorResource(R.color.halo), RoundedCornerShape(8.dp))

private fun splitDetails(details: String, separator: String? = null): List<String> =
    details.split(separator ?: "\n").filter { it.isNotEmpty() }.map { it.trim() }

@Composable
private fun AlertDescription(alert: Alert, affectedStopsKnown: Boolean) {
    val isElevatorAlert = alert.effect == Alert.Effect.ElevatorClosure
    val alertDescriptionParagraphs = buildList {
        val header = alert.header
        if (!isElevatorAlert && header != null) {
            addAll(splitDetails(header))
        }
        val description = alert.description
        if (description != null) {
            addAll(
                splitDetails(description, "\n\n").filterNot {
                    affectedStopsKnown && it.startsWith("Affected stops:")
                }
            )
        }
    }
    if (alertDescriptionParagraphs.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                if (!isElevatorAlert) stringResource(R.string.full_description)
                else stringResource(R.string.alternative_path),
                style = Typography.bodySemibold
            )
            for (section in alertDescriptionParagraphs) {
                Text(section, style = Typography.callout)
            }
        }
    }
}

@Composable
private fun AlertFooter(updatedAt: Instant) {
    val formattedDate =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(updatedAt.toJavaDate())
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HorizontalDivider(color = colorResource(R.color.halo))
        Text(
            stringResource(R.string.updated_at, formattedDate),
            color = colorResource(R.color.deemphasized),
            style = Typography.callout
        )
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL,
    backgroundColor = 0xFFFFFFFF
)
@Composable
private fun AlertDetailsPreview() {
    MyApplicationTheme {
        val objects = ObjectCollectionBuilder()
        val route =
            objects.route {
                color = "ED8B00"
                textColor = "FFFFFF"
                longName = "Orange Line"
            }
        val now = Clock.System.now()
        val stop = objects.stop { name = "Here @ There" }
        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                cause = Alert.Cause.Maintenance
                activePeriod(now - 3.days, now + 3.days)
                description =
                    "Orange Line service between Ruggles and Jackson Square will be suspended from Thursday, May 23 through Friday, May 31.\n\nAn accessible van will be available for riders. Please see Station Personnel or Transit Ambassadors for assistance.\n\nThe Haverhill Commuter Rail Line will be Fare Free between Oak Grove, Malden Center, and North Station during this work."
                updatedAt = now - 10.minutes
            }
        Column(Modifier.background(colorResource(R.color.fill2)).padding(16.dp)) {
            AlertDetails(alert, null, listOf(route), stop, listOf(stop), now, MockAnalytics())
        }
    }
}
