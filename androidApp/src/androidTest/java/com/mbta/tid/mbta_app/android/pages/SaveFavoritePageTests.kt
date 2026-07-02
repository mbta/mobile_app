package com.mbta.tid.mbta_app.android.pages

import android.os.Build
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.android.testUtils.assertCanBeDisplayed
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
    @get:Rule
    val runtimePermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)
        else GrantPermissionRule.grant()

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

        composeTestRule.onNodeWithText("Add Favorite").assertCanBeDisplayed()
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

        composeTestRule.onNodeWithText("Southbound to").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Forest Hills").assertCanBeDisplayed()
        composeTestRule
            .onNodeWithContentDescription("toggle direction")
            .assertCanBeDisplayed()
            .performClick()
        composeTestRule.onNodeWithText("Northbound to").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Oak Grove").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            Favorites(mapOf(RouteStopDirection(route.id, stop.id, 1) to FavoriteSettings())),
            favoritesSet,
        )
    }

    @Test
    fun testTogglesDirectionWithSaved() {
        val objects = TestData.clone()
        val route = objects.getRoute("Orange")
        val stop = objects.getStop("place-welln")
        loadKoinMocks(objects) {
            favorites =
                MockFavoritesRepository(
                    favorites =
                        Favorites(
                            mapOf(RouteStopDirection(route.id, stop.id, 1) to FavoriteSettings())
                        )
                )
        }

        composeTestRule.setContent {
            SaveFavoritePage(route.id, stop.id, 0, EditFavoritesContext.StopDetails, {})
        }

        composeTestRule.onNodeWithText("Southbound to").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Forest Hills").assertCanBeDisplayed()
        composeTestRule
            .onNodeWithContentDescription("toggle direction")
            .assertCanBeDisplayed()
            .performClick()
        composeTestRule.onNodeWithText("Northbound to").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Oak Grove").assertCanBeDisplayed()
        composeTestRule
            .onNodeWithContentDescription("toggle direction")
            .assertCanBeDisplayed()
            .performClick()
        composeTestRule.onNodeWithText("Southbound to").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Forest Hills").assertCanBeDisplayed()
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

        composeTestRule.onNodeWithText("Add Favorite").assertCanBeDisplayed()
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

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Edit Favorite").assertCanBeDisplayed()
        composeTestRule.onNodeWithContentDescription("toggle direction").assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Get disruption notifications")
            .assertCanBeDisplayed()
            .assertIsOn()
        composeTestRule
            .onNodeWithText("Remove from Favorites")
            .assertCanBeDisplayed()
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

        composeTestRule.onNodeWithText("Only Northbound to").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Oak Grove").assertCanBeDisplayed()
        composeTestRule.onNodeWithContentDescription("toggle direction").assertDoesNotExist()
    }
}
