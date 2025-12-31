package com.mbta.tid.mbta_app.android.alertDetails

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.FormattedAlert
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.formattedShortDateShortTime
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import org.koin.compose.koinInject

@Composable
fun AlertDetails(
    alert: Alert,
    line: Line?,
    routes: List<Route>?,
    stop: Stop?,
    affectedStops: List<Stop>,
    now: EasternTimeInstant,
    analytics: Analytics = koinInject(),
) {
    val routeId = line?.id ?: routes?.firstOrNull()?.id
    val routeLabel = line?.longName ?: routes?.firstOrNull()?.label
    val stopLabel = stop?.name
    val formattedAlert = FormattedAlert(alert)
    val currentPeriod = alert.currentPeriod(now)
    val isElevatorAlert = alert.effect == Alert.Effect.ElevatorClosure
    val elevatorSubtitle = if (isElevatorAlert) alert.header else null

    Column(
        Modifier.verticalScroll(rememberScrollState())
            .background(colorResource(R.color.fill2))
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp)
            .padding(bottom = 8.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        AlertTitle(routeLabel, stopLabel, formattedAlert, elevatorSubtitle, isElevatorAlert)
        if (!isElevatorAlert) {
            AlertPeriod(currentPeriod)

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AffectedStopCollapsible(
                    affectedStops,
                    onTappedAffectedStops = {
                        analytics.tappedAffectedStops(routeId, stop?.id ?: "", alert.id)
                    },
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
    formattedAlert: FormattedAlert,
    elevatorSubtitle: String?,
    isElevatorAlert: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val effectLabel = AnnotatedString.fromHtml(formattedAlert.effect)
        val causeLabel = formattedAlert.cause

        if (routeLabel != null) {
            Text(
                stringResource(R.string.route_effect, routeLabel, effectLabel),
                style = Typography.title2Bold,
                modifier = Modifier.semantics { heading() },
            )
        } else if (stopLabel != null) {
            Text(
                stringResource(R.string.route_effect, stopLabel, effectLabel),
                style = Typography.title2Bold,
                modifier = Modifier.semantics { heading() },
            )
        } else {
            Text(
                effectLabel,
                style = Typography.title2Bold,
                modifier = Modifier.semantics { heading() },
            )
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
    var startWidth by remember { mutableStateOf<Int?>(null) }
    var endWidth by remember { mutableStateOf<Int?>(null) }

    // This is all because Jetpack Compose has no built in table layout, we need the "Start" and
    // "End" label texts to take up the same column width, but they can be variable widths in
    // different languages, so we can't use a fixed size, but we also need the label and formatted
    // period to be in rows together in case the period needs to break onto multiple lines.
    val density = LocalDensity.current
    val style = Typography.callout
    val textMeasurer = rememberTextMeasurer()
    val columnTexts = listOf(stringResource(R.string.start), stringResource(R.string.end))

    val columnWidth = remember {
        columnTexts.maxOf { text ->
            with(density) {
                textMeasurer.measure(text = AnnotatedString(text), style = style).size.width.toDp()
            }
        }
    }

    if (currentPeriod != null) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    columnTexts[0],
                    Modifier.width(columnWidth),
                    style = style,
                    onTextLayout = { startWidth = it.size.width },
                )
                Text(currentPeriod.formatStart(LocalResources.current), style = style)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    columnTexts[1],
                    Modifier.width(columnWidth),
                    style = style,
                    onTextLayout = { endWidth = it.size.width },
                )
                Text(currentPeriod.formatEnd(LocalResources.current), style = style)
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
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    affectedStopsLabel,
                    Modifier.weight(1f),
                    color = colorResource(R.color.text),
                    textAlign = TextAlign.Start,
                    style = Typography.callout,
                )
                val degrees by
                    animateFloatAsState(if (areStopsExpanded) 90f else 0f, label = "rotation")
                Icon(
                    painterResource(R.drawable.fa_chevron_right),
                    null,
                    Modifier.rotate(degrees).size(16.dp),
                    colorResource(R.color.text),
                )
            }
            AnimatedVisibility(
                areStopsExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    for (stop in affectedStops) {
                        HorizontalDivider(color = colorResource(R.color.halo))
                        Text(
                            stop.name,
                            modifier = Modifier.padding(16.dp),
                            style = Typography.bodySemibold,
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Plan a route with Trip Planner",
            Modifier.weight(1f),
            color = colorResource(R.color.text),
            textAlign = TextAlign.Start,
            style = Typography.callout,
        )
        Icon(
            painterResource(R.drawable.fa_route),
            null,
            Modifier.size(16.dp),
            colorResource(R.color.text),
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
                style = Typography.bodySemibold,
                modifier = Modifier.semantics { heading() },
            )
            for (section in alertDescriptionParagraphs) {
                Text(section, style = Typography.callout)
            }
        }
    }
}

@Composable
private fun AlertFooter(updatedAt: EasternTimeInstant) {
    val formattedDate = updatedAt.formattedShortDateShortTime()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HorizontalDivider(color = colorResource(R.color.halo))
        Text(
            stringResource(R.string.updated_at, formattedDate),
            color = colorResource(R.color.deemphasized),
            style = Typography.callout,
        )
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun AlertDetailsPreview() {
    MyApplicationTheme {
        val objects = ObjectCollectionBuilder("AlertDetailsPreview")
        val route =
            objects.route {
                color = "ED8B00"
                textColor = "FFFFFF"
                longName = "Orange Line"
            }
        val now = EasternTimeInstant.now()
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
