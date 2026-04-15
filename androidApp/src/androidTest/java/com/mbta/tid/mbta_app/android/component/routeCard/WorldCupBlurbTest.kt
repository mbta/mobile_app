package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.stopDetails.TripRouteAccents
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.WorldCupService
import org.junit.Rule
import org.junit.Test

class WorldCupBlurbTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun showsBlurbOutbound() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = WorldCupService.route
        objects.put(route)
        composeTestRule.setContent {
            WorldCupBlurb(
                RouteCardData.Leaf(
                    LineOrRoute.Route(route),
                    stop,
                    directionId = 0,
                    routePatterns = listOf(WorldCupService.routePatternOutbound),
                    stopIds = emptySet(),
                    upcomingTrips = emptyList(),
                    alertsHere = emptyList(),
                    allDataLoaded = true,
                    hasSchedulesToday = false,
                    subwayServiceStartTime = null,
                    alertsDownstream = emptyList(),
                    context = RouteCardData.Context.NearbyTransit,
                ),
                TripRouteAccents(route),
                offerDetails = false,
            )
        }
        composeTestRule
            .onNodeWithText("Service from South Station to today’s World Cup match")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Boston Stadium Train ticket required").assertIsDisplayed()
    }

    @Test
    fun showsBlurbInbound() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = WorldCupService.route
        objects.put(route)
        composeTestRule.setContent {
            WorldCupBlurb(
                RouteCardData.Leaf(
                    LineOrRoute.Route(route),
                    stop,
                    directionId = 1,
                    routePatterns = listOf(WorldCupService.routePatternInbound),
                    stopIds = emptySet(),
                    upcomingTrips = emptyList(),
                    alertsHere = emptyList(),
                    allDataLoaded = true,
                    hasSchedulesToday = false,
                    subwayServiceStartTime = null,
                    alertsDownstream = emptyList(),
                    context = RouteCardData.Context.NearbyTransit,
                ),
                TripRouteAccents(route),
                offerDetails = false,
            )
        }
        composeTestRule
            .onNodeWithText("Service from today’s World Cup match to South Station")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Boston Stadium Train ticket required").assertIsDisplayed()
    }

    @Test
    fun hidesViewDetails() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = WorldCupService.route
        objects.put(route)
        composeTestRule.setContent {
            WorldCupBlurb(
                RouteCardData.Leaf(
                    LineOrRoute.Route(route),
                    stop,
                    directionId = 0,
                    routePatterns = listOf(WorldCupService.routePatternOutbound),
                    stopIds = emptySet(),
                    upcomingTrips = emptyList(),
                    alertsHere = emptyList(),
                    allDataLoaded = true,
                    hasSchedulesToday = false,
                    subwayServiceStartTime = null,
                    alertsDownstream = emptyList(),
                    context = RouteCardData.Context.NearbyTransit,
                ),
                TripRouteAccents(route),
                offerDetails = false,
            )
        }
        composeTestRule.onNodeWithText("View details").assertDoesNotExist()
    }

    @Test
    fun showsViewDetails() {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = WorldCupService.route
        objects.put(route)
        composeTestRule.setContent {
            WorldCupBlurb(
                RouteCardData.Leaf(
                    LineOrRoute.Route(route),
                    stop,
                    directionId = 0,
                    routePatterns = listOf(WorldCupService.routePatternOutbound),
                    stopIds = emptySet(),
                    upcomingTrips = emptyList(),
                    alertsHere = emptyList(),
                    allDataLoaded = true,
                    hasSchedulesToday = false,
                    subwayServiceStartTime = null,
                    alertsDownstream = emptyList(),
                    context = RouteCardData.Context.NearbyTransit,
                ),
                TripRouteAccents(route),
                offerDetails = true,
            )
        }
        composeTestRule.onNodeWithText("View details").assertIsDisplayed()
    }
}
