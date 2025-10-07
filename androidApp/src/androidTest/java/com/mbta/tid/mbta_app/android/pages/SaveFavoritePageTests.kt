package com.mbta.tid.mbta_app.android.pages

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.Favorites
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.repositories.MockFavoritesRepository
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.utils.TestData
import com.mbta.tid.mbta_app.utils.buildFavorites
import kotlin.test.assertEquals
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.Test

class SaveFavoritePageTests {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testCloses() {
        var backCalled = false

        val objects = TestData.clone()
        val route = objects.getRoute("Red")
        val stop = objects.getStop("70069")

        loadKoinMocks(objects)

        composeTestRule.setContent {
            SaveFavoritePage(
                route.id,
                stop.id,
                1,
                EditFavoritesContext.StopDetails,
                { backCalled = true },
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        assert(backCalled)
    }

    @Test
    fun testSaves() {
        val objects = TestData.clone()
        val route = objects.getRoute("Orange")
        val stop = objects.getStop("place-welln")
        var favoritesSet: Favorites? = null
        var closed = false
        loadKoinMocks(objects) {
            favorites = MockFavoritesRepository(onSet = { favoritesSet = it })
        }

        composeTestRule.setContent {
            SaveFavoritePage(
                route.id,
                stop.id,
                0,
                EditFavoritesContext.StopDetails,
                { closed = true },
            )
        }

        composeTestRule.onNodeWithText("Add Favorite").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            Favorites(mapOf(RouteStopDirection(route.id, stop.id, 0) to FavoriteSettings())),
            favoritesSet,
        )
        assert(closed)
    }

    @Test
    fun testTogglesDirection() {
        val objects = TestData.clone()
        val route = objects.getRoute("Orange")
        val stop = objects.getStop("place-welln")
        var favoritesSet: Favorites? = null
        loadKoinMocks(objects) {
            favorites = MockFavoritesRepository(onSet = { favoritesSet = it })
        }

        composeTestRule.setContent {
            SaveFavoritePage(route.id, stop.id, 0, EditFavoritesContext.StopDetails, {})
        }

        composeTestRule.onNodeWithText("Southbound to").assertIsDisplayed()
        composeTestRule.onNodeWithText("Forest Hills").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("toggle direction")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithText("Northbound to").assertIsDisplayed()
        composeTestRule.onNodeWithText("Oak Grove").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            Favorites(mapOf(RouteStopDirection(route.id, stop.id, 1) to FavoriteSettings())),
            favoritesSet,
        )
    }

    @Test
    fun testNotifications() {
        val objects = TestData.clone()
        val route = objects.getRoute("Orange")
        val stop = objects.getStop("place-welln")
        var favoritesSet: Favorites? = null
        loadKoinMocks(objects) {
            favorites = MockFavoritesRepository(onSet = { favoritesSet = it })
        }

        composeTestRule.setContent {
            SaveFavoritePage(route.id, stop.id, 0, EditFavoritesContext.StopDetails, {})
        }

        composeTestRule.onNodeWithText("Add Favorite").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            buildFavorites {
                routeStopDirection(route.id, stop.id, 0) {
                    notifications {
                        enabled = true
                        window(
                            LocalTime(8, 0),
                            LocalTime(9, 0),
                            setOf(
                                DayOfWeek.MONDAY,
                                DayOfWeek.TUESDAY,
                                DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY,
                                DayOfWeek.FRIDAY,
                            ),
                        )
                    }
                }
            },
            favoritesSet,
        )
    }

    @Test
    fun testEditExisting() {
        val objects = TestData.clone()
        val route = objects.getRoute("Orange")
        val stop = objects.getStop("place-welln")
        var favoritesSet: Favorites? = null
        var closed = false
        loadKoinMocks(objects) {
            favorites =
                MockFavoritesRepository(
                    favorites =
                        buildFavorites {
                            routeStopDirection(route.id, stop.id, 0) {
                                notifications {
                                    enabled = true
                                    window(
                                        LocalTime(8, 0),
                                        LocalTime(9, 0),
                                        setOf(DayOfWeek.TUESDAY),
                                    )
                                }
                            }
                        },
                    onSet = { favoritesSet = it },
                )
        }

        composeTestRule.setContent {
            SaveFavoritePage(
                route.id,
                stop.id,
                0,
                EditFavoritesContext.StopDetails,
                { closed = true },
            )
        }

        composeTestRule.onNodeWithText("Edit Favorite").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("toggle direction").assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Get disruption notifications")
            .assertIsDisplayed()
            .assertIsOn()
        composeTestRule
            .onNodeWithText("Remove from Favorites")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(Favorites(), favoritesSet)
        assert(closed)
    }

    @Test
    fun testOnlyOppositeDirection() {
        val objects = TestData.clone()
        val route = objects.getRoute("Orange")
        val stop = objects.getStop("place-forhl")
        loadKoinMocks(objects)

        composeTestRule.setContent {
            SaveFavoritePage(route.id, stop.id, 0, EditFavoritesContext.StopDetails, {})
        }

        composeTestRule.onNodeWithText("Only Northbound to").assertIsDisplayed()
        composeTestRule.onNodeWithText("Oak Grove").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("toggle direction").assertDoesNotExist()
    }
}
