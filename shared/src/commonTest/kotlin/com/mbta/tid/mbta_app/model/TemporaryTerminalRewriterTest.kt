package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
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
    fun `isTruncationOf crashes on missing stop IDs`() = truncatedPatternTest {
        val fullPattern = typical(0, "a", "b", "c", "d", "e")
        val notEarly =
            objects.routePattern(route) {
                directionId = 0
                typicality = RoutePattern.Typicality.Diversion
                representativeTrip()
            }

        assertFails { notEarly.isTruncationOf(fullPattern, "a") }
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
}
