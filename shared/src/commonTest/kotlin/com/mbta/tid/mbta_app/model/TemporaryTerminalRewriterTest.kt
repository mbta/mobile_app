package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Unit tests for the individual helper functions defined in [TemporaryTerminalRewriter]. */
class TemporaryTerminalRewriterTest {
    private val boardExitRide =
        listOf(
            Alert.InformedEntity.Activity.Board,
            Alert.InformedEntity.Activity.Exit,
            Alert.InformedEntity.Activity.Ride
        )

    @Test
    fun `appliesToRoute accepts non-branching suspension`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.HEAVY_RAIL }
        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(activities = boardExitRide, route = route.id)
            }
        val typical = objects.routePattern(route) { typicality = RoutePattern.Typicality.Typical }
        val diversion =
            objects.routePattern(route) { typicality = RoutePattern.Typicality.Diversion }
        objects.schedule { trip = objects.trip(diversion) }
        objects.prediction { trip = objects.trip(typical) }

        val rewriter =
            TemporaryTerminalRewriter(
                NearbyStaticData(emptyList()),
                PredictionsStreamDataResponse(objects),
                GlobalResponse(objects, emptyMap()),
                listOf(alert),
                ScheduleResponse(objects)
            )

        assertTrue(rewriter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects bus`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.BUS }
        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(activities = boardExitRide, route = route.id)
            }
        val typical = objects.routePattern(route) { typicality = RoutePattern.Typicality.Typical }
        val diversion =
            objects.routePattern(route) { typicality = RoutePattern.Typicality.Diversion }
        objects.schedule { trip = objects.trip(diversion) }
        objects.prediction { trip = objects.trip(typical) }

        val rewriter =
            TemporaryTerminalRewriter(
                NearbyStaticData(emptyList()),
                PredictionsStreamDataResponse(objects),
                GlobalResponse(objects, emptyMap()),
                listOf(alert),
                ScheduleResponse(objects)
            )

        assertFalse(rewriter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects insufficient alert effect`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.HEAVY_RAIL }
        val alert =
            objects.alert {
                effect = Alert.Effect.StationClosure
                informedEntity(activities = boardExitRide, route = route.id)
            }
        val typical = objects.routePattern(route) { typicality = RoutePattern.Typicality.Typical }
        val diversion =
            objects.routePattern(route) { typicality = RoutePattern.Typicality.Diversion }
        objects.schedule { trip = objects.trip(diversion) }
        objects.prediction { trip = objects.trip(typical) }

        val rewriter =
            TemporaryTerminalRewriter(
                NearbyStaticData(emptyList()),
                PredictionsStreamDataResponse(objects),
                GlobalResponse(objects, emptyMap()),
                listOf(alert),
                ScheduleResponse(objects)
            )

        assertFalse(rewriter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects wrong route`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.HEAVY_RAIL }
        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(activities = boardExitRide, route = "not-${route.id}")
            }
        val typical = objects.routePattern(route) { typicality = RoutePattern.Typicality.Typical }
        val diversion =
            objects.routePattern(route) { typicality = RoutePattern.Typicality.Diversion }
        objects.schedule { trip = objects.trip(diversion) }
        objects.prediction { trip = objects.trip(typical) }

        val rewriter =
            TemporaryTerminalRewriter(
                NearbyStaticData(emptyList()),
                PredictionsStreamDataResponse(objects),
                GlobalResponse(objects, emptyMap()),
                listOf(alert),
                ScheduleResponse(objects)
            )

        assertFalse(rewriter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects without non-typical schedule`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.HEAVY_RAIL }
        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(activities = boardExitRide, route = route.id)
            }
        val typical = objects.routePattern(route) { typicality = RoutePattern.Typicality.Typical }
        objects.routePattern(route) { typicality = RoutePattern.Typicality.Diversion }
        objects.schedule { trip = objects.trip(typical) }
        objects.prediction { trip = objects.trip(typical) }

        val rewriter =
            TemporaryTerminalRewriter(
                NearbyStaticData(emptyList()),
                PredictionsStreamDataResponse(objects),
                GlobalResponse(objects, emptyMap()),
                listOf(alert),
                ScheduleResponse(objects)
            )

        assertFalse(rewriter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects without missing typical schedule`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.HEAVY_RAIL }
        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(activities = boardExitRide, route = route.id)
            }
        val typical = objects.routePattern(route) { typicality = RoutePattern.Typicality.Typical }
        val diversion =
            objects.routePattern(route) { typicality = RoutePattern.Typicality.Diversion }
        objects.schedule { trip = objects.trip(diversion) }
        objects.schedule { trip = objects.trip(typical) }
        objects.prediction { trip = objects.trip(typical) }

        val rewriter =
            TemporaryTerminalRewriter(
                NearbyStaticData(emptyList()),
                PredictionsStreamDataResponse(objects),
                GlobalResponse(objects, emptyMap()),
                listOf(alert),
                ScheduleResponse(objects)
            )

        assertFalse(rewriter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute accepts branching suspension`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.HEAVY_RAIL }
        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(activities = boardExitRide, route = route.id)
            }
        val typicalA = objects.routePattern(route) { typicality = RoutePattern.Typicality.Typical }
        val typicalB = objects.routePattern(route) { typicality = RoutePattern.Typicality.Typical }
        val diversionA =
            objects.routePattern(route) { typicality = RoutePattern.Typicality.Diversion }
        objects.schedule { trip = objects.trip(diversionA) }
        objects.schedule { trip = objects.trip(typicalB) }
        objects.prediction { trip = objects.trip(typicalA) }

        val rewriter =
            TemporaryTerminalRewriter(
                NearbyStaticData(emptyList()),
                PredictionsStreamDataResponse(objects),
                GlobalResponse(objects, emptyMap()),
                listOf(alert),
                ScheduleResponse(objects)
            )

        assertTrue(rewriter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects no schedules`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.HEAVY_RAIL }
        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(activities = boardExitRide, route = route.id)
            }
        val typical = objects.routePattern(route) { typicality = RoutePattern.Typicality.Typical }
        objects.routePattern(route) { typicality = RoutePattern.Typicality.Diversion }
        objects.prediction { trip = objects.trip(typical) }

        val rewriter =
            TemporaryTerminalRewriter(
                NearbyStaticData(emptyList()),
                PredictionsStreamDataResponse(objects),
                GlobalResponse(objects, emptyMap()),
                listOf(alert),
                ScheduleResponse(objects)
            )

        assertFalse(rewriter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects no predictions`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.HEAVY_RAIL }
        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(activities = boardExitRide, route = route.id)
            }
        objects.routePattern(route) { typicality = RoutePattern.Typicality.Typical }
        val diversion =
            objects.routePattern(route) { typicality = RoutePattern.Typicality.Diversion }
        objects.schedule { trip = objects.trip(diversion) }

        val rewriter =
            TemporaryTerminalRewriter(
                NearbyStaticData(emptyList()),
                PredictionsStreamDataResponse(objects),
                GlobalResponse(objects, emptyMap()),
                listOf(alert),
                ScheduleResponse(objects)
            )

        assertFalse(rewriter.appliesToRoute(route))
    }

    @Test
    fun `appliesToRoute rejects with diversion prediction`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.HEAVY_RAIL }
        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                informedEntity(activities = boardExitRide, route = route.id)
            }
        val typical = objects.routePattern(route) { typicality = RoutePattern.Typicality.Typical }
        val diversion =
            objects.routePattern(route) { typicality = RoutePattern.Typicality.Diversion }
        objects.schedule { trip = objects.trip(diversion) }
        objects.prediction { trip = objects.trip(typical) }
        objects.prediction { trip = objects.trip(diversion) }

        val rewriter =
            TemporaryTerminalRewriter(
                NearbyStaticData(emptyList()),
                PredictionsStreamDataResponse(objects),
                GlobalResponse(objects, emptyMap()),
                listOf(alert),
                ScheduleResponse(objects)
            )

        assertFalse(rewriter.appliesToRoute(route))
    }
}
