package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.UpcomingTrip
import junit.framework.TestCase.assertTrue
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext
import org.koin.dsl.koinApplication

class DeparturesTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testDepartures() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route =
            objects.route {
                longName = "Route"
                type = RouteType.LIGHT_RAIL
            }

        val aTrip = objects.trip { headsign = "A" }
        val bTrip = objects.trip { headsign = "B" }

        val stopData =
            RouteCardData.RouteStopData(
                stop,
                listOf(Direction("A Headsign", null, 0), Direction("B Headsign", null, 1)),
                listOf(
                    RouteCardData.Leaf(
                        0,
                        listOf(objects.routePattern(route) {}),
                        setOf(stop.id),
                        listOf(
                            UpcomingTrip(
                                aTrip,
                                objects.prediction { departureTime = now.plus(5.minutes) }
                            )
                        ),
                        emptyList(),
                        true,
                        true
                    ),
                    RouteCardData.Leaf(
                        1,
                        listOf(objects.routePattern(route) {}),
                        setOf(stop.id),
                        listOf(
                            UpcomingTrip(
                                bTrip,
                                objects.prediction { departureTime = now.plus(20.minutes) }
                            )
                        ),
                        emptyList(),
                        true,
                        true
                    )
                )
            )
        val cardData =
            RouteCardData(
                RouteCardData.LineOrRoute.Route(route),
                listOf(stopData),
                RouteCardData.Context.NearbyTransit,
                now,
            )

        composeTestRule.setContent { Departures(stopData, cardData, now, false) { _ -> } }

        composeTestRule.onNodeWithText("5 min").assertIsDisplayed()
        composeTestRule.onNodeWithText(aTrip.headsign).assertIsDisplayed()

        composeTestRule.onNodeWithText("20 min").assertIsDisplayed()
        composeTestRule.onNodeWithText(bTrip.headsign).assertIsDisplayed()
    }

    @Test
    fun testStopHeadsign() {
        val now = Clock.System.now()
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

        val stopData =
            RouteCardData.RouteStopData(
                stop,
                listOf(Direction("A Headsign", null, 0), Direction("B Headsign", null, 1)),
                listOf(
                    RouteCardData.Leaf(
                        0,
                        listOf(objects.routePattern(route) {}),
                        setOf(stop.id),
                        listOf(
                            UpcomingTrip(
                                aTrip,
                                aSchedule,
                                objects.prediction { departureTime = now.plus(5.minutes) }
                            )
                        ),
                        emptyList(),
                        true,
                        true
                    ),
                    RouteCardData.Leaf(
                        1,
                        listOf(objects.routePattern(route) {}),
                        setOf(stop.id),
                        listOf(
                            UpcomingTrip(
                                bTrip,
                                objects.prediction { departureTime = now.plus(20.minutes) }
                            )
                        ),
                        emptyList(),
                        true,
                        true
                    )
                )
            )
        val cardData =
            RouteCardData(
                RouteCardData.LineOrRoute.Route(route),
                listOf(stopData),
                RouteCardData.Context.NearbyTransit,
                now,
            )

        composeTestRule.setContent { Departures(stopData, cardData, now, false) { _ -> } }

        composeTestRule.onNodeWithText(aTrip.headsign).assertDoesNotExist()
        composeTestRule.onNodeWithText(aSchedule.stopHeadsign!!).assertIsDisplayed()
        composeTestRule.onNodeWithText(bTrip.headsign).assertIsDisplayed()
    }

    @Test
    fun testTapAnalytics() {
        val now = Clock.System.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}
        val route =
            objects.route {
                longName = "Route"
                type = RouteType.LIGHT_RAIL
            }

        val aTrip = objects.trip { headsign = "A" }
        val bTrip = objects.trip { headsign = "B" }

        val stopData =
            RouteCardData.RouteStopData(
                stop,
                listOf(Direction("A Headsign", null, 0), Direction("B Headsign", null, 1)),
                listOf(
                    RouteCardData.Leaf(
                        0,
                        listOf(objects.routePattern(route) {}),
                        setOf(stop.id),
                        listOf(
                            UpcomingTrip(
                                aTrip,
                                objects.prediction { departureTime = now.plus(5.minutes) }
                            )
                        ),
                        emptyList(),
                        true,
                        true
                    ),
                    RouteCardData.Leaf(
                        1,
                        listOf(objects.routePattern(route) {}),
                        setOf(stop.id),
                        listOf(
                            UpcomingTrip(
                                bTrip,
                                objects.prediction { departureTime = now.plus(20.minutes) }
                            )
                        ),
                        emptyList(),
                        true,
                        true
                    )
                )
            )
        val cardData =
            RouteCardData(
                RouteCardData.LineOrRoute.Route(route),
                listOf(stopData),
                RouteCardData.Context.NearbyTransit,
                now,
            )

        var tapAnalytics: Pair<String, Map<String, String>>? = null
        var onClickCalled = false

        val koinApplication =
            testKoinApplication(
                analytics = MockAnalytics({ event, props -> tapAnalytics = Pair(event, props) })
            )

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                Departures(stopData, cardData, now, pinned = true) { onClickCalled = true }
            }
        }

        composeTestRule.onNodeWithText(aTrip.headsign).assertIsDisplayed().performClick()

        assertTrue(onClickCalled)
        assertEquals(
            tapAnalytics,
            Pair(
                "tapped_departure",
                mapOf(
                    "route_id" to route.id,
                    "stop_id" to stop.id,
                    "pinned" to "true",
                    "alert" to "false",
                    "mode" to "subway",
                    "no_trips" to ""
                )
            )
        )
    }
}
