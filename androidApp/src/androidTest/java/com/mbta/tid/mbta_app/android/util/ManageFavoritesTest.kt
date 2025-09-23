package com.mbta.tid.mbta_app.android.util

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.Favorites
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.repositories.MockFavoritesRepository
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.usecases.FavoritesUsecases
import com.mbta.tid.mbta_app.utils.buildFavorites
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class ManageFavoritesTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testManageFavorites() = runBlocking {
        val rsd0 = RouteStopDirection("route1", "stop1", 0)
        val rsd1 = RouteStopDirection("route1", "stop1", 1)
        val favoritesRepo = MockFavoritesRepository(buildFavorites { routeStopDirection(rsd0) })
        val favoritesUseCases = FavoritesUsecases(favoritesRepo, MockAnalytics())

        var managedFavorites: ManagedFavorites? = null
        composeTestRule.setContent {
            managedFavorites = manageFavorites(favoritesUseCases)
            Column {
                Button(
                    onClick = {
                        CoroutineScope(Dispatchers.Default).launch {
                            managedFavorites.updateFavorites(
                                mapOf(
                                    rsd0 to null,
                                    rsd1 to
                                        FavoriteSettings(
                                            notifications = FavoriteSettings.Notifications.disabled
                                        ),
                                ),
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
        assertEquals(
            mapOf(
                rsd0 to FavoriteSettings(notifications = FavoriteSettings.Notifications.disabled)
            ),
            managedFavorites!!.favoriteRoutes,
        )
        composeTestRule.onNodeWithText("Click me").performClick()
        composeTestRule.awaitIdle()
        composeTestRule.waitUntilDefaultTimeout {
            managedFavorites.favoriteRoutes ==
                mapOf(
                    rsd1 to
                        FavoriteSettings(notifications = FavoriteSettings.Notifications.disabled)
                )
        }
    }
}
