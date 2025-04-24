package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
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
    showStationAccessibility: Boolean = false,
    onOpenStopDetails: (String, StopDetailsFilter) -> Unit
) {
    Column(Modifier.haloContainer(1.dp)) {
        TransitHeader(data.lineOrRoute) { color ->
            PinButton(pinned = pinned, color = color) { onPin(data.lineOrRoute.id) }
        }

        data.stopData.forEach {
            if (data.context == RouteCardData.Context.NearbyTransit) {
                StopHeader(it, showStationAccessibility)
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
    fun LocalDateTime.toInstant(): Instant = toInstant(TimeZone.currentSystemDefault())

    val today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val now: Instant = today.atTime(11, 30).toInstant()
    val objects = ObjectCollectionBuilder()
    val greenLine =
        objects.line {
            id = "line-Green"
            color = "00843D"
            longName = "Green Line"
            textColor = "FFFFFF"
        }
    val slWaterfront =
        objects.line {
            color = "7C878E"
            longName = "Silver Line SL1/SL2/SL3"
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
    val sl1 =
        objects.route {
            color = slWaterfront.color
            directionNames = listOf("Outbound", "Inbound")
            lineId = slWaterfront.id
            shortName = "SL1"
            textColor = slWaterfront.textColor
            type = RouteType.BUS
        }
    val sl2 =
        objects.route {
            color = slWaterfront.color
            directionNames = listOf("Outbound", "Inbound")
            lineId = slWaterfront.id
            shortName = "SL2"
            textColor = slWaterfront.textColor
            type = RouteType.BUS
        }
    val sl3 =
        objects.route {
            color = slWaterfront.color
            directionNames = listOf("Outbound", "Inbound")
            lineId = slWaterfront.id
            shortName = "SL3"
            textColor = slWaterfront.textColor
            type = RouteType.BUS
        }
    val providenceLine =
        objects.route {
            color = "80276C"
            directionDestinations = listOf("Stoughton or Wickford Junction", "South Station")
            directionNames = listOf("Outbound", "Inbound")
            longName = "Providence/Stoughton Line"
            textColor = "FFFFFF"
            type = RouteType.COMMUTER_RAIL
        }
    val bus87 =
        objects.route {
            color = "FFC72C"
            directionDestinations = listOf("Clarendon Hill or Arlington Center", "Lechmere Station")
            directionNames = listOf("Outbound", "Inbound")
            shortName = "87"
            textColor = "000000"
            type = RouteType.BUS
        }
    val bus15 =
        objects.route {
            color = "FFC72C"
            directionDestinations =
                listOf("Fields Corner Station or St Peter's Square", "Ruggles Station")
            directionNames = listOf("Outbound", "Inbound")
            shortName = "15"
            textColor = "000000"
            type = RouteType.BUS
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
            representativeTrip {
                headsign = "Boston College"
                // only the stops required to correctly apply the direction special casing
                stopIds = listOf("place-boyls", "place-armnl", "place-kencl", "place-lake")
            }
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
            representativeTrip {
                headsign = "Cleveland Circle"
                // only the stops required to correctly apply the direction special casing
                stopIds = listOf("place-boyls", "place-armnl", "place-kencl", "place-clmnl")
            }
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
            representativeTrip {
                headsign = "Riverside"
                // only the stops required to correctly apply the direction special casing
                stopIds = listOf("place-boyls", "place-armnl", "place-kencl", "place-river")
            }
        }
    val greenLineDEastbound =
        objects.routePattern(greenLineD) {
            directionId = 1
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Union Square" }
        }
    val arlingtonOutbound =
        objects.routePattern(bus87) {
            directionId = 0
            representativeTrip { headsign = "Arlington Center" }
        }
    val arlingtonInbound =
        objects.routePattern(bus87) {
            directionId = 1
            representativeTrip { headsign = "Lechmere" }
        }
    val clarendonOutbound =
        objects.routePattern(bus87) {
            directionId = 0
            representativeTrip { headsign = "Clarendon Hill" }
        }
    val clarendonInbound =
        objects.routePattern(bus87) {
            directionId = 1
            representativeTrip { headsign = "Lechmere" }
        }
    val ruggles =
        objects.stop {
            name = "Ruggles"
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
        }
    val jfkUmass =
        objects.stop {
            name = "JFK/UMass"
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
        }
    val boylston =
        objects.stop {
            id = "place-boyls"
            name = "Boylston"
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE
        }

    val kenmore =
        objects.stop {
            id = "place-kencl"
            name = "Kenmore"
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
        }
    val somervilleAtCarlton =
        objects.stop {
            name = "Somerville Ave @ Carlton St"
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
        }
    val bowAtWarren =
        objects.stop {
            name = "Bow St @ Warren Ave"
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
        }
    val shuttleAlert = objects.alert { effect = Alert.Effect.Shuttle }
    val suspensionAlert = objects.alert { effect = Alert.Effect.Suspension }
    val global = GlobalResponse(objects)
    val context = RouteCardData.Context.NearbyTransit

    val koin = koinApplication { modules(module { single<Analytics> { MockAnalytics() } }) }

    fun cardStop(
        lineOrRoute: RouteCardData.LineOrRoute,
        stop: Stop,
        patterns: List<RoutePattern>,
        trips: List<UpcomingTrip>,
        alertHere: Map<Int, Alert>,
        alertDownstream: Map<Int, Alert>
    ) =
        RouteCardData.RouteStopData(
            stop,
            lineOrRoute,
            listOfNotNull(
                RouteCardData.Leaf(
                        0,
                        patterns.filter { it.directionId == 0 },
                        setOf(stop.id),
                        trips.filter { it.trip.directionId == 0 },
                        listOfNotNull(alertHere[0]),
                        true,
                        true,
                        listOfNotNull(alertDownstream[0])
                    )
                    .takeUnless { it.routePatterns.isEmpty() },
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
                    .takeUnless { it.routePatterns.isEmpty() }
            ),
            global
        )

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
            listOf(cardStop(lineOrRoute, stop, patterns, trips, alertHere, alertDownstream)),
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
        KoinContext(koin.koin) {
            Box(Modifier.width(358.dp)) {
                RouteCard(card, global, now, false, {}, true, { _, _ -> })
            }
        }
    }

    @Preview(name = "Downstream disruption", group = "1. Orange Line disruption")
    @Composable
    fun OL1() {
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

    @Preview(name = "Disrupted stop", group = "1. Orange Line disruption")
    @Composable
    fun OL2() {
        CardForPreview(
            card(
                orangeLine,
                objects.stop {
                    name = "Stony Brook"
                    wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
                },
                listOf(),
                mapOf(0 to shuttleAlert, 1 to shuttleAlert)
            )
        )
    }

    @Preview(
        name = "Show up to the next three trips in the branching direction",
        group = "2. Red Line branching"
    )
    @Composable
    fun RL1() {
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

    @Preview(name = "Next three trips go to the same destination", group = "2. Red Line branching")
    @Composable
    fun RL2() {
        CardForPreview(
            card(
                redLine,
                jfkUmass,
                listOf(
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(redLineAshmontSouthbound)
                            departureTime = now + 2.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(redLineAshmontSouthbound)
                            departureTime = now + 5.minutes
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
                            trip = objects.trip(redLineBraintreeSouthbound)
                            departureTime = now + 15.minutes
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

    @Preview(name = "Predictions unavailable for a branch", group = "2. Red Line branching")
    @Composable
    fun RL3() {
        CardForPreview(
            card(
                redLine,
                jfkUmass,
                listOf(
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(redLineAshmontSouthbound)
                            departureTime = now + 2.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(redLineAshmontSouthbound)
                            departureTime = now + 5.minutes
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

    @Preview(name = "Service not running on a branch downstream", group = "2. Red Line branching")
    @Composable
    fun RL4() {
        CardForPreview(
            card(
                redLine,
                objects.stop {
                    name = "Park Street"
                    wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
                },
                listOf(
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(redLineAshmontSouthbound)
                            departureTime = now + 2.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip = objects.trip(redLineAshmontSouthbound)
                            departureTime = now + 5.minutes
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
                alertDownstream = mapOf(0 to shuttleAlert)
            )
        )
    }

    @Preview(name = "Service disrupted on a branch downstream", group = "2. Red Line branching")
    @Composable
    fun RL5() {
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

    @Preview(name = "Branching in both directions", group = "3. Green Line branching")
    @Composable
    fun GL1() {
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

    @Preview(name = "Downstream disruption", group = "3. Green Line branching")
    @Composable
    fun GL2() {
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

    @Preview(name = "Branching in one direction", group = "4. Silver Line branching")
    @Composable
    fun SL1() {
        CardForPreview(
            card(
                slWaterfront,
                objects.stop {
                    name = "World Trade Center"
                    wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
                },
                listOf(
                    objects.upcomingTrip(
                        objects.prediction {
                            trip =
                                objects.trip(
                                    objects.routePattern(sl1) {
                                        directionId = 1
                                        typicality = RoutePattern.Typicality.Typical
                                        representativeTrip { headsign = "South Station" }
                                    }
                                )
                            departureTime = now + 5.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip =
                                objects.trip(
                                    objects.routePattern(sl2) {
                                        directionId = 1
                                        typicality = RoutePattern.Typicality.Typical
                                        representativeTrip { headsign = "South Station" }
                                    }
                                )
                            departureTime = now + 7.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip =
                                objects.trip(
                                    objects.routePattern(sl1) {
                                        directionId = 0
                                        typicality = RoutePattern.Typicality.Typical
                                        representativeTrip { headsign = "Logan Airport" }
                                    }
                                )
                            departureTime = now + 5.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip =
                                objects.trip(
                                    objects.routePattern(sl2) {
                                        directionId = 0
                                        typicality = RoutePattern.Typicality.Typical
                                        representativeTrip { headsign = "Design Center" }
                                    }
                                )
                            departureTime = now + 7.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip =
                                objects.trip(
                                    objects.routePattern(sl3) {
                                        directionId = 0
                                        typicality = RoutePattern.Typicality.Typical
                                        representativeTrip { headsign = "Chelsea" }
                                    }
                                )
                            departureTime = now + 9.minutes
                        }
                    ),
                )
            )
        )
    }

    @Preview(name = "Branching in one direction", group = "5. CR branching")
    @Composable
    fun CR1() {
        CardForPreview(
            card(
                providenceLine,
                ruggles,
                listOf(
                    objects.upcomingTrip(
                        objects.prediction {
                            trip =
                                objects.trip(
                                    objects.routePattern(providenceLine) {
                                        directionId = 0
                                        typicality = RoutePattern.Typicality.CanonicalOnly
                                        representativeTrip { headsign = "Stoughton" }
                                    }
                                )
                            departureTime = today.atTime(12, 5).toInstant()
                        }
                    ),
                    objects.upcomingTrip(
                        objects.schedule {
                            trip =
                                objects.trip(
                                    objects.routePattern(providenceLine) {
                                        directionId = 0
                                        typicality = RoutePattern.Typicality.Typical
                                        representativeTrip { headsign = "Providence" }
                                    }
                                )
                            departureTime = today.atTime(15, 28).toInstant()
                        }
                    ),
                    objects.upcomingTrip(
                        objects.schedule {
                            trip =
                                objects.trip(
                                    objects.routePattern(providenceLine) {
                                        directionId = 0
                                        typicality = RoutePattern.Typicality.CanonicalOnly
                                        representativeTrip { headsign = "Wickford Junction" }
                                    }
                                )
                            departureTime = today.atTime(16, 1).toInstant()
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip =
                                objects.trip(
                                    objects.routePattern(providenceLine) {
                                        directionId = 1
                                        typicality = RoutePattern.Typicality.CanonicalOnly
                                        representativeTrip { headsign = "South Station" }
                                    }
                                )
                            departureTime = today.atTime(15, 31).toInstant()
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip =
                                objects.trip(
                                    objects.routePattern(providenceLine) {
                                        directionId = 1
                                        typicality = RoutePattern.Typicality.Typical
                                        representativeTrip { headsign = "South Station" }
                                    }
                                )
                            departureTime = today.atTime(15, 53).toInstant()
                        }
                    ),
                )
            )
        )
    }

    @Preview(
        name = "Next two trips go to the same destination",
        group = "6. Bus route single direction"
    )
    @Composable
    fun Bus1() {
        val lineOrRoute = RouteCardData.LineOrRoute.Route(bus87)
        CardForPreview(
            RouteCardData(
                lineOrRoute,
                listOf(
                    cardStop(
                        lineOrRoute,
                        somervilleAtCarlton,
                        listOf(arlingtonInbound, clarendonInbound),
                        listOf(
                            objects.upcomingTrip(
                                objects.prediction {
                                    trip = objects.trip(arlingtonInbound)
                                    departureTime = now + 16.minutes
                                }
                            ),
                            objects.upcomingTrip(
                                objects.prediction {
                                    trip = objects.trip(clarendonInbound)
                                    departureTime = now + 42.minutes
                                }
                            )
                        ),
                        emptyMap(),
                        emptyMap()
                    ),
                    cardStop(
                        lineOrRoute,
                        bowAtWarren,
                        listOf(arlingtonOutbound, clarendonOutbound),
                        listOf(
                            objects.upcomingTrip(
                                objects.prediction {
                                    trip = objects.trip(arlingtonOutbound)
                                    departureTime = now + 3.minutes
                                }
                            ),
                            objects.upcomingTrip(
                                objects.prediction {
                                    trip = objects.trip(arlingtonOutbound)
                                    departureTime = now + 12.minutes
                                }
                            ),
                            objects.upcomingTrip(
                                objects.prediction {
                                    trip = objects.trip(clarendonOutbound)
                                    departureTime = now + 45.minutes
                                }
                            )
                        ),
                        emptyMap(),
                        emptyMap()
                    )
                ),
                context,
                now
            )
        )
    }

    @Preview(
        name = "Next two trips go to different destinations",
        group = "6. Bus route single direction"
    )
    @Composable
    fun Bus2() {
        val lineOrRoute = RouteCardData.LineOrRoute.Route(bus87)
        CardForPreview(
            RouteCardData(
                lineOrRoute,
                listOf(
                    cardStop(
                        lineOrRoute,
                        somervilleAtCarlton,
                        listOf(arlingtonInbound, clarendonInbound),
                        listOf(
                            objects.upcomingTrip(
                                objects.prediction {
                                    trip = objects.trip(arlingtonInbound)
                                    departureTime = now + 16.minutes
                                }
                            ),
                            objects.upcomingTrip(
                                objects.prediction {
                                    trip = objects.trip(clarendonInbound)
                                    departureTime = now + 42.minutes
                                }
                            )
                        ),
                        emptyMap(),
                        emptyMap()
                    ),
                    cardStop(
                        lineOrRoute,
                        bowAtWarren,
                        listOf(arlingtonOutbound, clarendonOutbound),
                        listOf(
                            objects.upcomingTrip(
                                objects.prediction {
                                    trip = objects.trip(arlingtonOutbound)
                                    departureTime = now + 1.minutes
                                }
                            ),
                            objects.upcomingTrip(
                                objects.prediction {
                                    trip = objects.trip(clarendonOutbound)
                                    departureTime = now + 32.minutes
                                }
                            )
                        ),
                        emptyMap(),
                        emptyMap()
                    )
                ),
                context,
                now
            )
        )
    }

    @Preview(
        name = "Next two trips go to different destinations",
        group = "7. Bus route both directions"
    )
    @Composable
    fun Bus3() {
        CardForPreview(
            card(
                bus15,
                objects.stop {
                    name = "Nubian"
                    wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
                },
                listOf(
                    objects.upcomingTrip(
                        objects.prediction {
                            trip =
                                objects.trip(
                                    objects.routePattern(bus15) {
                                        directionId = 0
                                        representativeTrip { headsign = "St Peter's Square" }
                                    }
                                )
                            departureTime = now + 8.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip =
                                objects.trip(
                                    objects.routePattern(bus15) {
                                        directionId = 0
                                        representativeTrip { headsign = "Kane Square" }
                                    }
                                )
                            departureTime = now + 12.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip =
                                objects.trip(
                                    objects.routePattern(bus15) {
                                        directionId = 1
                                        representativeTrip { headsign = "Ruggles" }
                                    }
                                )
                            departureTime = now + 15.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.prediction {
                            trip =
                                objects.trip(
                                    objects.routePattern(bus15) {
                                        directionId = 1
                                        representativeTrip { headsign = "Ruggles" }
                                    }
                                )
                            departureTime = now + 23.minutes
                        }
                    ),
                )
            )
        )
    }

    @Preview(name = "Service ended on a branch", group = "8. Service ended")
    @Composable
    fun RL6() {
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
                            trip = objects.trip(redLineAshmontSouthbound)
                            departureTime = now + 2.minutes
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

    @Preview(name = "Service ended on all branches", group = "8. Service ended")
    @Composable
    fun RL7() {
        CardForPreview(
            card(
                redLine,
                jfkUmass,
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
                    )
                )
            )
        )
    }

    @Preview(name = "Predictions unavailable on a branch", group = "9. Predictions unavailable")
    @Composable
    fun GL3() {
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
                            trip = objects.trip(greenLineBWestbound)
                            departureTime = now + 10.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.schedule {
                            trip = objects.trip(greenLineDWestbound)
                            departureTime = now + 2.minutes
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

    @Preview(name = "Predictions unavailable on all branches", group = "9. Predictions unavailable")
    @Composable
    fun GL4() {
        CardForPreview(
            card(
                greenLine,
                boylston,
                listOf(
                    objects.upcomingTrip(
                        objects.schedule {
                            trip = objects.trip(greenLineCWestbound)
                            departureTime = now + 3.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.schedule {
                            trip = objects.trip(greenLineBWestbound)
                            departureTime = now + 5.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.schedule {
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

    @Preview(name = "Disruption on a branch", group = "A. Disruption")
    @Composable
    fun GL5() {

        val kenmoreShuttleToRiverside =
            objects.alert {
                effect = Alert.Effect.Shuttle
                informedEntity =
                    mutableListOf(
                        Alert.InformedEntity(
                            activities = listOf(Alert.InformedEntity.Activity.Board),
                            directionId = 0,
                            route = greenLineD.id,
                            routeType = RouteType.LIGHT_RAIL,
                            stop = kenmore.id,
                            trip = null
                        )
                    )
            }

        CardForPreview(
            card(
                greenLine,
                kenmore,
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
                alertHere = mapOf(0 to kenmoreShuttleToRiverside)
            )
        )
    }

    @Preview(
        name = "Disruption on a branch, predictions unavailable for other branches",
        group = "A. Disruption"
    )
    @Composable
    fun GL6() {

        val boylstonShuttleToRiverside =
            objects.alert {
                effect = Alert.Effect.Shuttle
                informedEntity =
                    mutableListOf(
                        Alert.InformedEntity(
                            activities = listOf(Alert.InformedEntity.Activity.Board),
                            directionId = 0,
                            route = greenLineD.id,
                            routeType = RouteType.LIGHT_RAIL,
                            stop = boylston.id,
                            trip = null
                        )
                    )
            }
        CardForPreview(
            card(
                greenLine,
                boylston,
                listOf(
                    objects.upcomingTrip(
                        objects.schedule {
                            trip = objects.trip(greenLineCWestbound)
                            departureTime = now + 3.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.schedule {
                            trip = objects.trip(greenLineBWestbound)
                            departureTime = now + 5.minutes
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
                alertHere = mapOf(0 to boylstonShuttleToRiverside)
            )
        )
    }

    @Preview(name = "Disruption on all branches", group = "A. Disruption")
    @Composable
    fun GL7() {

        val shuttleAllBranches =
            objects.alert {
                effect = Alert.Effect.Shuttle
                informedEntity =
                    mutableListOf(
                        Alert.InformedEntity(
                            activities = listOf(Alert.InformedEntity.Activity.Board),
                            directionId = 0,
                            route = greenLineB.id,
                            routeType = RouteType.LIGHT_RAIL,
                            stop = boylston.id,
                            trip = null
                        ),
                        Alert.InformedEntity(
                            activities = listOf(Alert.InformedEntity.Activity.Board),
                            directionId = 0,
                            route = greenLineC.id,
                            routeType = RouteType.LIGHT_RAIL,
                            stop = boylston.id,
                            trip = null
                        ),
                        Alert.InformedEntity(
                            activities = listOf(Alert.InformedEntity.Activity.Board),
                            directionId = 0,
                            route = greenLineD.id,
                            routeType = RouteType.LIGHT_RAIL,
                            stop = boylston.id,
                            trip = null
                        )
                    )
            }

        CardForPreview(
            card(
                greenLine,
                boylston,
                listOf(
                    objects.upcomingTrip(
                        objects.schedule {
                            trip = objects.trip(greenLineDEastbound)
                            departureTime = now + 1.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.schedule {
                            trip = objects.trip(greenLineBEastbound)
                            departureTime = now + 6.minutes
                        }
                    ),
                    objects.upcomingTrip(
                        objects.schedule {
                            trip = objects.trip(greenLineCEastbound)
                            departureTime = now + 12.minutes
                        }
                    ),
                ),
                alertHere = mapOf(0 to shuttleAllBranches)
            )
        )
    }
}
