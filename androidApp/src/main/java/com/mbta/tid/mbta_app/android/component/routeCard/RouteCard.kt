package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.component.PinButton
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.WheelchairBoardingStatus
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.compose.KoinContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@Composable
fun RouteCard(
    data: RouteCardData,
    globalData: GlobalResponse?,
    now: Instant,
    pinned: Boolean,
    onPin: (String) -> Unit,
    showElevatorAccessibility: Boolean = false,
    onOpenStopDetails: (String, StopDetailsFilter) -> Unit
) {
    Column(Modifier.haloContainer(1.dp)) {
        TransitHeader(data.lineOrRoute) { color ->
            PinButton(pinned = pinned, color = color) { onPin(data.lineOrRoute.id) }
        }

        data.stopData.forEach {
            if (data.context == RouteCardData.Context.NearbyTransit) {
                StopHeader(it, showElevatorAccessibility)
            }

            Departures(it, data, globalData, now, pinned) { leaf ->
                onOpenStopDetails(
                    it.stop.id,
                    StopDetailsFilter(data.lineOrRoute.id, leaf.directionId)
                )
            }
        }
    }
}

class Previews() {
    val now = Clock.System.now()
    val objects = ObjectCollectionBuilder()
    val greenLine =
        objects.line {
            color = "00843D"
            longName = "Green Line"
            textColor = "FFFFFF"
        }
    val orangeLine =
        objects.route {
            id = "Orange"
            color = "ED8B00"
            directionDestinations = listOf("Forest Hills", "Oak Grove")
            directionNames = listOf("South", "North")
            longName = "Orange Line"
            textColor = "FFFFFF"
            type = RouteType.HEAVY_RAIL
        }
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
    val greenLineB =
        objects.route {
            id = "Green-B"
            color = greenLine.color
            directionDestinations = listOf("Boston College", "Government Center")
            directionNames = listOf("West", "East")
            lineId = greenLine.id
            longName = "Green Line B"
            shortName = "B"
            textColor = greenLine.textColor
            type = RouteType.LIGHT_RAIL
        }
    val greenLineC =
        objects.route {
            id = "Green-C"
            color = greenLine.color
            directionDestinations = listOf("Cleveland Circle", "Government Center")
            directionNames = listOf("West", "East")
            lineId = greenLine.id
            longName = "Green Line C"
            shortName = "C"
            textColor = greenLine.textColor
            type = RouteType.LIGHT_RAIL
        }
    val greenLineD =
        objects.route {
            id = "Green-D"
            color = greenLine.color
            directionDestinations = listOf("Riverside", "Union Square")
            directionNames = listOf("West", "East")
            lineId = greenLine.id
            longName = "Green Line D"
            shortName = "D"
            textColor = greenLine.textColor
            type = RouteType.LIGHT_RAIL
        }
    val greenLineE =
        objects.route {
            id = "Green-E"
            color = greenLine.color
            directionDestinations = listOf("Heath Street", "Medford/Tufts")
            directionNames = listOf("West", "East")
            lineId = greenLine.id
            longName = "Green Line E"
            shortName = "E"
            textColor = greenLine.textColor
            type = RouteType.LIGHT_RAIL
        }
    val orangeLineSouthbound =
        objects.routePattern(orangeLine) {
            directionId = 0
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Forest Hills" }
        }
    val orangeLineNorthbound =
        objects.routePattern(orangeLine) {
            directionId = 1
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Oak Grove" }
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
    val greenLineBWestbound =
        objects.routePattern(greenLineB) {
            directionId = 0
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Boston College" }
        }
    val greenLineBEastbound =
        objects.routePattern(greenLineB) {
            directionId = 1
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Government Center" }
        }
    val greenLineCWestbound =
        objects.routePattern(greenLineC) {
            directionId = 0
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Cleveland Circle" }
        }
    val greenLineCEastbound =
        objects.routePattern(greenLineC) {
            directionId = 1
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Government Center" }
        }
    val greenLineDWestbound =
        objects.routePattern(greenLineD) {
            directionId = 0
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Riverside" }
        }
    val greenLineDEastbound =
        objects.routePattern(greenLineD) {
            directionId = 1
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Union Square" }
        }
    val ruggles =
        objects.stop {
            name = "Ruggles"
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
        }
    val stonyBrook =
        objects.stop {
            name = "Stony Brook"
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
        }
    val jfkUmass =
        objects.stop {
            name = "JFK/UMass"
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
        }
    val boylston =
        objects.stop {
            name = "Boylston"
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE
        }
    val shuttleAlert = objects.alert { effect = Alert.Effect.Shuttle }
    val suspensionAlert = objects.alert { effect = Alert.Effect.Suspension }
    val global = GlobalResponse(objects)
    val context = RouteCardData.Context.NearbyTransit

    val koin = koinApplication { modules(module { single<Analytics> { MockAnalytics() } }) }

    fun card(
        lineOrRoute: RouteCardData.LineOrRoute,
        stop: Stop,
        patterns: List<RoutePattern>,
        trips: List<UpcomingTrip>,
        alertHere: Map<Int, Alert>,
        alertDownstream: Map<Int, Alert>
    ) =
        RouteCardData(
            lineOrRoute,
            listOf(
                RouteCardData.RouteStopData(
                    stop,
                    lineOrRoute,
                    listOf(
                        RouteCardData.Leaf(
                            0,
                            patterns.filter { it.directionId == 0 },
                            setOf(stop.id),
                            trips.filter { it.trip.directionId == 0 },
                            listOfNotNull(alertHere[0]),
                            true,
                            true,
                            listOfNotNull(alertDownstream[0])
                        ),
                        RouteCardData.Leaf(
                            1,
                            patterns.filter { it.directionId == 1 },
                            setOf(stop.id),
                            trips.filter { it.trip.directionId == 1 },
                            listOfNotNull(alertHere[1]),
                            true,
                            true,
                            listOfNotNull(alertDownstream[1])
                        )
                    ),
                    global
                )
            ),
            context,
            now
        )

    fun card(
        route: Route,
        stop: Stop,
        trips: List<UpcomingTrip>,
        alertHere: Map<Int, Alert> = emptyMap(),
        alertDownstream: Map<Int, Alert> = emptyMap()
    ) =
        card(
            RouteCardData.LineOrRoute.Route(route),
            stop,
            objects.routePatterns.values.filter { it.routeId == route.id },
            trips,
            alertHere,
            alertDownstream
        )

    fun card(
        line: Line,
        stop: Stop,
        trips: List<UpcomingTrip>,
        alertHere: Map<Int, Alert> = emptyMap(),
        alertDownstream: Map<Int, Alert> = emptyMap()
    ): RouteCardData {
        val routes = objects.routes.values.filter { it.lineId == line.id }.toSet()
        val routeIds = routes.map { it.id }
        val routePatterns = objects.routePatterns.values.filter { routeIds.contains(it.routeId) }
        return card(
            RouteCardData.LineOrRoute.Line(line, routes),
            stop,
            routePatterns,
            trips,
            alertHere,
            alertDownstream
        )
    }

    @Composable
    fun CardForPreview(card: RouteCardData) {
        KoinContext(koin.koin) { RouteCard(card, global, now, false, {}, true, { _, _ -> }) }
    }

    @Preview(group = "Orange Line disruption")
    @Composable
    fun DownstreamDisruption() {
        CardForPreview(
            card(
                orangeLine,
                ruggles,
                listOf(
                    objects.upcomingTrip(
                        objects.prediction {
                            trip =
                                objects.trip(orangeLineSouthbound) { headsign = "Jackson Square" }
                            departureTime = now + 5.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip =
                                objects.trip(orangeLineSouthbound) { headsign = "Jackson Square" }
                            departureTime = now + 16.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(orangeLineNorthbound)
                            departureTime = now + 7.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(orangeLineNorthbound)
                            departureTime = now + 12.minutes
                        }
                    )
                ),
                alertDownstream = mapOf(0 to shuttleAlert)
            )
        )
    }

    @Preview(group = "Orange Line disruption")
    @Composable
    fun DisruptedStop() {
        CardForPreview(
            card(orangeLine, stonyBrook, listOf(), mapOf(0 to shuttleAlert, 1 to shuttleAlert))
        )
    }

    @Preview(group = "Red Line branching")
    @Composable
    fun NextThreeTrips() {
        CardForPreview(
            card(
                redLine,
                jfkUmass,
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
                    )
                )
            )
        )
    }

    @Preview(group = "Red Line branching")
    @Composable
    fun DisruptedDownstream() {
        CardForPreview(
            card(
                redLine,
                jfkUmass,
                listOf(
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(redLineAshmontSouthbound)
                            departureTime = now + 1.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip =
                                objects.trip(redLineBraintreeSouthbound) { headsign = "Wollaston" }
                            departureTime = now + 2.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(redLineAshmontSouthbound)
                            departureTime = now + 9.minutes
                        }
                    ),
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
                    )
                ),
                alertDownstream = mapOf(0 to suspensionAlert)
            )
        )
    }

    @Preview(group = "Green Line branching")
    @Composable
    fun BranchingInBothDirections() {
        CardForPreview(
            card(
                greenLine,
                boylston,
                listOf(
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(greenLineCWestbound)
                            departureTime = now + 3.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(greenLineBWestbound)
                            departureTime = now + 5.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(greenLineDWestbound)
                            departureTime = now + 10.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(greenLineDEastbound)
                            departureTime = now + 1.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(greenLineBEastbound)
                            departureTime = now + 6.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(greenLineCEastbound)
                            departureTime = now + 12.minutes
                        }
                    ),
                )
            )
        )
    }

    @Preview(group = "Green Line branching")
    @Composable
    fun GLDownstreamDisruption() {
        CardForPreview(
            card(
                greenLine,
                boylston,
                listOf(
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(greenLineCWestbound)
                            departureTime = now + 3.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(greenLineBWestbound)
                            departureTime = now + 5.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(greenLineDWestbound)
                            departureTime = now + 10.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(greenLineDEastbound)
                            departureTime = now + 1.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(greenLineBEastbound)
                            departureTime = now + 6.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(greenLineCEastbound)
                            departureTime = now + 12.minutes
                        }
                    ),
                ),
                alertDownstream = mapOf(0 to suspensionAlert)
            )
        )
    }
}
