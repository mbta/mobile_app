package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.TestData
import junit.framework.TestCase.assertTrue
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import org.junit.Rule
import org.junit.Test

class DeparturesTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testDepartures() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()

        val downstreamAlert = objects.alert { effect = Alert.Effect.Shuttle }
        val stop = objects.stop {}
        val route =
            objects.route {
                longName = "Route"
                type = RouteType.LIGHT_RAIL
            }

        val aTrip = objects.trip { headsign = "A" }
        val bTrip = objects.trip { headsign = "B" }

        val lineOrRoute = LineOrRoute.Route(route)
        val context = RouteCardData.Context.NearbyTransit
        val stopData =
            RouteCardData.RouteStopData(
                lineOrRoute,
                stop,
                listOf(Direction("A Headsign", null, 0), Direction("B Headsign", null, 1)),
                listOf(
                    RouteCardData.Leaf(
                        lineOrRoute,
                        stop,
                        0,
                        listOf(objects.routePattern(route) {}),
                        setOf(stop.id),
                        listOf(
                            UpcomingTrip(
                                aTrip,
                                objects.prediction { departureTime = now.plus(5.minutes) },
                            )
                        ),
                        emptyList(),
                        true,
                        true,
                        emptyList(),
                        context,
                    ),
                    RouteCardData.Leaf(
                        lineOrRoute,
                        stop,
                        1,
                        listOf(objects.routePattern(route) {}),
                        setOf(stop.id),
                        listOf(
                            UpcomingTrip(
                                bTrip,
                                objects.prediction { departureTime = now.plus(20.minutes) },
                            )
                        ),
                        emptyList(),
                        true,
                        true,
                        listOf(downstreamAlert),
                        context,
                    ),
                ),
            )

        composeTestRule.setContent {
            Departures(stopData, GlobalResponse(objects), now, { false }) { _ -> }
        }

        composeTestRule.onNodeWithText("5 min").assertIsDisplayed()
        composeTestRule.onNodeWithText(aTrip.headsign).assertIsDisplayed()

        composeTestRule.onNodeWithText("20 min").assertIsDisplayed()
        composeTestRule.onNodeWithText(bTrip.headsign).assertIsDisplayed()
        composeTestRule.onNodeWithText(bTrip.headsign).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Alert").assertIsDisplayed()
    }

    @Test
    fun testStopHeadsign() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route =
            objects.route {
                longName = "Route"
                type = RouteType.LIGHT_RAIL
            }

        val aTrip = objects.trip { headsign = "A" }
        val aSchedule = objects.schedule { stopHeadsign = "A Stop Headsign" }
        val bTrip = objects.trip { headsign = "B" }

        val lineOrRoute = LineOrRoute.Route(route)
        val context = RouteCardData.Context.NearbyTransit
        val stopData =
            RouteCardData.RouteStopData(
                lineOrRoute,
                stop,
                listOf(Direction("A Headsign", null, 0), Direction("B Headsign", null, 1)),
                listOf(
                    RouteCardData.Leaf(
                        lineOrRoute,
                        stop,
                        0,
                        listOf(objects.routePattern(route) {}),
                        setOf(stop.id),
                        listOf(
                            UpcomingTrip(
                                aTrip,
                                aSchedule,
                                objects.prediction { departureTime = now.plus(5.minutes) },
                            )
                        ),
                        emptyList(),
                        true,
                        true,
                        emptyList(),
                        context,
                    ),
                    RouteCardData.Leaf(
                        lineOrRoute,
                        stop,
                        1,
                        listOf(objects.routePattern(route) {}),
                        setOf(stop.id),
                        listOf(
                            UpcomingTrip(
                                bTrip,
                                objects.prediction { departureTime = now.plus(20.minutes) },
                            )
                        ),
                        emptyList(),
                        true,
                        true,
                        emptyList(),
                        context,
                    ),
                ),
            )

        composeTestRule.setContent {
            Departures(stopData, GlobalResponse(objects), now, { false }) { _ -> }
        }

        composeTestRule.onNodeWithText(aTrip.headsign).assertDoesNotExist()
        composeTestRule.onNodeWithText(aSchedule.stopHeadsign!!).assertIsDisplayed()
        composeTestRule.onNodeWithText(bTrip.headsign).assertIsDisplayed()
    }

    @Test
    fun testSinglePill() {
        val now = EasternTimeInstant.now()
        val objects = TestData.clone()

        val stop = objects.getStop("place-rsmnl")
        val line = objects.getLine("line-Green")
        val route = objects.getRoute("Green-D")
        val routePattern = objects.getRoutePattern("Green-D-855-0")

        val trip =
            objects.upcomingTrip(
                objects.prediction {
                    trip = objects.trip(routePattern)
                    departureTime = now + 5.minutes
                }
            )

        val lineOrRoute = LineOrRoute.Line(line, setOf(route))
        val context = RouteCardData.Context.NearbyTransit
        val stopData =
            RouteCardData.RouteStopData(
                lineOrRoute,
                stop,
                listOf(Direction("West", "Riverside", 0), Direction("East", "Park St & North", 1)),
                listOf(
                    RouteCardData.Leaf(
                        lineOrRoute,
                        stop,
                        0,
                        listOf(routePattern),
                        setOf(stop.id),
                        listOf(trip),
                        emptyList(),
                        true,
                        true,
                        emptyList(),
                        context,
                    )
                ),
            )

        composeTestRule.setContent {
            Departures(stopData, GlobalResponse(objects), now, { false }) { _ -> }
        }

        composeTestRule.onNodeWithText("D").assertIsDisplayed()
    }

    @Test
    fun testTapAnalytics() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route =
            objects.route {
                longName = "Route"
                type = RouteType.LIGHT_RAIL
            }

        val aTrip = objects.trip { headsign = "A" }
        val bTrip = objects.trip { headsign = "B" }

        val lineOrRoute = LineOrRoute.Route(route)
        val context = RouteCardData.Context.NearbyTransit
        val stopData =
            RouteCardData.RouteStopData(
                lineOrRoute,
                stop,
                listOf(Direction("A Headsign", null, 0), Direction("B Headsign", null, 1)),
                listOf(
                    RouteCardData.Leaf(
                        lineOrRoute,
                        stop,
                        0,
                        listOf(objects.routePattern(route) {}),
                        setOf(stop.id),
                        listOf(
                            UpcomingTrip(
                                aTrip,
                                objects.prediction { departureTime = now.plus(5.minutes) },
                            )
                        ),
                        emptyList(),
                        true,
                        true,
                        emptyList(),
                        context,
                    ),
                    RouteCardData.Leaf(
                        lineOrRoute,
                        stop,
                        1,
                        listOf(objects.routePattern(route) {}),
                        setOf(stop.id),
                        listOf(
                            UpcomingTrip(
                                bTrip,
                                objects.prediction { departureTime = now.plus(20.minutes) },
                            )
                        ),
                        emptyList(),
                        true,
                        true,
                        emptyList(),
                        context,
                    ),
                ),
            )

        var tapAnalytics: Pair<String, Map<String, String>>? = null
        var onClickCalled = false

        loadKoinMocks(
            analytics = MockAnalytics({ event, props -> tapAnalytics = Pair(event, props) })
        )

        composeTestRule.setContent {
            Departures(stopData, GlobalResponse(objects), now, { true }) { onClickCalled = true }
        }

        composeTestRule.onNodeWithText(aTrip.headsign).assertIsDisplayed().performClick()

        assertTrue(onClickCalled)
        assertEquals(
            tapAnalytics,
            Pair(
                "tapped_departure",
                mapOf(
                    "route_id" to route.id.idText,
                    "stop_id" to stop.id,
                    "pinned" to "true",
                    "alert" to "false",
                    "mode" to "subway",
                    "no_trips" to "",
                    "context" to "unknown",
                ),
            ),
        )
    }
}
