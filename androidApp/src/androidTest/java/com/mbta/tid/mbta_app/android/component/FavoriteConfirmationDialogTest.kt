package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class FavoriteConfirmationDialogTest {

    @get:Rule val composeTestRule = createComposeRule()

    val line = RouteCardData.LineOrRoute.Line(TestData.getLine("line-Green"), emptySet())
    val stop = TestData.getStop("place-boyls")
    val directions =
        listOf(
            Direction(id = 0, name = "West", destination = "Copley & West"),
            Direction(id = 1, name = "East", destination = "Park St & North"),
        )

    @Test
    fun testWithoutTappingAnyButtonSavesProposedChanges() {
        val toggleFavoritesCalledFor: MutableSet<RouteStopDirection> = mutableSetOf()
        var onCloseCalled = false

        composeTestRule.setContent {
            FavoriteConfirmationDialog(
                lineOrRoute = line,
                stop = stop,
                directions = directions,
                currentFavorites = mapOf(),
                proposedFavoritesToToggle = setOf(0),
                toggleFavorite = { toggleFavoritesCalledFor.add(it) },
            ) {
                onCloseCalled = true
            }
        }

        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        assertEquals(toggleFavoritesCalledFor, setOf(RouteStopDirection(line.id, stop.id, 0)))
        assertTrue(onCloseCalled)
    }

    @Test
    fun testCancelDoesntToggleFavorites() {
        var toggleFavoritesCalled = false
        var onCloseCalled = false

        composeTestRule.setContent {
            FavoriteConfirmationDialog(
                lineOrRoute = line,
                stop = stop,
                directions = directions,
                currentFavorites = mapOf(),
                proposedFavoritesToToggle = setOf(0),
                toggleFavorite = { toggleFavoritesCalled = true },
            ) {
                onCloseCalled = true
            }
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        assertTrue(onCloseCalled)
        assertFalse(toggleFavoritesCalled)
    }

    @Test
    fun testAddingOtherDirectionSavesBoth() {
        val toggleFavoritesCalledFor: MutableSet<RouteStopDirection> = mutableSetOf()

        composeTestRule.setContent {
            FavoriteConfirmationDialog(
                lineOrRoute = line,
                stop = stop,
                directions = directions,
                currentFavorites = mapOf(),
                proposedFavoritesToToggle = setOf(0),
                toggleFavorite = { toggleFavoritesCalledFor.add(it) },
            ) {}
        }

        composeTestRule.onNodeWithText("East", substring = true).performClick()
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            toggleFavoritesCalledFor,
            setOf(RouteStopDirection(line.id, stop.id, 0), RouteStopDirection(line.id, stop.id, 1)),
        )
    }

    @Test
    fun testRemovingOtherDirectoinSavesBoth() {
        val toggleFavoritesCalledFor: MutableSet<RouteStopDirection> = mutableSetOf()

        composeTestRule.setContent {
            FavoriteConfirmationDialog(
                lineOrRoute = line,
                stop = stop,
                directions = directions,
                currentFavorites = mapOf(1 to true),
                proposedFavoritesToToggle = setOf(0),
                toggleFavorite = { toggleFavoritesCalledFor.add(it) },
            ) {}
        }

        composeTestRule.onNodeWithText("East", substring = true).performClick()
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            toggleFavoritesCalledFor,
            setOf(RouteStopDirection(line.id, stop.id, 0), RouteStopDirection(line.id, stop.id, 1)),
        )
    }

    @Test
    fun testRemovingProposedFavoriteDoesntToggleAnyFavorites() {
        var toggleFavoritesCalled = false
        var onCloseCalled = false

        composeTestRule.setContent {
            FavoriteConfirmationDialog(
                lineOrRoute = line,
                stop = stop,
                directions = directions,
                currentFavorites = mapOf(),
                proposedFavoritesToToggle = setOf(0),
                toggleFavorite = { toggleFavoritesCalled = true },
            ) {
                onCloseCalled = true
            }
        }

        composeTestRule.onNodeWithText("West", substring = true).performClick()
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        assertTrue(onCloseCalled)
        assertFalse(toggleFavoritesCalled)
    }
}
