package com.mbta.tid.mbta_app.android.util

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.model.Favorites
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.repositories.IFavoritesRepository
import com.mbta.tid.mbta_app.usecases.FavoritesUsecases
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
        val favoritesRepo =
            object : IFavoritesRepository {
                var favorites = Favorites()

                override suspend fun getFavorites(): Favorites {
                    return favorites
                }

                override suspend fun setFavorites(favorites: Favorites) {
                    this.favorites = favorites
                }
            }
        val favoritesUseCases = FavoritesUsecases(favoritesRepo)

        var managedFavorites: ManagedFavorites? = null
        composeTestRule.setContent {
            managedFavorites = manageFavorites(favoritesUseCases)
            Column {
                Button(
                    onClick = {
                        CoroutineScope(Dispatchers.Default).launch {
                            managedFavorites!!.toggleFavorite(
                                RouteStopDirection("route1", "stop1", 1)
                            )
                        }
                    }
                ) {
                    Text("Toggle Me")
                }
            }
        }

        composeTestRule.awaitIdle()
        assertNotNull(managedFavorites)
        assertEquals(emptySet<RouteStopDirection>(), managedFavorites!!.favoriteRoutes)
        composeTestRule.onNodeWithText("Toggle Me").performClick()
        composeTestRule.awaitIdle()
        composeTestRule.waitUntil {
            managedFavorites!!.favoriteRoutes?.firstOrNull() ==
                RouteStopDirection("route1", "stop1", 1)
        }
        composeTestRule.onNodeWithText("Toggle Me").performClick()
        composeTestRule.awaitIdle()
        composeTestRule.waitUntil { managedFavorites!!.favoriteRoutes?.isEmpty() == true }
    }
}
