package com.mbta.tid.mbta_app.utils

import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
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

open class RouteCardPreviewData {
    private fun LocalDateTime.toInstant(): Instant = toInstant(TimeZone.currentSystemDefault())

    private val today: LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    private val objects = ObjectCollectionBuilder()
    private val greenLine =
        objects.line {
            id = "line-Green"
            color = "00843D"
            longName = "Green Line"
            textColor = "FFFFFF"
        }
    private val slWaterfront =
        objects.line {
            color = "7C878E"
            longName = "Silver Line SL1/SL2/SL3"
            textColor = "FFFFFF"
        }
    private val orangeLine =
        objects.route {
            id = "Orange"
            color = "ED8B00"
            directionDestinations = listOf("Forest Hills", "Oak Grove")
            directionNames = listOf("South", "North")
            longName = "Orange Line"
            textColor = "FFFFFF"
            type = RouteType.HEAVY_RAIL
        }
    private val redLine =
        objects.route {
            id = "Red"
            color = "DA291C"
            directionDestinations = listOf("Ashmont/Braintree", "Alewife")
            directionNames = listOf("South", "North")
            longName = "Red Line"
            textColor = "FFFFFF"
            type = RouteType.HEAVY_RAIL
        }
    private val greenLineB =
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
    private val greenLineC =
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
    private val greenLineD =
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
    private val sl1 =
        objects.route {
            color = slWaterfront.color
            directionNames = listOf("Outbound", "Inbound")
            lineId = slWaterfront.id
            shortName = "SL1"
            textColor = slWaterfront.textColor
            type = RouteType.BUS
        }
    private val sl2 =
        objects.route {
            color = slWaterfront.color
            directionNames = listOf("Outbound", "Inbound")
            lineId = slWaterfront.id
            shortName = "SL2"
            textColor = slWaterfront.textColor
            type = RouteType.BUS
        }
    private val sl3 =
        objects.route {
            color = slWaterfront.color
            directionNames = listOf("Outbound", "Inbound")
            lineId = slWaterfront.id
            shortName = "SL3"
            textColor = slWaterfront.textColor
            type = RouteType.BUS
        }
    private val providenceLine =
        objects.route {
            color = "80276C"
            directionDestinations = listOf("Stoughton or Wickford Junction", "South Station")
            directionNames = listOf("Outbound", "Inbound")
            longName = "Providence/Stoughton Line"
            textColor = "FFFFFF"
            type = RouteType.COMMUTER_RAIL
        }
    private val bus87 =
        objects.route {
            color = "FFC72C"
            directionDestinations = listOf("Clarendon Hill or Arlington Center", "Lechmere Station")
            directionNames = listOf("Outbound", "Inbound")
            shortName = "87"
            textColor = "000000"
            type = RouteType.BUS
        }
    private val bus15 =
        objects.route {
            color = "FFC72C"
            directionDestinations =
                listOf("Fields Corner Station or St Peter's Square", "Ruggles Station")
            directionNames = listOf("Outbound", "Inbound")
            shortName = "15"
            textColor = "000000"
            type = RouteType.BUS
        }
    private val orangeLineSouthbound =
        objects.routePattern(orangeLine) {
            directionId = 0
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Forest Hills" }
        }
    private val orangeLineNorthbound =
        objects.routePattern(orangeLine) {
            directionId = 1
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Oak Grove" }
        }
    private val redLineAshmontSouthbound =
        objects.routePattern(redLine) {
            directionId = 0
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Ashmont" }
        }
    private val redLineBraintreeSouthbound =
        objects.routePattern(redLine) {
            directionId = 0
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Braintree" }
        }
    private val redLineAshmontNorthbound =
        objects.routePattern(redLine) {
            directionId = 1
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Alewife" }
        }
    private val redLineBraintreeNorthbound =
        objects.routePattern(redLine) {
            directionId = 1
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Alewife" }
        }
    private val greenLineBWestbound =
        objects.routePattern(greenLineB) {
            directionId = 0
            typicality = RoutePattern.Typicality.Typical
            representativeTrip {
                headsign = "Boston College"
                // only the stops required to correctly apply the direction special casing
                stopIds = listOf("place-boyls", "place-armnl", "place-kencl", "place-lake")
            }
        }
    private val greenLineBEastbound =
        objects.routePattern(greenLineB) {
            directionId = 1
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Government Center" }
        }
    private val greenLineCWestbound =
        objects.routePattern(greenLineC) {
            directionId = 0
            typicality = RoutePattern.Typicality.Typical
            representativeTrip {
                headsign = "Cleveland Circle"
                // only the stops required to correctly apply the direction special casing
                stopIds = listOf("place-boyls", "place-armnl", "place-kencl", "place-clmnl")
            }
        }
    private val greenLineCEastbound =
        objects.routePattern(greenLineC) {
            directionId = 1
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Government Center" }
        }
    private val greenLineDWestbound =
        objects.routePattern(greenLineD) {
            directionId = 0
            typicality = RoutePattern.Typicality.Typical
            representativeTrip {
                headsign = "Riverside"
                // only the stops required to correctly apply the direction special casing
                stopIds = listOf("place-boyls", "place-armnl", "place-kencl", "place-river")
            }
        }
    private val greenLineDEastbound =
        objects.routePattern(greenLineD) {
            directionId = 1
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { headsign = "Union Square" }
        }
    private val arlingtonOutbound =
        objects.routePattern(bus87) {
            directionId = 0
            representativeTrip { headsign = "Arlington Center" }
        }
    private val arlingtonInbound =
        objects.routePattern(bus87) {
            directionId = 1
            representativeTrip { headsign = "Lechmere" }
        }
    private val clarendonOutbound =
        objects.routePattern(bus87) {
            directionId = 0
            representativeTrip { headsign = "Clarendon Hill" }
        }
    private val clarendonInbound =
        objects.routePattern(bus87) {
            directionId = 1
            representativeTrip { headsign = "Lechmere" }
        }
    private val ruggles =
        objects.stop {
            name = "Ruggles"
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
        }
    private val jfkUmass =
        objects.stop {
            name = "JFK/UMass"
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
        }
    private val boylston =
        objects.stop {
            id = "place-boyls"
            name = "Boylston"
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE
        }

    private val kenmore =
        objects.stop {
            id = "place-kencl"
            name = "Kenmore"
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
        }
    private val somervilleAtCarlton =
        objects.stop {
            name = "Somerville Ave @ Carlton St"
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
        }
    private val bowAtWarren =
        objects.stop {
            name = "Bow St @ Warren Ave"
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
        }
    private val shuttleAlert = objects.alert { effect = Alert.Effect.Shuttle }
    private val suspensionAlert = objects.alert { effect = Alert.Effect.Suspension }
    private val context = RouteCardData.Context.NearbyTransit

    val now: Instant = today.atTime(11, 30).toInstant()
    val global = GlobalResponse(objects)

    private fun cardStop(
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

    private fun card(
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

    private fun card(
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

    private fun card(
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

    // "Downstream disruption" group = "1. Orange Line disruption"
    fun OL1() =
        card(
            orangeLine,
            ruggles,
            listOf(
                objects.upcomingTrip(
                    objects.prediction {
                        trip = objects.trip(orangeLineSouthbound) { headsign = "Jackson Square" }
                        departureTime = now + 5.minutes
                    }
                ),
                objects.upcomingTrip(
                    objects.prediction {
                        trip = objects.trip(orangeLineSouthbound) { headsign = "Jackson Square" }
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

    // "Disrupted stop" group = "1. Orange Line disruption"
    fun OL2() =
        card(
            orangeLine,
            objects.stop {
                name = "Stony Brook"
                wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
            },
            listOf(),
            mapOf(0 to shuttleAlert, 1 to shuttleAlert)
        )

    // "Show up to the next three trips in the branching direction" group = "2. Red Line branching"
    fun RL1() =
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

    // "Next three trips go to the same destination" group = "2. Red Line branching"
    fun RL2() =
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

    // "Predictions unavailable for a branch" group = "2. Red Line branching"
    fun RL3() =
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

    // "Service not running on a branch downstream", group = "2. Red Line branching"
    fun RL4() =
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

    // "Service disrupted on a branch downstream", group = "2. Red Line branching"
    fun RL5() =
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
                        trip = objects.trip(redLineBraintreeSouthbound) { headsign = "Wollaston" }
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

    // "Branching in both directions" group = "3. Green Line branching")
    fun GL1() =
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

    // "Downstream disruption", group = "3. Green Line branching"
    fun GL2() =
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

    // "Branching in one direction", group = "4. Silver Line branching"
    fun SL1() =
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

    // "Branching in one direction", group = "5. CR branching"
    fun CR1() =
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

    // "Next two trips go to the same destination" group = "6. Bus route single direction"
    fun Bus1(): RouteCardData {
        val lineOrRoute = RouteCardData.LineOrRoute.Route(bus87)
        return RouteCardData(
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
    }

    // "Next two trips go to different destinations" group = "6. Bus route single direction"
    fun Bus2(): RouteCardData {
        val lineOrRoute = RouteCardData.LineOrRoute.Route(bus87)
        return RouteCardData(
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
    }

    // "Next two trips go to different destinations" group = "7. Bus route both directions"
    fun Bus3() =
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

    // "Service ended on a branch", group = "8. Service ended")
    fun RL6() =
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

    // "Service ended on all branches" group = "8. Service ended"
    fun RL7() =
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

    // "Predictions unavailable on a branch", group = "9. Predictions unavailable"
    fun GL3() =
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

    // "Predictions unavailable on all branches", group = "9. Predictions unavailable"
    fun GL4() =
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

    // "Disruption on a branch" group = "A. Disruption"
    fun GL5(): RouteCardData {
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

        return card(
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
    }

    // "Disruption on a branch, predictions unavailable for other branches" group = "A. Disruption"
    fun GL6(): RouteCardData {
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
        return card(
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
    }

    // "Disruption on all branches" group = "A. Disruption"
    fun GL7(): RouteCardData {

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

        return card(
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
    }
}
