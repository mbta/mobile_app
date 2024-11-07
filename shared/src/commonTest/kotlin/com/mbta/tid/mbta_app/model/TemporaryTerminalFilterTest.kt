package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock

/** Unit tests for the individual helper functions defined in [TemporaryTerminalFilter]. */
class TemporaryTerminalFilterTest {
    private class AppliesToRouteTestHelper {
        val boardExitRide =
            listOf(
                Alert.InformedEntity.Activity.Board,
                Alert.InformedEntity.Activity.Exit,
                Alert.InformedEntity.Activity.Ride
            )

        val objects = ObjectCollectionBuilder()
        lateinit var route: Route

        fun createRoute() {
            route = objects.route { type = RouteType.HEAVY_RAIL }
        }

        lateinit var alert: Alert

        fun createAlert() {
            alert =
                objects.alert {
                    effect = Alert.Effect.Suspension
                    informedEntity(activities = boardExitRide, route = route.id)
                }
        }

        lateinit var typical: RoutePattern
        lateinit var diversion: RoutePattern

        fun createPatterns() {
            typical = objects.routePattern(route) { typicality = RoutePattern.Typicality.Typical }
            diversion =
                objects.routePattern(route) { typicality = RoutePattern.Typicality.Diversion }
        }

        fun createSchedule() {
            objects.schedule { trip = objects.trip(diversion) }
        }

        fun createPrediction() {
            objects.prediction { trip = objects.trip(typical) }
        }

        fun filter() =
            TemporaryTerminalFilter(
                NearbyStaticData(emptyList()),
                PredictionsStreamDataResponse(objects),
                GlobalResponse(objects, emptyMap()),
                listOf(alert),
                ScheduleResponse(objects)
            )
    }

    private fun appliesToRouteTest(block: AppliesToRouteTestHelper.() -> Unit) =
        AppliesToRouteTestHelper().block()

    @Test
    fun `appliesToRoute accepts non-branching suspension`() = appliesToRouteTest {
        createRoute()
        createAlert()
        createPatterns()
        createSchedule()
        createPrediction()

        val filter = filter()

        assertTrue(filter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects bus`() = appliesToRouteTest {
        route = objects.route { type = RouteType.BUS }
        createAlert()
        createPatterns()
        createSchedule()
        createPrediction()

        val filter = filter()

        assertFalse(filter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects insufficient alert effect`() = appliesToRouteTest {
        createRoute()
        alert =
            objects.alert {
                effect = Alert.Effect.StationClosure
                informedEntity(activities = boardExitRide, route = route.id)
            }
        createPatterns()
        createSchedule()
        createPrediction()

        val filter = filter()

        assertFalse(filter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects wrong route`() = appliesToRouteTest {
        createRoute()
        alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(activities = boardExitRide, route = "not-${route.id}")
            }
        createPatterns()
        createSchedule()
        createPrediction()

        val filter = filter()

        assertFalse(filter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects without non-typical schedule`() = appliesToRouteTest {
        createRoute()
        createAlert()
        createPatterns()
        objects.schedule { trip = objects.trip(typical) }
        createPrediction()

        val filter = filter()

        assertFalse(filter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects without missing typical schedule`() = appliesToRouteTest {
        createRoute()
        createAlert()
        createPatterns()
        createSchedule()
        objects.schedule { trip = objects.trip(typical) }
        createPrediction()

        val filter = filter()

        assertFalse(filter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute accepts branching suspension`() = appliesToRouteTest {
        createRoute()
        createAlert()
        val typicalA = objects.routePattern(route) { typicality = RoutePattern.Typicality.Typical }
        val typicalB = objects.routePattern(route) { typicality = RoutePattern.Typicality.Typical }
        val diversionA =
            objects.routePattern(route) { typicality = RoutePattern.Typicality.Diversion }
        objects.schedule { trip = objects.trip(diversionA) }
        objects.schedule { trip = objects.trip(typicalB) }
        objects.prediction { trip = objects.trip(typicalA) }

        val filter = filter()

        assertTrue(filter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects no schedules`() = appliesToRouteTest {
        createRoute()
        createAlert()
        createPatterns()
        createPrediction()

        val filter = filter()

        assertFalse(filter.appliesToRoute(route))
    }

    private class DiscardProbableTemporaryTerminalsTestHelper {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()

        var nextPatternSortOrder = 0

        // this should probably be somewhere more useful than both here and TripDetailsStopListTest
        private val childStopId = Regex("""(?<parentStop>[A-Za-z]+)\d+""")

        // Generate stops dynamically based on ID, using a numeric suffix to indicate children.
        fun stop(
            stopId: String,
            childStopIds: List<String> = listOf(),
            connectingStopIds: List<String> = listOf()
        ): Stop {
            objects.stops[stopId]?.let {
                return it
            }

            val parentStationId =
                when (val match = childStopId.matchEntire(stopId)) {
                    null -> null
                    else -> stop(match.groups["parentStop"]!!.value).id
                }
            return objects.stop {
                id = stopId
                this.parentStationId = parentStationId
                this.childStopIds = childStopIds
                this.connectingStopIds = connectingStopIds
            }
        }

        fun typical(route: Route, directionId: Int, vararg stopIds: String) =
            objects.routePattern(route) {
                this.directionId = directionId
                typicality = RoutePattern.Typicality.Typical
                sortOrder = nextPatternSortOrder
                nextPatternSortOrder += 1
                representativeTrip { this.stopIds = stopIds.asList() }
            }

        fun diversion(route: Route, directionId: Int, vararg stopIds: String) =
            objects.routePattern(route) {
                this.directionId = directionId
                typicality = RoutePattern.Typicality.Diversion
                sortOrder = nextPatternSortOrder
                nextPatternSortOrder += 1
                representativeTrip { this.stopIds = stopIds.asList() }
            }

        fun schedule(routePattern: RoutePattern) =
            objects.schedule {
                this.trip = objects.trip(routePattern)
                this.departureTime = Clock.System.now() + 5.minutes
            }

        fun prediction(routePattern: RoutePattern) =
            objects.prediction {
                this.trip = objects.trip(routePattern)
                this.departureTime = Clock.System.now() + 5.minutes
            }

        fun stopPatterns(block: NearbyStaticDataBuilder.() -> Unit): NearbyStaticData.StopPatterns {
            val staticData = NearbyStaticData.build(block)
            return staticData.data.single().patternsByStop.single()
        }

        val filter by lazy {
            TemporaryTerminalFilter(
                NearbyStaticData(emptyList()),
                PredictionsStreamDataResponse(objects),
                GlobalResponse(objects, emptyMap()),
                emptyList(),
                ScheduleResponse(objects)
            )
        }
    }

    private fun filterPatternsAtStopTest(block: FilterPatternsAtStopTestHelper.() -> Unit) =
        FilterPatternsAtStopTestHelper().block()

    @Test
    fun `filterPatternsAtStop handles easy case`() = filterPatternsAtStopTest {
        val typical = typical(route, 0, "a", "b", "c", "d", "e")
        val early = diversion(route, 0, "a", "b")

        schedule(early)
        prediction(typical)

        val original = stopPatterns {
            route(route) {
                stop(stop("a")) {
                    headsign("e", listOf(typical))
                    headsign("b", listOf(early))
                }
            }
        }

        val expected = stopPatterns {
            route(route) { stop(stop("a")) { headsign("e", listOf(typical)) } }
        }

        assertEquals(expected, filter.filterPatternsAtStop(original))
    }

    @Test
    fun `filterPatternsAtStop does not hide predictions`() = filterPatternsAtStopTest {
        val typical = typical(route, 0, "a", "b", "c", "d", "e")
        val early = diversion(route, 0, "a", "b")

        schedule(early)
        prediction(early)

        val original = stopPatterns {
            route(route) {
                stop(stop("a")) {
                    headsign("e", listOf(typical))
                    headsign("b", listOf(early))
                }
            }
        }

        assertEquals(original, filter.filterPatternsAtStop(original))
    }

    @Test
    fun `filterPatternsAtStop handles Red Line`() = filterPatternsAtStopTest {
        stop("Al", listOf("Al0", "Al1"))
        val harvard = stop("Ha", listOf("Ha0", "Ha1"))
        stop("Ke", listOf("Ke0", "Ke1"))
        // 0_ southbound 1_ northbound _0 Ashmont _1 Braintree
        val jfkUmass = stop("Jf", listOf("Jf00", "Jf01", "Jf10", "Jf11"))
        stop("As", listOf("As0", "As1"))
        stop("Br", listOf("Br0", "Br1"))
        val typicalBSouth = typical(route, 0, "Al0", "Ha0", "Ke0", "Jf01", "Br0")
        val typicalASouth = typical(route, 0, "Al0", "Ha0", "Ke0", "Jf00", "As0")
        val divASouth = diversion(route, 0, "Jf00", "As0")
        val divBSouth = diversion(route, 0, "Jf01", "Br0")
        val divSSouth = diversion(route, 0, "Al0", "Ha0", "Ke0")
        val typicalBNorth = typical(route, 1, "Br1", "Jf11", "Ke1", "Ha1", "Al1")
        val typicalANorth = typical(route, 1, "As1", "Jf10", "Ke1", "Ha1", "Al1")
        val divANorth = diversion(route, 1, "As1", "Jf10")
        val divBNorth = diversion(route, 1, "Br1", "Jf11")
        val divSNorth = diversion(route, 1, "Ke1", "Ha1", "Al1")

        schedule(divASouth)
        schedule(divBSouth)
        schedule(divSSouth)
        schedule(divANorth)
        schedule(divBNorth)
        schedule(divSNorth)
        prediction(typicalBSouth)
        prediction(typicalBNorth)
        prediction(typicalANorth)

        val originalHarvard = stopPatterns {
            route(route) {
                stop(harvard) {
                    headsign("Braintree", listOf(typicalBSouth))
                    headsign("Ashmont", listOf(typicalASouth))
                    headsign("Kendall/MIT", listOf(divSSouth))
                    headsign("Alewife", listOf(typicalBNorth, typicalANorth, divSNorth))
                }
            }
        }

        val expectedHarvard = stopPatterns {
            route(route) {
                stop(harvard) {
                    headsign("Braintree", listOf(typicalBSouth))
                    headsign("Ashmont", listOf(typicalASouth))
                    headsign("Alewife", listOf(typicalBNorth, typicalANorth, divSNorth))
                }
            }
        }

        val originalJFK = stopPatterns {
            route(route) {
                stop(jfkUmass) {
                    headsign("Braintree", listOf(typicalBSouth, divBSouth))
                    headsign("Ashmont", listOf(typicalASouth, divASouth))
                    headsign("Alewife", listOf(typicalBNorth, typicalANorth))
                    headsign("JFK/UMass", listOf(divANorth, divBNorth))
                }
            }
        }

        val expectedJFK = stopPatterns {
            route(route) {
                stop(jfkUmass) {
                    headsign("Braintree", listOf(typicalBSouth, divBSouth))
                    headsign("Ashmont", listOf(typicalASouth, divASouth))
                    headsign("Alewife", listOf(typicalBNorth, typicalANorth))
                }
            }
        }

        assertEquals(expectedHarvard, filter.filterPatternsAtStop(originalHarvard))
        assertEquals(expectedJFK, filter.filterPatternsAtStop(originalJFK))
    }
}
