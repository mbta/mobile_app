package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import kotlin.test.Test
import kotlin.test.assertFalse
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
}
