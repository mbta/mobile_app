package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.parametric.ParametricTest
import com.mbta.tid.mbta_app.parametric.parametricTest
import io.github.dellisd.spatialk.geojson.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class RealtimePatternsTest {
    // trip details doesn't use RealtimePatterns
    private fun ParametricTest.anyContext() =
        anyEnumValueExcept(TripInstantDisplay.Context.TripDetails)

    private fun ParametricTest.anyNonScheduleBasedRouteType() =
        anyEnumValueExcept(RouteType.COMMUTER_RAIL, RouteType.FERRY)

    @Test
    fun `formats as alert with no trips and major alert`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { id = "Red" }

        val alert = objects.alert { effect = Alert.Effect.Suspension }

        assertEquals(
            UpcomingFormat.Disruption(alert, "alert-large-red-suspension"),
            RealtimePatterns.ByHeadsign(route, "", null, emptyList(), emptyList(), listOf(alert))
                .format(now, anyNonScheduleBasedRouteType(), anyContext())
        )
    }

    @Test
    fun `formats as ended with no trips but schedules and secondary alert`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()

        val context = anyContext()

        val cases =
            mapOf(
                objects.route { id = "Red" } to "alert-large-red-issue",
                objects.route { id = "Mattapan" } to "alert-large-mattapan-issue",
                objects.route { id = "Orange" } to "alert-large-orange-issue",
                objects.route { id = "Green-B" } to "alert-large-green-issue",
                objects.route { id = "Blue" } to "alert-large-blue-issue",
                objects.route { id = "741" } to "alert-large-silver-issue",
                objects.route { type = RouteType.COMMUTER_RAIL } to "alert-large-commuter-issue",
                objects.route { type = RouteType.FERRY } to "alert-large-ferry-issue",
                objects.route { type = RouteType.BUS } to "alert-large-bus-issue",
                objects.route { type = RouteType.HEAVY_RAIL } to "alert-borderless-issue"
            )

        val alert = objects.alert { effect = Alert.Effect.ServiceChange }

        for ((route, icon) in cases) {
            assertEquals(
                UpcomingFormat.NoTrips(
                    UpcomingFormat.NoTripsFormat.ServiceEndedToday,
                    UpcomingFormat.SecondaryAlert(icon)
                ),
                RealtimePatterns.ByHeadsign(
                        route,
                        "",
                        null,
                        emptyList(),
                        emptyList(),
                        listOf(alert),
                        hasSchedulesToday = true
                    )
                    .format(now, route.type, context)
            )
        }
    }

    @Test
    fun `formats as alert with trip and major alert`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { id = "743" }

        val trip = objects.trip()
        val prediction =
            objects.prediction {
                this.trip = trip
                departureTime = now + 1.minutes
            }
        val upcomingTrip = objects.upcomingTrip(prediction)

        val alert = objects.alert { effect = Alert.Effect.Suspension }

        assertEquals(
            UpcomingFormat.Disruption(alert, "alert-large-silver-suspension"),
            RealtimePatterns.ByHeadsign(
                    route,
                    "",
                    null,
                    emptyList(),
                    listOf(upcomingTrip),
                    listOf(alert)
                )
                .format(now, anyNonScheduleBasedRouteType(), anyContext())
        )
    }

    @Test
    fun `preserves trip alongside secondary alert`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.BUS }

        val trip = objects.trip()
        val prediction =
            objects.prediction {
                this.trip = trip
                departureTime = now + 1.minutes
            }
        val upcomingTrip = objects.upcomingTrip(prediction)

        val alert = objects.alert { effect = Alert.Effect.ServiceChange }

        assertEquals(
            UpcomingFormat.Some(
                listOf(
                    UpcomingFormat.Some.FormattedTrip(
                        trip.id,
                        route.type,
                        TripInstantDisplay.Minutes(1)
                    )
                ),
                UpcomingFormat.SecondaryAlert("alert-large-bus-issue")
            ),
            RealtimePatterns.ByHeadsign(
                    route,
                    "",
                    null,
                    emptyList(),
                    listOf(upcomingTrip),
                    listOf(alert)
                )
                .format(now, route.type, anyContext())
        )
    }

    @Test
    fun `includes downstream alert as secondary alert`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.BUS }

        val trip = objects.trip()
        val prediction =
            objects.prediction {
                this.trip = trip
                departureTime = now + 1.minutes
            }
        val upcomingTrip = objects.upcomingTrip(prediction)

        val alert = objects.alert { effect = Alert.Effect.Shuttle }

        assertEquals(
            UpcomingFormat.Some(
                listOf(
                    UpcomingFormat.Some.FormattedTrip(
                        trip.id,
                        route.type,
                        TripInstantDisplay.Minutes(1)
                    )
                ),
                UpcomingFormat.SecondaryAlert("alert-large-bus-issue")
            ),
            RealtimePatterns.ByHeadsign(
                    route,
                    "",
                    null,
                    emptyList(),
                    listOf(upcomingTrip),
                    alertsHere = emptyList(),
                    alertsDownstream = listOf(alert)
                )
                .format(now, route.type, anyContext())
        )
    }

    @Test
    fun `formats as ended with no trips but schedules and no alert`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route()

        assertEquals(
            UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.ServiceEndedToday),
            RealtimePatterns.ByHeadsign(
                    route,
                    "",
                    null,
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    hasSchedulesToday = true
                )
                .format(now, anyNonScheduleBasedRouteType(), anyContext())
        )
    }

    @Test
    fun `formats as none with subway schedules but no predictions and no alert`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = anyOf(RouteType.LIGHT_RAIL, RouteType.HEAVY_RAIL) }
        val pattern = objects.routePattern(route)
        val schedule =
            objects.schedule {
                trip = objects.trip(pattern)
                departureTime = now + 2.minutes
            }
        assertEquals(
            UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.PredictionsUnavailable),
            RealtimePatterns.ByHeadsign(
                    route,
                    "",
                    null,
                    listOf(pattern),
                    listOf(objects.upcomingTrip(schedule)),
                    emptyList(),
                    hasSchedulesToday = true
                )
                .format(now, route.type, anyContext())
        )
    }

    @Test
    fun `formats as loading if empty trips but still loading`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = anyEnumValue() }
        val pattern = objects.routePattern(route)
        assertEquals(
            UpcomingFormat.Loading,
            RealtimePatterns.ByHeadsign(
                    route,
                    "",
                    null,
                    listOf(pattern),
                    upcomingTrips = emptyList(),
                    allDataLoaded = false
                )
                .format(now, route.type, anyContext())
        )
    }

    @Test
    fun `skips trips that should be hidden`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route()

        val trip1 = objects.trip()
        val trip2 = objects.trip()

        val prediction1 =
            objects.prediction {
                trip = trip1
                departureTime = null
            }
        val prediction2 =
            objects.prediction {
                trip = trip2
                departureTime = now + 5.minutes
            }

        val upcomingTrip1 = objects.upcomingTrip(prediction1)
        val upcomingTrip2 = objects.upcomingTrip(prediction2)
        val routeType = anyNonScheduleBasedRouteType()
        assertEquals(
            UpcomingFormat.Some(
                listOf(
                    UpcomingFormat.Some.FormattedTrip(
                        trip2.id,
                        routeType,
                        TripInstantDisplay.Minutes(5)
                    )
                ),
                null
            ),
            RealtimePatterns.ByHeadsign(
                    route,
                    "",
                    null,
                    emptyList(),
                    listOf(upcomingTrip1, upcomingTrip2)
                )
                .format(now, routeType, anyContext())
        )
    }

    @Test
    fun `format skips schedules on subway but keeps on non-subway`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val subwayRoute = objects.route { type = RouteType.LIGHT_RAIL }
        val busRoute = objects.route { type = RouteType.BUS }

        val trip1 = objects.trip()
        val trip2 = objects.trip()

        val schedule1 =
            objects.schedule {
                trip = trip1
                departureTime = now + 5.minutes
            }
        val prediction2 =
            objects.prediction {
                trip = trip2
                departureTime = now + 5.minutes
            }

        val upcomingTrip1 = objects.upcomingTrip(schedule1)
        val upcomingTrip2 = objects.upcomingTrip(prediction2)

        assertEquals(
            UpcomingFormat.Some(
                listOf(
                    UpcomingFormat.Some.FormattedTrip(
                        trip2.id,
                        RouteType.LIGHT_RAIL,
                        TripInstantDisplay.Minutes(5)
                    )
                ),
                null
            ),
            RealtimePatterns.ByHeadsign(
                    subwayRoute,
                    "",
                    null,
                    emptyList(),
                    listOf(upcomingTrip1, upcomingTrip2)
                )
                .format(now, RouteType.LIGHT_RAIL, anyContext())
        )
        assertEquals(
            UpcomingFormat.Some(
                listOf(
                    UpcomingFormat.Some.FormattedTrip(
                        trip1.id,
                        RouteType.BUS,
                        TripInstantDisplay.ScheduleMinutes(5)
                    ),
                    UpcomingFormat.Some.FormattedTrip(
                        trip2.id,
                        RouteType.BUS,
                        TripInstantDisplay.Minutes(5)
                    )
                ),
                null
            ),
            RealtimePatterns.ByHeadsign(
                    busRoute,
                    "",
                    null,
                    emptyList(),
                    listOf(upcomingTrip1, upcomingTrip2)
                )
                .format(now, RouteType.BUS, anyContext())
        )
    }

    @Test
    fun `format handles no schedules all day`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route { type = RouteType.BUS }

        assertEquals(
            UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.NoSchedulesToday),
            RealtimePatterns.ByHeadsign(
                    route,
                    "",
                    null,
                    emptyList(),
                    listOf(),
                    hasSchedulesToday = false
                )
                .format(now, RouteType.BUS, anyContext())
        )
    }

    @Test
    fun `hasMajorAlerts is true when a major alert is active`() = parametricTest {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()

        val majorAlert = objects.alert { effect = Alert.Effect.Suspension }
        val minorAlert = objects.alert { effect = Alert.Effect.FacilityIssue }

        assertTrue(
            RealtimePatterns.ByHeadsign(
                    route,
                    "",
                    null,
                    emptyList(),
                    emptyList(),
                    listOf(majorAlert)
                )
                .hasMajorAlerts
        )
        assertFalse(
            RealtimePatterns.ByHeadsign(
                    route,
                    "",
                    null,
                    emptyList(),
                    emptyList(),
                    listOf(minorAlert)
                )
                .hasMajorAlerts
        )
    }

    @Test
    fun `hasSchedulesToday returns true when schedules exist or have not loaded`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val patternA = objects.routePattern(route)
        val patternB = objects.routePattern(route)
        assertTrue(RealtimePatterns.hasSchedulesToday(null, listOf(patternA, patternB)))
        val hasSchedulesByPattern = mapOf(Pair(patternA.id, true))
        assertTrue(
            RealtimePatterns.hasSchedulesToday(hasSchedulesByPattern, listOf(patternA, patternB))
        )
        assertFalse(RealtimePatterns.hasSchedulesToday(hasSchedulesByPattern, listOf(patternB)))
    }

    @Test
    fun `directionId finds trips`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val trip0 = objects.trip { directionId = 0 }
        val prediction0 = objects.schedule { trip = trip0 }
        val trip1 = objects.trip { directionId = 1 }
        val prediction1 = objects.schedule { trip = trip1 }
        assertEquals(
            0,
            RealtimePatterns.ByHeadsign(
                    route,
                    "",
                    null,
                    emptyList(),
                    listOf(objects.upcomingTrip(prediction0))
                )
                .directionId()
        )
        assertEquals(
            1,
            RealtimePatterns.ByHeadsign(
                    route,
                    "",
                    null,
                    emptyList(),
                    listOf(objects.upcomingTrip(prediction1))
                )
                .directionId()
        )
    }

    @Test
    fun `directionId finds patterns`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val routePattern0 = objects.routePattern(route) { directionId = 0 }
        val routePattern1 = objects.routePattern(route) { directionId = 1 }
        assertEquals(
            0,
            RealtimePatterns.ByHeadsign(route, "", null, listOf(routePattern0), emptyList())
                .directionId()
        )
        assertEquals(
            1,
            RealtimePatterns.ByHeadsign(route, "", null, listOf(routePattern1), emptyList())
                .directionId()
        )
    }

    @Test
    fun `directionId throws if empty`() {
        assertFailsWith<NoSuchElementException> {
            RealtimePatterns.ByHeadsign(
                    ObjectCollectionBuilder.Single.route(),
                    "",
                    null,
                    emptyList(),
                    emptyList()
                )
                .directionId()
        }
    }

    @Test
    fun `predictions grouped by direction are displayed`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val line = objects.line {}
        val route1 = objects.route()
        val route2 = objects.route()
        val route3 = objects.route()

        val trip1 = objects.trip { routeId = route1.id }
        val trip2 = objects.trip { routeId = route2.id }
        val trip3 = objects.trip { routeId = route3.id }
        val trip4 = objects.trip { routeId = route1.id }

        val prediction1 =
            objects.prediction {
                trip = trip1
                departureTime = now + 3.minutes
            }
        val prediction2 =
            objects.prediction {
                trip = trip2
                departureTime = now + 5.minutes
            }
        val prediction3 =
            objects.prediction {
                trip = trip3
                departureTime = now + 7.minutes
            }
        val prediction4 =
            objects.prediction {
                trip = trip4
                departureTime = now + 9.minutes
            }

        val upcomingTrip1 = objects.upcomingTrip(prediction1)
        val upcomingTrip2 = objects.upcomingTrip(prediction2)
        val upcomingTrip3 = objects.upcomingTrip(prediction3)
        val upcomingTrip4 = objects.upcomingTrip(prediction4)

        val directionPatterns =
            RealtimePatterns.ByDirection(
                line,
                routes = listOf(route1, route2, route3),
                direction = Direction("", "", 0),
                emptyList(),
                listOf(upcomingTrip1, upcomingTrip2, upcomingTrip3, upcomingTrip4)
            )

        val routeType = anyNonScheduleBasedRouteType()
        assertEquals(
            UpcomingFormat.Some(
                listOf(
                    UpcomingFormat.Some.FormattedTrip(
                        trip1.id,
                        routeType,
                        TripInstantDisplay.Minutes(3)
                    ),
                    UpcomingFormat.Some.FormattedTrip(
                        trip2.id,
                        routeType,
                        TripInstantDisplay.Minutes(5)
                    ),
                    UpcomingFormat.Some.FormattedTrip(
                        trip3.id,
                        routeType,
                        TripInstantDisplay.Minutes(7)
                    )
                ),
                null
            ),
            directionPatterns.format(now, routeType, anyContext())
        )

        assertEquals(directionPatterns.routesByTrip[trip2.id], route2)
    }

    @Test
    fun `properly applies platform alerts by pattern`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        lateinit var platform1: Stop
        lateinit var platform2: Stop
        val station =
            objects.stop {
                platform1 = childStop()
                platform2 = childStop()
            }

        val route = objects.route()
        val pattern1 =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                sortOrder = 1
                representativeTrip { headsign = "A" }
            }
        val pattern2 =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                sortOrder = 2
                representativeTrip { headsign = "B" }
            }

        val alert =
            objects.alert {
                activePeriod(Instant.DISTANT_PAST, null)
                effect = Alert.Effect.Suspension
                informedEntity(
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride
                    ),
                    route = route.id,
                    stop = platform2.id
                )
            }

        val static =
            NearbyStaticData.build {
                route(route) {
                    stop(station) {
                        headsign("A", listOf(pattern1), setOf(platform1.id))
                        headsign("B", listOf(pattern2), setOf(platform2.id))
                    }
                }
            }

        val actual =
            static.withRealtimeInfo(
                GlobalResponse(objects, emptyMap()),
                Position(0.0, 0.0),
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                Clock.System.now(),
                emptySet(),
            )

        assertEquals(
            listOf(
                StopsAssociated.WithRoute(
                    route,
                    listOf(
                        PatternsByStop(
                            route,
                            station,
                            listOf(
                                // since B has an alert, it sorts above A
                                RealtimePatterns.ByHeadsign(
                                    route,
                                    "B",
                                    null,
                                    listOf(pattern2),
                                    emptyList(),
                                    listOf(alert),
                                    emptyList(),
                                    false
                                ),
                                RealtimePatterns.ByHeadsign(
                                    route,
                                    "A",
                                    null,
                                    listOf(pattern1),
                                    emptyList(),
                                    emptyList(),
                                    emptyList(),
                                    false
                                )
                            )
                        )
                    )
                )
            ),
            actual
        )
    }

    @Test
    fun `handles logical vs physical platforms`() = runBlocking {
        // at Union Sq, North/South Station, and some others, the platforms don't map one-to-one to
        // the directions, and the schedules are by direction but the predictions are by physical
        // platform
        val objects = ObjectCollectionBuilder()
        lateinit var logicalPlatform: Stop
        lateinit var physicalPlatform: Stop
        val station =
            objects.stop {
                logicalPlatform = childStop()
                physicalPlatform = childStop()
            }

        val route = objects.route()
        val pattern =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                sortOrder = 1
                representativeTrip {
                    headsign = "A"
                    stopIds = listOf(logicalPlatform.id)
                }
            }

        val static =
            NearbyStaticData.build {
                route(route) {
                    stop(station) { headsign("A", listOf(pattern), setOf(logicalPlatform.id)) }
                }
            }

        val now = Clock.System.now()

        val schedule =
            objects.schedule {
                trip = objects.trip(pattern)
                stopId = logicalPlatform.id
                departureTime = now + 5.minutes
            }

        val prediction =
            objects.prediction(schedule) {
                stopId = physicalPlatform.id
                departureTime = now + 5.minutes
            }

        val actual =
            static.withRealtimeInfo(
                GlobalResponse(objects, emptyMap()),
                Position(0.0, 0.0),
                ScheduleResponse(objects),
                PredictionsStreamDataResponse(objects),
                AlertsStreamDataResponse(objects),
                now,
                emptySet(),
            )

        assertEquals(
            listOf(
                StopsAssociated.WithRoute(
                    route,
                    listOf(
                        PatternsByStop(
                            route,
                            station,
                            listOf(
                                RealtimePatterns.ByHeadsign(
                                    route,
                                    "A",
                                    null,
                                    listOf(pattern),
                                    listOf(objects.upcomingTrip(schedule, prediction)),
                                    emptyList()
                                )
                            )
                        )
                    )
                )
            ),
            actual
        )
    }

    @Test
    fun `includes cancelled trips`() = parametricTest {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()
        val route = objects.route()

        val trip1 = objects.trip()
        val trip2 = objects.trip()

        val schedule1 =
            objects.schedule {
                trip = trip1
                departureTime = now + 2.minutes
            }
        val prediction1 =
            objects.prediction {
                trip = trip1
                departureTime = null
                scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
            }
        val prediction2 =
            objects.prediction {
                trip = trip2
                departureTime = now + 5.minutes
            }

        val upcomingTrip1 = objects.upcomingTrip(schedule1, prediction1)
        val upcomingTrip2 = objects.upcomingTrip(prediction2)
        val routeType = RouteType.BUS
        assertEquals(
            UpcomingFormat.Some(
                listOf(
                    UpcomingFormat.Some.FormattedTrip(
                        trip1.id,
                        routeType,
                        TripInstantDisplay.Cancelled(schedule1.departureTime!!)
                    ),
                    UpcomingFormat.Some.FormattedTrip(
                        trip2.id,
                        routeType,
                        TripInstantDisplay.Minutes(5)
                    )
                ),
                null
            ),
            RealtimePatterns.ByHeadsign(
                    route,
                    "",
                    null,
                    emptyList(),
                    listOf(upcomingTrip1, upcomingTrip2)
                )
                .format(now, routeType, TripInstantDisplay.Context.StopDetailsFiltered)
        )
    }

    @Test
    fun `filterCancellations filters if 2 or less`() {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()

        val trip1 = objects.trip()
        val trip2 = objects.trip()

        val schedule1 =
            objects.schedule {
                trip = trip1
                departureTime = now + 2.minutes
            }
        val prediction1 =
            objects.prediction {
                trip = trip1
                departureTime = null
                scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
            }
        val prediction2 =
            objects.prediction {
                trip = trip2
                departureTime = now + 5.minutes
            }

        val upcomingTrip1 = objects.upcomingTrip(schedule1, prediction1)
        val upcomingTrip2 = objects.upcomingTrip(prediction2)
        val sut = listOf(upcomingTrip1, upcomingTrip2).filterCancellations(false)
        assertEquals(sut, listOf(upcomingTrip2))
    }

    @Test
    fun `filterCancellations filters subway cancellations`() {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()

        val trip1 = objects.trip()
        val trip2 = objects.trip()
        val trip3 = objects.trip()

        val schedule1 =
            objects.schedule {
                trip = trip1
                departureTime = now + 2.minutes
            }
        val prediction1 =
            objects.prediction {
                trip = trip1
                departureTime = null
                scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
            }
        val prediction2 =
            objects.prediction {
                trip = trip2
                departureTime = now + 5.minutes
            }
        val prediction3 =
            objects.prediction {
                trip = trip3
                departureTime = now + 7.minutes
            }

        val upcomingTrip1 = objects.upcomingTrip(schedule1, prediction1)
        val upcomingTrip2 = objects.upcomingTrip(prediction2)
        val upcomingTrip3 = objects.upcomingTrip(prediction3)
        val sut = listOf(upcomingTrip1, upcomingTrip2, upcomingTrip3).filterCancellations(true)
        assertEquals(sut, listOf(upcomingTrip2, upcomingTrip3))
    }

    @Test
    fun `include cancellations if not subway`() {
        val now = Clock.System.now()

        val objects = ObjectCollectionBuilder()

        val trip1 = objects.trip()
        val trip2 = objects.trip()
        val trip3 = objects.trip()

        val schedule1 =
            objects.schedule {
                trip = trip1
                departureTime = now + 2.minutes
            }
        val prediction1 =
            objects.prediction {
                trip = trip1
                departureTime = null
                scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
            }
        val prediction2 =
            objects.prediction {
                trip = trip2
                departureTime = now + 5.minutes
            }
        val prediction3 =
            objects.prediction {
                trip = trip3
                departureTime = now + 7.minutes
            }

        val upcomingTrip1 = objects.upcomingTrip(schedule1, prediction1)
        val upcomingTrip2 = objects.upcomingTrip(prediction2)
        val upcomingTrip3 = objects.upcomingTrip(prediction3)
        val sut = listOf(upcomingTrip1, upcomingTrip2, upcomingTrip3).filterCancellations(false)
        assertEquals(sut, listOf(upcomingTrip1, upcomingTrip2, upcomingTrip3))
    }
}
