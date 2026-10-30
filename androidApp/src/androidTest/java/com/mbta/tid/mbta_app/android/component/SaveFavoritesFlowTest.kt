package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SaveFavoritesFlowTest {

    @get:Rule val composeTestRule = createComposeRule(effectContext = UnconfinedTestDispatcher())

    val line =
        LineOrRoute.Line(
            TestData.getLine("line-Green"),
            setOf(
                TestData.getRoute("Green-B"),
                TestData.getRoute("Green-C"),
                TestData.getRoute("Green-D"),
                TestData.getRoute("Green-E"),
            ),
        )
    val stop = TestData.getStop("place-boyls")
    val direction0 = Direction(id = 0, name = "West", destination = "Copley & West")
    val direction1 = Direction(id = 1, name = "East", destination = "Park St & North")
    val directions = listOf(direction0, direction1)

    @Test
    fun testWithoutTappingAnyButtonSavesProposedChanges() {
        var updateFavoritesCalledFor: Map<RouteStopDirection, FavoriteSettings?> = mapOf()
        var onCloseCalled = false

        loadKoinMocks { settings = MockSettingsRepository(mapOf(Settings.Notifications to false)) }

        composeTestRule.setContent {
            FavoriteConfirmation(
                lineOrRoute = line,
                stop = stop,
                selectedDirection = 0,
                directions = directions,
                proposedFavorites = mapOf(0 to FavoriteSettings()),
                context = EditFavoritesContext.Favorites,
                updateFavorites = { updateFavoritesCalledFor = (it) },
            ) {
                onCloseCalled = true
            }
        }

        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            updateFavoritesCalledFor,
            mapOf(RouteStopDirection(line.id, stop.id, 0) to FavoriteSettings()),
        )
        assertTrue(onCloseCalled)
    }

    @Test
    fun testCancelDoesntUpdateFavorites() {
        var updateFavoritesCalled = false
        var onCloseCalled = false

        loadKoinMocks { settings = MockSettingsRepository(mapOf(Settings.Notifications to false)) }

        composeTestRule.setContent {
            FavoriteConfirmation(
                lineOrRoute = line,
                stop = stop,
                directions = directions,
                selectedDirection = 0,
                proposedFavorites = mapOf(0 to FavoriteSettings()),
                context = EditFavoritesContext.Favorites,
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
        var updateFavoritesCalledFor: Map<RouteStopDirection, FavoriteSettings?> = mapOf()

        loadKoinMocks { settings = MockSettingsRepository(mapOf(Settings.Notifications to false)) }

        composeTestRule.setContent {
            FavoriteConfirmation(
                lineOrRoute = line,
                stop = stop,
                directions = directions,
                selectedDirection = 0,
                proposedFavorites = mapOf(0 to FavoriteSettings()),
                context = EditFavoritesContext.Favorites,
                updateFavorites = { updateFavoritesCalledFor = it },
            ) {}
        }

        composeTestRule.onNodeWithText("East", substring = true).performClick()
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            updateFavoritesCalledFor,
            mapOf(
                RouteStopDirection(line.id, stop.id, 0) to FavoriteSettings(),
                RouteStopDirection(line.id, stop.id, 1) to FavoriteSettings(),
            ),
        )
    }

    @Test
    fun testRemovingOtherDirectoinSavesBoth() {
        var updateFavoritesCalledFor: Map<RouteStopDirection, FavoriteSettings?> = mapOf()

        loadKoinMocks { settings = MockSettingsRepository(mapOf(Settings.Notifications to false)) }

        composeTestRule.setContent {
            FavoriteConfirmation(
                lineOrRoute = line,
                stop = stop,
                directions = directions,
                selectedDirection = 0,
                proposedFavorites = mapOf(0 to FavoriteSettings(), 1 to FavoriteSettings()),
                context = EditFavoritesContext.Favorites,
                updateFavorites = { updateFavoritesCalledFor = it },
            ) {}
        }

        composeTestRule.onNodeWithText("East", substring = true).performClick()
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            updateFavoritesCalledFor,
            mapOf(
                RouteStopDirection(line.id, stop.id, 0) to FavoriteSettings(),
                RouteStopDirection(line.id, stop.id, 1) to null,
            ),
        )
    }

    @Test
    fun testFavoritingWhenDropOffOnly() {
        var onCloseCalled = false
        var modalOpened: ModalRoutes? = null

        composeTestRule.setContent {
            SaveFavoritesFlow(
                lineOrRoute = line,
                stop = stop,
                directions = listOf(),
                selectedDirection = 0,
                context = EditFavoritesContext.Favorites,
                isFavorite = { false },
                updateFavorites = {},
                onClose = { onCloseCalled = true },
                openModal = { modal -> modalOpened = modal },
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilDefaultTimeout { onCloseCalled }
        assertTrue(onCloseCalled)
        assertEquals(
            ModalRoutes.SaveFavorite(line.id, stop.id, 0, EditFavoritesContext.Favorites),
            modalOpened,
        )
    }

    @Test
    fun testPopsModalThenClosesWhenNotificationsFlagIsEnabled() {
        var onCloseCalled = false
        var modalOpened: ModalRoutes? = null

        loadKoinMocks { settings = MockSettingsRepository(mapOf(Settings.Notifications to true)) }

        composeTestRule.setContent {
            SaveFavoritesFlow(
                lineOrRoute = line,
                stop = stop,
                directions = directions,
                selectedDirection = 0,
                context = EditFavoritesContext.Favorites,
                isFavorite = { rsd -> rsd.direction == 1 },
                updateFavorites = { _ -> },
                onClose = { onCloseCalled = true },
                openModal = { modal -> modalOpened = modal },
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilDefaultTimeout { onCloseCalled }
        assertTrue(onCloseCalled)
        assertEquals(
            ModalRoutes.SaveFavorite(line.id, stop.id, 0, EditFavoritesContext.Favorites),
            modalOpened,
        )
    }
}
