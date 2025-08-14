package com.mbta.tid.mbta_app.utils

import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.WheelchairBoardingStatus
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime

public open class RouteCardPreviewData {
    private val today: LocalDate = EasternTimeInstant.now().local.date
    private val objects = TestData.clone()
    private val greenLine = objects.getLine("line-Green")
    private val slWaterfront = objects.getLine("line-SLWaterfront")
    private val orangeLine = objects.getRoute("Orange")
    private val redLine = objects.getRoute("Red")
    private val greenLineB = objects.getRoute("Green-B")
    private val greenLineC = objects.getRoute("Green-C")
    private val greenLineD = objects.getRoute("Green-D")
    private val sl1 = objects.getRoute("741")
    private val sl2 = objects.getRoute("742")
    private val sl3 = objects.getRoute("743")
    private val providenceLine = objects.getRoute("CR-Providence")
    private val bus87 = objects.getRoute("87")
    private val bus15 = objects.getRoute("15")
    private val orangeLineSouthbound = objects.getRoutePattern("Orange-3-0")
    private val orangeLineNorthbound = objects.getRoutePattern("Orange-3-1")
    private val redLineAshmontSouthbound = objects.getRoutePattern("Red-1-0")
    private val redLineBraintreeSouthbound = objects.getRoutePattern("Red-3-0")
    private val redLineAshmontNorthbound = objects.getRoutePattern("Red-1-1")
    private val redLineBraintreeNorthbound = objects.getRoutePattern("Red-3-1")
    private val greenLineBWestbound = objects.getRoutePattern("Green-B-812-0")
    private val greenLineBEastbound = objects.getRoutePattern("Green-B-812-1")
    private val greenLineCWestbound = objects.getRoutePattern("Green-C-832-0")
    private val greenLineCEastbound = objects.getRoutePattern("Green-C-832-1")
    private val greenLineDWestbound = objects.getRoutePattern("Green-D-855-0")
    private val greenLineDEastbound = objects.getRoutePattern("Green-D-855-1")
    private val arlingtonOutbound = objects.getRoutePattern("87-2-0")
    private val arlingtonInbound = objects.getRoutePattern("87-2-1")
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
    private val ruggles = objects.getStop("place-rugg")
    private val jfkUmass = objects.getStop("place-jfk")
    private val boylston = objects.getStop("place-boyls")
    private val kenmore = objects.getStop("place-kencl")
    private val somervilleAtCarlton = objects.getStop("2595")
    private val bowAtWarren = objects.getStop("26131")
    private val shuttleAlert = objects.alert { effect = Alert.Effect.Shuttle }
    private val suspensionAlert = objects.alert { effect = Alert.Effect.Suspension }
    private val context = RouteCardData.Context.NearbyTransit

    public val now: EasternTimeInstant = EasternTimeInstant(today.atTime(11, 30))
    public val global: GlobalResponse = GlobalResponse(objects)

    private fun cardStop(
        lineOrRoute: RouteCardData.LineOrRoute,
        stop: Stop,
        patterns: List<RoutePattern>,
        trips: List<UpcomingTrip>,
        alertHere: Map<Int, Alert>,
        alertDownstream: Map<Int, Alert>,
    ) =
        RouteCardData.RouteStopData(
            lineOrRoute,
            stop,
            listOfNotNull(
                RouteCardData.Leaf(
                        lineOrRoute,
                        stop,
                        0,
                        patterns.filter { it.directionId == 0 },
                        setOf(stop.id),
                        trips.filter { it.trip.directionId == 0 },
                        listOfNotNull(alertHere[0]),
                        true,
                        true,
                        listOfNotNull(alertDownstream[0]),
                        context,
                    )
                    .takeUnless { it.routePatterns.isEmpty() },
                RouteCardData.Leaf(
                        lineOrRoute,
                        stop,
                        1,
                        patterns.filter { it.directionId == 1 },
                        setOf(stop.id),
                        trips.filter { it.trip.directionId == 1 },
                        listOfNotNull(alertHere[1]),
                        true,
                        true,
                        listOfNotNull(alertDownstream[1]),
                        context,
                    )
                    .takeUnless { it.routePatterns.isEmpty() },
            ),
            global,
        )

    private fun card(
        lineOrRoute: RouteCardData.LineOrRoute,
        stop: Stop,
        patterns: List<RoutePattern>,
        trips: List<UpcomingTrip>,
        alertHere: Map<Int, Alert>,
        alertDownstream: Map<Int, Alert>,
    ) =
        RouteCardData(
            lineOrRoute,
            listOf(cardStop(lineOrRoute, stop, patterns, trips, alertHere, alertDownstream)),
            now,
        )

    private fun card(
        route: Route,
        stop: Stop,
        trips: List<UpcomingTrip>,
        alertHere: Map<Int, Alert> = emptyMap(),
        alertDownstream: Map<Int, Alert> = emptyMap(),
    ) =
        card(
            RouteCardData.LineOrRoute.Route(route),
            stop,
            objects.routePatterns.values.filter { it.routeId == route.id },
            trips,
            alertHere,
            alertDownstream,
        )

    private fun card(
        line: Line,
        stop: Stop,
        trips: List<UpcomingTrip>,
        alertHere: Map<Int, Alert> = emptyMap(),
        alertDownstream: Map<Int, Alert> = emptyMap(),
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
            alertDownstream,
        )
    }

    // "Downstream disruption" group = "1. Orange Line disruption"
    public fun OL1(): RouteCardData =
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
                ),
            ),
            alertDownstream = mapOf(0 to shuttleAlert),
        )

    // "Disrupted stop" group = "1. Orange Line disruption"
    public fun OL2(): RouteCardData =
        card(
            orangeLine,
            objects.stop {
                name = "Stony Brook"
                wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
            },
            listOf(),
            mapOf(0 to shuttleAlert, 1 to shuttleAlert),
        )

    // "Show up to the next three trips in the branching direction" group = "2. Red Line branching"
    public fun RL1(): RouteCardData =
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
                ),
            ),
        )

    // "Next three trips go to the same destination" group = "2. Red Line branching"
    public fun RL2(): RouteCardData =
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
                ),
            ),
        )

    // "Predictions unavailable for a branch" group = "2. Red Line branching"
    public fun RL3(): RouteCardData =
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
                ),
            ),
        )

    // "Service not running on a branch downstream", group = "2. Red Line branching"
    public fun RL4(): RouteCardData =
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
                ),
            ),
            alertDownstream = mapOf(0 to shuttleAlert),
        )

    // "Service disrupted on a branch downstream", group = "2. Red Line branching"
    public fun RL5(): RouteCardData =
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
                ),
            ),
            alertDownstream = mapOf(0 to suspensionAlert),
        )

    // "Branching in both directions" group = "3. Green Line branching")
    public fun GL1(): RouteCardData =
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
        )

    // "Downstream disruption", group = "3. Green Line branching"
    public fun GL2(): RouteCardData =
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
            alertDownstream = mapOf(0 to suspensionAlert),
        )

    // "Branching in one direction", group = "4. Silver Line branching"
    public fun SL1(): RouteCardData =
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
            ),
        )

    // "Branching in one direction", group = "5. CR branching"
    public fun CR1(): RouteCardData =
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
                        departureTime = EasternTimeInstant(today.atTime(12, 5))
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
                        departureTime = EasternTimeInstant(today.atTime(15, 28))
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
                        departureTime = EasternTimeInstant(today.atTime(16, 1))
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
                        departureTime = EasternTimeInstant(today.atTime(15, 31))
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
                        departureTime = EasternTimeInstant(today.atTime(15, 53))
                    }
                ),
            ),
        )

    // "Next two trips go to the same destination" group = "6. Bus route single direction"
    public fun Bus1(): RouteCardData {
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
                        ),
                    ),
                    emptyMap(),
                    emptyMap(),
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
                        ),
                    ),
                    emptyMap(),
                    emptyMap(),
                ),
            ),
            now,
        )
    }

    // "Next two trips go to different destinations" group = "6. Bus route single direction"
    public fun Bus2(): RouteCardData {
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
                        ),
                    ),
                    emptyMap(),
                    emptyMap(),
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
                        ),
                    ),
                    emptyMap(),
                    emptyMap(),
                ),
            ),
            now,
        )
    }

    // "Next two trips go to different destinations" group = "7. Bus route both directions"
    public fun Bus3(): RouteCardData =
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
            ),
        )

    // "Service ended on a branch", group = "8. Service ended")
    public fun RL6(): RouteCardData =
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
                ),
            ),
        )

    // "Service ended on all branches" group = "8. Service ended"
    public fun RL7(): RouteCardData =
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
                ),
            ),
        )

    // "Predictions unavailable on a branch", group = "9. Predictions unavailable"
    public fun GL3(): RouteCardData =
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
            ),
        )

    // "Predictions unavailable on all branches", group = "9. Predictions unavailable"
    public fun GL4(): RouteCardData =
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
            ),
        )

    // "Disruption on a branch" group = "A. Disruption"
    public fun GL5(): RouteCardData {
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
                            trip = null,
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
            alertHere = mapOf(0 to kenmoreShuttleToRiverside),
        )
    }

    // "Disruption on a branch, predictions unavailable for other branches" group = "A. Disruption"
    public fun GL6(): RouteCardData {
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
                            trip = null,
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
            alertHere = mapOf(0 to boylstonShuttleToRiverside),
        )
    }

    // "Disruption on all branches" group = "A. Disruption"
    public fun GL7(): RouteCardData {

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
                            trip = null,
                        ),
                        Alert.InformedEntity(
                            activities = listOf(Alert.InformedEntity.Activity.Board),
                            directionId = 0,
                            route = greenLineC.id,
                            routeType = RouteType.LIGHT_RAIL,
                            stop = boylston.id,
                            trip = null,
                        ),
                        Alert.InformedEntity(
                            activities = listOf(Alert.InformedEntity.Activity.Board),
                            directionId = 0,
                            route = greenLineD.id,
                            routeType = RouteType.LIGHT_RAIL,
                            stop = boylston.id,
                            trip = null,
                        ),
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
            alertHere = mapOf(0 to shuttleAllBranches),
        )
    }
}
