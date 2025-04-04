package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.UpcomingTrip
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test

class StopDetailsRouteViewTest {
    val builder = ObjectCollectionBuilder()
    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
    val route =
        builder.route {
            id = "route_1"
            type = RouteType.BUS
            color = "FF0000"
            directionNames = listOf("North", "South")
            directionDestinations = listOf("Downtown", "Uptown")
            longName = "Sample Route Long Name"
            shortName = "Sample Route"
            textColor = "000000"
            lineId = "line_1"
            routePatternIds = mutableListOf("pattern_1", "pattern_2")
        }
    val routePatternOne =
        builder.routePattern(route) {
            id = "pattern_1"
            directionId = 0
            name = "Sample Route Pattern"
            routeId = "route_1"
            representativeTripId = "trip_1"
        }
    val routePatternTwo =
        builder.routePattern(route) {
            id = "pattern_2"
            directionId = 1
            name = "Sample Route Pattern Two"
            routeId = "route_1"
            representativeTripId = "trip_1"
        }
    val stop =
        builder.stop {
            id = "stop_1"
            name = "Sample Stop"
            locationType = LocationType.STOP
            latitude = 0.0
            longitude = 0.0
        }
    val line =
        builder.line {
            id = "line_1"
            color = "FF0000"
            textColor = "FFFFFF"
        }
    val trip =
        builder.trip {
            id = "trip_1"
            routeId = "route_1"
            directionId = 0
            headsign = "Sample Headsign"
            routePatternId = "pattern_1"
        }
    val prediction =
        builder.prediction {
            id = "prediction_1"
            revenue = true
            stopId = "stop_1"
            tripId = "trip_1"
            routeId = "route_1"
            stopSequence = 1
            directionId = 0
            arrivalTime = now.plus(1.minutes)
            departureTime = now.plus(1.5.minutes)
        }

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testStopDetailsRouteViewDisplaysCorrectly() {
        composeTestRule.setContent {
            StopDetailsRouteView(
                patternsByStop =
                    PatternsByStop(
                        route,
                        stop,
                        listOf(
                            RealtimePatterns.ByHeadsign(
                                staticData =
                                    NearbyStaticData.StaticPatterns.ByHeadsign(
                                        route,
                                        trip.headsign,
                                        line,
                                        listOf(routePatternOne, routePatternTwo),
                                        stopIds = setOf(stop.id),
                                    ),
                                upcomingTripsMap =
                                    mapOf(
                                        RealtimePatterns.UpcomingTripKey.ByRoutePattern(
                                            route.id,
                                            trip.routePatternId,
                                            stop.id,
                                        ) to listOf(UpcomingTrip(trip, prediction))
                                    ),
                                parentStopId = stop.id,
                                alertsHere = emptyList(),
                                alertsDownstream = emptyList(),
                                hasSchedulesTodayByPattern = null,
                                allDataLoaded = false
                            )
                        )
                    ),
                now = now,
                pinned = false,
                onPin = {},
                updateStopFilter = {}
            )
        }

        composeTestRule.onNodeWithText("Sample Route").assertExists()
        composeTestRule.onNodeWithText("Sample Headsign").assertExists()
        composeTestRule.onNodeWithText("1 min").assertExists()
    }

    @Test
    fun testUpdatesStopFilter() {
        var updatedStopFilter: StopDetailsFilter? = null
        composeTestRule.setContent {
            StopDetailsRouteView(
                patternsByStop =
                    PatternsByStop(
                        route,
                        stop,
                        listOf(
                            RealtimePatterns.ByHeadsign(
                                staticData =
                                    NearbyStaticData.StaticPatterns.ByHeadsign(
                                        route,
                                        trip.headsign,
                                        line,
                                        listOf(routePatternOne, routePatternTwo),
                                        stopIds = setOf(stop.id),
                                    ),
                                upcomingTripsMap =
                                    mapOf(
                                        RealtimePatterns.UpcomingTripKey.ByRoutePattern(
                                            route.id,
                                            trip.routePatternId,
                                            stop.id,
                                        ) to listOf(UpcomingTrip(trip, prediction))
                                    ),
                                parentStopId = stop.id,
                                alertsHere = emptyList(),
                                alertsDownstream = emptyList(),
                                hasSchedulesTodayByPattern = null,
                                allDataLoaded = false
                            )
                        )
                    ),
                now = now,
                pinned = false,
                onPin = {},
                updateStopFilter = { updatedStopFilter = it }
            )
        }

        composeTestRule.onNodeWithText("Sample Headsign").performClick()
        assertEquals(StopDetailsFilter(route.id, trip.directionId), updatedStopFilter)
    }
}
