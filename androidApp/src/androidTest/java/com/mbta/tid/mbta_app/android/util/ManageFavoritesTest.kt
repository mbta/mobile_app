package com.mbta.tid.mbta_app.android.util

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.MockFavoritesRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.MockSubscriptionsRepository
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.usecases.FavoritesUsecases
import com.mbta.tid.mbta_app.utils.buildFavorites
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class ManageFavoritesTest {
    @get:Rule val composeTestRule = createComposeRule(effectContext = UnconfinedTestDispatcher())

    @Test
    fun testManageFavorites() = runBlocking {
        val rsd0 = RouteStopDirection(Route.Id("route1"), "stop1", 0)
        val rsd1 = RouteStopDirection(Route.Id("route1"), "stop1", 1)
        val favoritesRepo = MockFavoritesRepository(buildFavorites { routeStopDirection(rsd0) })

        val objects = ObjectCollectionBuilder()
        val route = objects.route { id = "route1" }
        val stop = objects.stop { id = "stop1" }
        val otherStop = objects.stop { id = "other_stop" }
        objects.routePattern(route) {
            directionId = 0
            representativeTrip { stopIds = listOf(stop.id, otherStop.id) }
        }
        objects.routePattern(route) {
            directionId = 1
            representativeTrip { stopIds = listOf(stop.id, otherStop.id) }
        }

        val favoritesUseCases =
            FavoritesUsecases(
                favoritesRepo,
                MockGlobalRepository(GlobalResponse(objects)),
                MockSettingsRepository(),
                MockSubscriptionsRepository(),
                MockAnalytics(),
            )

        var managedFavorites: ManagedFavorites? = null
        composeTestRule.setContent {
            managedFavorites = manageFavorites(favoritesUseCases)
            Column {
                Button(
                    onClick = {
                        CoroutineScope(Dispatchers.Default).launch {
                            managedFavorites.updateFavorites(
                                mapOf(rsd0 to null, rsd1 to FavoriteSettings()),
                                EditFavoritesContext.Favorites,
                                1,
                            )
                        }
                    }
                ) {
                    Text("Click me")
                }
            }
        }

        composeTestRule.awaitIdle()
        assertNotNull(managedFavorites)
        assertEquals(mapOf(rsd0 to FavoriteSettings()), managedFavorites!!.favoriteRoutes)
        composeTestRule.onNodeWithText("Click me").performClick()
        composeTestRule.awaitIdle()
        composeTestRule.waitUntilDefaultTimeout {
            managedFavorites.favoriteRoutes == mapOf(rsd1 to FavoriteSettings())
        }
    }
}
