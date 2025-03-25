package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.UpcomingTrip
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test

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

        composeTestRule.setContent { Departures(stopData, cardData, now) { _ -> } }

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

        composeTestRule.setContent { Departures(stopData, cardData, now) { _ -> } }

        composeTestRule.onNodeWithText(aTrip.headsign).assertDoesNotExist()
        composeTestRule.onNodeWithText(aSchedule.stopHeadsign!!).assertIsDisplayed()
        composeTestRule.onNodeWithText(bTrip.headsign).assertIsDisplayed()
    }
}
