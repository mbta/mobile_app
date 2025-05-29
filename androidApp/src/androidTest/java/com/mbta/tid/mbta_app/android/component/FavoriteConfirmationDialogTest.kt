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
        var updateFavoritesCalledFor: Map<RouteStopDirection, Boolean> = mapOf()
        var onCloseCalled = false

        composeTestRule.setContent {
            FavoriteConfirmationDialog(
                lineOrRoute = line,
                stop = stop,
                directions = directions,
                proposedFavorites = mapOf(0 to true),
                updateFavorites = { updateFavoritesCalledFor = (it) },
            ) {
                onCloseCalled = true
            }
        }

        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            updateFavoritesCalledFor,
            mapOf(RouteStopDirection(line.id, stop.id, 0) to true),
        )
        assertTrue(onCloseCalled)
    }

    @Test
    fun testCancelDoesntUpdateFavorites() {
        var updateFavoritesCalled = false
        var onCloseCalled = false

        composeTestRule.setContent {
            FavoriteConfirmationDialog(
                lineOrRoute = line,
                stop = stop,
                directions = directions,
                proposedFavorites = mapOf(0 to true),
                updateFavorites = { updateFavoritesCalled = true },
            ) {
                onCloseCalled = true
            }
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        assertTrue(onCloseCalled)
        assertFalse(updateFavoritesCalled)
    }

    @Test
    fun testAddingOtherDirectionSavesBoth() {
        var updateFavoritesCalledFor: Map<RouteStopDirection, Boolean> = mapOf()

        composeTestRule.setContent {
            FavoriteConfirmationDialog(
                lineOrRoute = line,
                stop = stop,
                directions = directions,
                proposedFavorites = mapOf(0 to true),
                updateFavorites = { updateFavoritesCalledFor = it },
            ) {}
        }

        composeTestRule.onNodeWithText("East", substring = true).performClick()
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            updateFavoritesCalledFor,
            mapOf(
                RouteStopDirection(line.id, stop.id, 0) to true,
                RouteStopDirection(line.id, stop.id, 1) to true,
            ),
        )
    }

    @Test
    fun testRemovingOtherDirectoinSavesBoth() {
        var updateFavoritesCalledFor: Map<RouteStopDirection, Boolean> = mapOf()

        composeTestRule.setContent {
            FavoriteConfirmationDialog(
                lineOrRoute = line,
                stop = stop,
                directions = directions,
                proposedFavorites = mapOf(0 to true, 1 to true),
                updateFavorites = { updateFavoritesCalledFor = it },
            ) {}
        }

        composeTestRule.onNodeWithText("East", substring = true).performClick()
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            updateFavoritesCalledFor,
            mapOf(
                RouteStopDirection(line.id, stop.id, 0) to true,
                RouteStopDirection(line.id, stop.id, 1) to false,
            ),
        )
    }

    @Test
    fun testRemovingProposedFavoriteUpdatesToFalse() {
        var updateFavoritesCalledFor: Map<RouteStopDirection, Boolean> = mapOf()
        var onCloseCalled = false

        composeTestRule.setContent {
            FavoriteConfirmationDialog(
                lineOrRoute = line,
                stop = stop,
                directions = directions,
                proposedFavorites = mapOf(0 to true),
                updateFavorites = { updateFavoritesCalledFor = it },
            ) {
                onCloseCalled = true
            }
        }

        composeTestRule.onNodeWithText("West", substring = true).performClick()
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        assertTrue(onCloseCalled)
        assertEquals(
            updateFavoritesCalledFor,
            mapOf(RouteStopDirection(line.id, stop.id, 0) to false),
        )
    }
}
