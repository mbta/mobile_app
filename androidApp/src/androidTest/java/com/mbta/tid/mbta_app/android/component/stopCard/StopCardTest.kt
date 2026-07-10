package com.mbta.tid.mbta_app.android.component.stopCard

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.testUtils.assertCanBeDisplayed
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopCardData
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import org.junit.Rule
import org.junit.Test

class StopCardTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testNearbyCard() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop { name = "Stop" }
        val route =
            objects.route {
                longName = "Route"
                type = RouteType.LIGHT_RAIL
            }

        composeTestRule.setContent {
            StopCard(
                StopCardData(
                    stop,
                    listOf(
                        RouteCardData.Leaf(
                            LineOrRoute.Route(route),
                            stop,
                            Direction(0, route),
                            emptyList(),
                            emptySet(),
                            emptyList(),
                            emptyList(),
                            true,
                            true,
                            null,
                            emptyList(),
                            RouteCardData.Context.Favorites,
                        )
                    ),
                ),
                GlobalResponse(objects),
                now,
                isFavorite = { false },
            ) { _, _ ->
            }
        }

        composeTestRule.onNodeWithText(route.label, ignoreCase = true).assertCanBeDisplayed()
        composeTestRule.onNodeWithText(stop.name).assertCanBeDisplayed()
    }
}
