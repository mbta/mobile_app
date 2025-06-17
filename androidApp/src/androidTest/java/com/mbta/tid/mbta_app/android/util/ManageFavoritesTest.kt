package com.mbta.tid.mbta_app.android.util

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.model.Favorites
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.repositories.MockFavoritesRepository
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
        val rsd0 = RouteStopDirection("route1", "stop1", 0)
        val rsd1 = RouteStopDirection("route1", "stop1", 1)
        val favoritesRepo = MockFavoritesRepository(Favorites(routeStopDirection = setOf(rsd0)))
        val favoritesUseCases = FavoritesUsecases(favoritesRepo)

        var managedFavorites: ManagedFavorites? = null
        composeTestRule.setContent {
            managedFavorites = manageFavorites(favoritesUseCases)
            Column {
                Button(
                    onClick = {
                        CoroutineScope(Dispatchers.Default).launch {
                            managedFavorites!!.updateFavorites(mapOf(rsd0 to false, rsd1 to true))
                        }
                    }
                ) {
                    Text("Click me")
                }
            }
        }

        composeTestRule.awaitIdle()
        assertNotNull(managedFavorites)
        assertEquals(setOf(rsd0), managedFavorites!!.favoriteRoutes)
        composeTestRule.onNodeWithText("Click me").performClick()
        composeTestRule.awaitIdle()
        composeTestRule.waitUntil { managedFavorites!!.favoriteRoutes == setOf(rsd1) }
    }
}
