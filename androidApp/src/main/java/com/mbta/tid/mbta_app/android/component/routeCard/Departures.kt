package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.DirectionLabel
import com.mbta.tid.mbta_app.android.component.DirectionRowView
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.component.HeadsignRowView
import com.mbta.tid.mbta_app.android.component.NavDrilldownRow
import com.mbta.tid.mbta_app.android.component.PillDecoration
import com.mbta.tid.mbta_app.android.generated.drawableByName
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.model.LeafFormat
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.WheelchairBoardingStatus
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Duration.Companion.minutes
import org.koin.compose.koinInject

@Composable
fun Departures(
    stopData: RouteCardData.RouteStopData,
    globalData: GlobalResponse?,
    now: EasternTimeInstant,
    isFavorite: (RouteStopDirection) -> Boolean?,
    analytics: Analytics = koinInject(),
    onClick: (RouteCardData.Leaf) -> Unit,
) {
    val localContext = LocalContext.current

    Column {
        stopData.data.withIndex().forEach { (index, leaf) ->
            fun analyticsTappedDeparture(leafFormat: LeafFormat) {
                val format = (leafFormat as? LeafFormat.Single)?.format
                val noTrips = (format as? UpcomingFormat.NoTrips)?.noTripsFormat
                analytics.tappedDeparture(
                    stopData.lineOrRoute.id,
                    stopData.stop.id,
                    isFavorite(
                        RouteStopDirection(
                            stopData.lineOrRoute.id,
                            stopData.stop.id,
                            leaf.directionId,
                        )
                    ) ?: false,
                    leaf.alertsHere().isNotEmpty(),
                    stopData.lineOrRoute.type,
                    noTrips,
                )
            }

            val formatted = leaf.format(now, globalData)
            val direction = stopData.directions.first { it.id == leaf.directionId }

            NavDrilldownRow(
                onClick = {
                    onClick(leaf)
                    analyticsTappedDeparture(formatted)
                },
                onClickLabel = localContext.getString(R.string.open_for_more_arrivals),
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
            ) { modifier ->
                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                ) {
                    when (formatted) {
                        is LeafFormat.Single -> {
                            DirectionRowView(
                                direction.copy(
                                    destination = formatted.headsign ?: direction.destination
                                ),
                                formatted.format,
                                pillDecoration =
                                    formatted.route?.let {
                                        PillDecoration.OnDirectionDestination(it)
                                    },
                            )
                        }
                        is LeafFormat.Branched -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                formatted.secondaryAlert?.let { secondaryAlert ->
                                    Image(
                                        painterResource(drawableByName(secondaryAlert.iconName)),
                                        stringResource(R.string.alert),
                                        modifier =
                                            Modifier.placeholderIfLoading().padding(end = 8.dp),
                                    )
                                }
                                DirectionLabel(direction, showDestination = false)
                            }
                            for (branch in formatted.branchRows) {
                                HeadsignRowView(
                                    branch.headsign,
                                    branch.format,
                                    pillDecoration = branch.route?.let { PillDecoration.OnRow(it) },
                                )
                            }
                        }
                    }
                }
            }

            if (index < stopData.data.lastIndex) {
                HaloSeparator()
            }
        }
    }
}

@Preview
@Composable
private fun DeparturesPreview() {
    val now = EasternTimeInstant.now()
    val objects = ObjectCollectionBuilder()
    val redLine =
        objects.route {
            id = "Red"
            color = "DA291C"
            directionDestinations = listOf("Ashmont/Braintree", "Alewife")
            directionNames = listOf("South", "North")
            longName = "Red Line"
            textColor = "FFFFFF"
            type = RouteType.HEAVY_RAIL
        }
    val redLineAshmontSouthbound =
        objects.routePattern(redLine) {
            directionId = 0
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Ashmont" }
        }
    val redLineBraintreeSouthbound =
        objects.routePattern(redLine) {
            directionId = 0
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Braintree" }
        }
    val redLineAshmontNorthbound =
        objects.routePattern(redLine) {
            directionId = 1
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Alewife" }
        }
    val redLineBraintreeNorthbound =
        objects.routePattern(redLine) {
            directionId = 1
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Alewife" }
        }
    val jfkUmass =
        objects.stop {
            name = "JFK/UMass"
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
        }
    val global = GlobalResponse(objects)
    val context = RouteCardData.Context.NearbyTransit

    val lineOrRoute = RouteCardData.LineOrRoute.Route(redLine)
    val stopData =
        RouteCardData.RouteStopData(
            lineOrRoute,
            jfkUmass,
            listOf(
                RouteCardData.Leaf(
                    lineOrRoute,
                    jfkUmass,
                    0,
                    listOf(redLineAshmontSouthbound, redLineBraintreeSouthbound),
                    setOf(jfkUmass.id),
                    listOf(
                        objects.upcomingTrip(
                            objects.prediction {
                                trip = objects.trip(redLineAshmontSouthbound)
                                departureTime = now + 1.minutes
                            }
                        ),
                        objects.upcomingTrip(
                            objects.prediction {
                                trip = objects.trip(redLineBraintreeSouthbound)
                                departureTime = now + 2.minutes
                            }
                        ),
                        objects.upcomingTrip(
                            objects.prediction {
                                trip = objects.trip(redLineAshmontSouthbound)
                                departureTime = now + 9.minutes
                            }
                        ),
                    ),
                    emptyList(),
                    true,
                    true,
                    emptyList(),
                    context,
                ),
                RouteCardData.Leaf(
                    lineOrRoute,
                    jfkUmass,
                    1,
                    listOf(redLineAshmontNorthbound, redLineBraintreeNorthbound),
                    setOf(jfkUmass.id),
                    listOf(
                        objects.upcomingTrip(
                            objects.prediction {
                                trip = objects.trip(redLineAshmontNorthbound)
                                departureTime = now + 3.minutes
                            }
                        ),
                        objects.upcomingTrip(
                            objects.prediction {
                                trip = objects.trip(redLineBraintreeNorthbound)
                                departureTime = now + 12.minutes
                            }
                        ),
                    ),
                    emptyList(),
                    true,
                    true,
                    emptyList(),
                    context,
                ),
            ),
            global,
        )

    MyApplicationTheme {
        Column(Modifier.background(MaterialTheme.colorScheme.background)) {
            Departures(stopData, global, now, { _ -> false }, MockAnalytics(), onClick = {})
        }
    }
}
