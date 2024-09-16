package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Unit tests for the individual helper functions defined in [TemporaryTerminalRewriter]. */
class TemporaryTerminalRewriterTest {
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

        fun rewriter() =
            TemporaryTerminalRewriter(
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

        val rewriter = rewriter()

        assertTrue(rewriter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects bus`() = appliesToRouteTest {
        route = objects.route { type = RouteType.BUS }
        createAlert()
        createPatterns()
        createSchedule()
        createPrediction()

        val rewriter = rewriter()

        assertFalse(rewriter.appliesToRoute(route))
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

        val rewriter = rewriter()

        assertFalse(rewriter.appliesToRoute(route))
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

        val rewriter = rewriter()

        assertFalse(rewriter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects without non-typical schedule`() = appliesToRouteTest {
        createRoute()
        createAlert()
        createPatterns()
        objects.schedule { trip = objects.trip(typical) }
        createPrediction()

        val rewriter = rewriter()

        assertFalse(rewriter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects without missing typical schedule`() = appliesToRouteTest {
        createRoute()
        createAlert()
        createPatterns()
        createSchedule()
        objects.schedule { trip = objects.trip(typical) }
        createPrediction()

        val rewriter = rewriter()

        assertFalse(rewriter.appliesToRoute(route))
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

        val rewriter = rewriter()

        assertTrue(rewriter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects no schedules`() = appliesToRouteTest {
        createRoute()
        createAlert()
        createPatterns()
        createPrediction()

        val rewriter = rewriter()

        assertFalse(rewriter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects no predictions`() = appliesToRouteTest {
        createRoute()
        createAlert()
        createPatterns()
        createSchedule()

        val rewriter = rewriter()

        assertFalse(rewriter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects with diversion prediction`() = appliesToRouteTest {
        createRoute()
        createAlert()
        createPatterns()
        createSchedule()
        createPrediction()
        objects.prediction { trip = objects.trip(diversion) }

        val rewriter = rewriter()

        assertFalse(rewriter.appliesToRoute(route))
    }

    private class TruncatedPatternTestHelper {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()

        fun typical(directionId: Int, vararg stopIds: String) =
            objects.routePattern(route) {
                this.directionId = directionId
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { this.stopIds = stopIds.asList() }
            }

        fun diversion(directionId: Int, vararg stopIds: String) =
            objects.routePattern(route) {
                this.directionId = directionId
                typicality = RoutePattern.Typicality.Diversion
                representativeTrip { this.stopIds = stopIds.asList() }
            }

        fun schedule(routePattern: RoutePattern) =
            objects.schedule { trip = objects.trip(routePattern) }

        val rewriter by lazy {
            TemporaryTerminalRewriter(
                NearbyStaticData(emptyList()),
                PredictionsStreamDataResponse(objects),
                GlobalResponse(objects, emptyMap()),
                emptyList(),
                ScheduleResponse(objects)
            )
        }

        fun RoutePattern.isTruncationOf(fullPattern: RoutePattern, stopId: String) =
            with(rewriter) {
                val fullPatternStopIds = objects.trips[fullPattern.representativeTripId]?.stopIds
                checkNotNull(fullPatternStopIds)
                isTruncationOf(fullPattern, fullPatternStopIds, stopId)
            }

        fun truncatedPattern(fullPattern: RoutePattern, stopId: String) =
            rewriter.run {
                val fullPatternStopIds = objects.trips[fullPattern.representativeTripId]?.stopIds
                checkNotNull(fullPatternStopIds)
                truncatedPattern(fullPattern, fullPatternStopIds, stopId)
            }
    }

    private fun truncatedPatternTest(block: TruncatedPatternTestHelper.() -> Unit) =
        TruncatedPatternTestHelper().block()

    @Test
    fun `isTruncationOf accepts truncations`() = truncatedPatternTest {
        val fullPattern = typical(0, "a", "b", "c", "d", "e")
        val early = diversion(0, "a", "b")
        val late = diversion(0, "d", "e")

        assertTrue(early.isTruncationOf(fullPattern, "a"))
        assertTrue(early.isTruncationOf(fullPattern, "b"))
        assertTrue(late.isTruncationOf(fullPattern, "d"))
        assertTrue(late.isTruncationOf(fullPattern, "e"))
    }

    @Test
    fun `isTruncationOf rejects typical`() = truncatedPatternTest {
        val fullPattern = typical(0, "a", "b", "c", "d", "e")
        val almostEarly = typical(0, "a", "b")

        assertFalse(almostEarly.isTruncationOf(fullPattern, "a"))
    }

    @Test
    fun `isTruncationOf rejects wrong direction`() = truncatedPatternTest {
        val fullPattern = typical(0, "a", "b", "c", "d", "e")
        // on subway it shouldn't be possible to not also have "b", "a" here anyway
        val notEarly = diversion(1, "a", "b")

        assertFalse(notEarly.isTruncationOf(fullPattern, "a"))
    }

    @Test
    fun `isTruncationOf does not crash on missing trip`() = truncatedPatternTest {
        val fullPattern = typical(0, "a", "b", "c", "d", "e")
        val notEarly =
            objects.routePattern(route) {
                directionId = 0
                typicality = RoutePattern.Typicality.Diversion
            }

        assertFalse(notEarly.isTruncationOf(fullPattern, "a"))
    }

    @Test
    fun `isTruncationOf does not crash on missing stop IDs`() = truncatedPatternTest {
        val fullPattern = typical(0, "a", "b", "c", "d", "e")
        val notEarly =
            objects.routePattern(route) {
                directionId = 0
                typicality = RoutePattern.Typicality.Diversion
                representativeTrip()
            }

        assertFalse(notEarly.isTruncationOf(fullPattern, "a"))
    }

    @Test
    fun `isTruncationOf rejects missing current stop`() = truncatedPatternTest {
        val fullPattern = typical(0, "a", "b", "c", "d", "e")
        val early = diversion(0, "a", "b")
        val late = diversion(0, "d", "e")

        assertFalse(early.isTruncationOf(fullPattern, "e"))
        assertFalse(late.isTruncationOf(fullPattern, "a"))
    }

    @Test
    fun `isTruncationOf rejects not subsequence`() = truncatedPatternTest {
        val fullPattern = typical(0, "a", "b", "c", "d", "e")
        val tooEarly = diversion(0, "z", "a", "b")
        val tooLate = diversion(0, "d", "e", "f")
        val earlyBranch = diversion(0, "a", "b", "g")
        val lateBranch = diversion(0, "h", "d", "e")
        val earlyReversed = diversion(0, "b", "a")
        val lateReversed = diversion(0, "e", "d")

        assertFalse(tooEarly.isTruncationOf(fullPattern, "a"))
        assertFalse(tooLate.isTruncationOf(fullPattern, "e"))
        assertFalse(earlyBranch.isTruncationOf(fullPattern, "a"))
        assertFalse(lateBranch.isTruncationOf(fullPattern, "e"))
        assertFalse(earlyReversed.isTruncationOf(fullPattern, "a"))
        assertFalse(lateReversed.isTruncationOf(fullPattern, "e"))
    }

    @Test
    fun `truncatedPattern uses single if available`() = truncatedPatternTest {
        val fullPattern = typical(0, "a", "b", "c", "d", "e")
        val early = diversion(0, "a", "b")
        val late = diversion(0, "d", "e")

        assertEquals(early, truncatedPattern(fullPattern, "a"))
        assertEquals(late, truncatedPattern(fullPattern, "e"))
    }

    @Test
    fun `truncatedPattern checks schedule if multiple available`() = truncatedPatternTest {
        val fullPattern = typical(0, "a", "b", "c", "d", "e")
        val earlyShort = diversion(0, "a", "b")
        diversion(0, "a", "b", "c")
        schedule(earlyShort)

        assertEquals(earlyShort, truncatedPattern(fullPattern, "a"))
    }

    @Test
    fun `truncatedPattern gives up if multiple scheduled`() = truncatedPatternTest {
        val fullPattern = typical(0, "a", "b", "c", "d", "e")
        val earlyShort = diversion(0, "a", "b")
        val earlyLong = diversion(0, "a", "b", "c")
        schedule(earlyShort)
        schedule(earlyLong)

        assertNull(truncatedPattern(fullPattern, "a"))
    }

    @Test
    fun `truncatedPattern gives up if none available`() = truncatedPatternTest {
        val fullPattern = typical(0, "a", "b", "c", "d", "e")

        assertNull(truncatedPattern(fullPattern, "a"))
    }

    private class TruncatePatternsAtStopTestHelper {
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

        fun stopPatterns(block: NearbyStaticDataBuilder.() -> Unit): NearbyStaticData.StopPatterns {
            val staticData = NearbyStaticData.build(block)
            return staticData.data.single().patternsByStop.single()
        }

        val rewriter by lazy {
            TemporaryTerminalRewriter(
                NearbyStaticData(emptyList()),
                PredictionsStreamDataResponse(objects),
                GlobalResponse(objects, emptyMap()),
                emptyList(),
                ScheduleResponse(objects)
            )
        }
    }

    private fun truncatePatternsAtStopTest(block: TruncatePatternsAtStopTestHelper.() -> Unit) =
        TruncatePatternsAtStopTestHelper().block()

    @Test
    fun `truncatePatternsAtStop handles easy case`() = truncatePatternsAtStopTest {
        val typical = typical(route, 0, "a", "b", "c", "d", "e")
        val early = diversion(route, 0, "a", "b")

        val original = stopPatterns {
            route(route) {
                stop(stop("a")) {
                    headsign("e", listOf(typical))
                    headsign("b", listOf(early))
                }
            }
        }

        val expected = stopPatterns {
            route(route) { stop(stop("a")) { headsign("b", listOf(typical, early)) } }
        }

        assertEquals(expected, rewriter.truncatePatternsAtStop(original))
        assertEquals(
            mapOf(Pair(typical.id, "a") to early.id),
            rewriter.truncatedPatternByFullPatternAndStop.toMap()
        )
    }

    @Test
    fun `truncatePatternsAtStop handles Red Line`() = truncatePatternsAtStopTest {
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
                    headsign("Kendall/MIT", listOf(typicalBSouth, typicalASouth, divSSouth))
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
                    headsign("JFK/UMass", listOf(divANorth, divBNorth))
                }
            }
        }

        assertEquals(expectedHarvard, rewriter.truncatePatternsAtStop(originalHarvard))
        assertEquals(expectedJFK, rewriter.truncatePatternsAtStop(originalJFK))
        assertEquals(
            mapOf(
                Pair(typicalBSouth.id, "Ha0") to divSSouth.id,
                Pair(typicalASouth.id, "Ha0") to divSSouth.id,
                Pair(typicalBNorth.id, "Ha1") to divSNorth.id,
                Pair(typicalANorth.id, "Ha1") to divSNorth.id,
                Pair(typicalBSouth.id, "Jf01") to divBSouth.id,
                Pair(typicalASouth.id, "Jf00") to divASouth.id,
                Pair(typicalBNorth.id, "Jf11") to divBNorth.id,
                Pair(typicalANorth.id, "Jf10") to divANorth.id
            ),
            rewriter.truncatedPatternByFullPatternAndStop.toMap()
        )
    }
}
