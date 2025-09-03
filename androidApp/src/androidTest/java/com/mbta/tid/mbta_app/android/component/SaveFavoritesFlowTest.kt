package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.utils.TestData
import com.mbta.tid.mbta_app.viewModel.MockToastViewModel
import com.mbta.tid.mbta_app.viewModel.ToastViewModel
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext

@OptIn(ExperimentalTestApi::class)
class SaveFavoritesFlowTest {

    @get:Rule val composeTestRule = createComposeRule()

    val line =
        RouteCardData.LineOrRoute.Line(
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
        var updateFavoritesCalledFor: Map<RouteStopDirection, Boolean> = mapOf()
        var onCloseCalled = false

        composeTestRule.setContent {
            FavoriteConfirmationDialog(
                lineOrRoute = line,
                stop = stop,
                selectedDirection = 0,
                directions = directions,
                proposedFavorites = mapOf(0 to true),
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
                selectedDirection = 0,
                proposedFavorites = mapOf(0 to true),
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
        var updateFavoritesCalledFor: Map<RouteStopDirection, Boolean> = mapOf()

        composeTestRule.setContent {
            FavoriteConfirmationDialog(
                lineOrRoute = line,
                stop = stop,
                directions = directions,
                selectedDirection = 0,
                proposedFavorites = mapOf(0 to true),
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
                selectedDirection = 0,
                proposedFavorites = mapOf(0 to true, 1 to true),
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
                RouteStopDirection(line.id, stop.id, 0) to true,
                RouteStopDirection(line.id, stop.id, 1) to false,
            ),
        )
    }

    @Test
    fun testRemovingProposedFavoriteDisablesAddButton() {
        var updateFavoritesCalledFor: Map<RouteStopDirection, Boolean> = mapOf()
        var onCloseCalled = false

        composeTestRule.setContent {
            FavoriteConfirmationDialog(
                lineOrRoute = line,
                stop = stop,
                directions = directions,
                selectedDirection = 0,
                proposedFavorites = mapOf(0 to true),
                context = EditFavoritesContext.Favorites,
                updateFavorites = { updateFavoritesCalledFor = it },
            ) {
                onCloseCalled = true
            }
        }

        composeTestRule.onNodeWithText("West", substring = true).performClick()
        composeTestRule.onNodeWithText("Add").assertIsNotEnabled()
    }

    @Test
    fun testFavoritingOnlyDirectionPresentsDialogWhenNonBus() {
        var updateFavoritesCalledFor: Map<RouteStopDirection, Boolean> = mapOf()
        var onCloseCalled = false

        composeTestRule.setContent {
            SaveFavoritesFlow(
                lineOrRoute = line,
                stop = stop,
                directions = listOf(direction0),
                selectedDirection = 0,
                context = EditFavoritesContext.Favorites,
                isFavorite = { false },
                updateFavorites = { updateFavoritesCalledFor = it },
                onClose = { onCloseCalled = true },
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Add"))
        composeTestRule.onNodeWithText("Add").assertExists()
    }

    @Test
    fun testFavoritingOnlyDirectionSkipsDialogWhenBus() {
        var updateFavoritesCalledFor: Map<RouteStopDirection, Boolean> = mapOf()
        var onCloseCalled = false

        val busRoute = TestData.getRoute("15")
        val busStop = TestData.getStop("17861")

        composeTestRule.setContent {
            SaveFavoritesFlow(
                lineOrRoute = RouteCardData.LineOrRoute.Route(busRoute),
                stop = busStop,
                directions = listOf(direction0),
                selectedDirection = 0,
                context = EditFavoritesContext.Favorites,
                isFavorite = { false },
                updateFavorites = { updateFavoritesCalledFor = it },
                onClose = { onCloseCalled = true },
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilDefaultTimeout { onCloseCalled }
        assertTrue(onCloseCalled)
        assertEquals(
            updateFavoritesCalledFor,
            mapOf(RouteStopDirection(busRoute.id, busStop.id, 0) to true),
        )
    }

    @Test
    fun testUnfavoritingOnlyDirectionUpdatesFavoritesWithoutDialog() {
        var updateFavoritesCalledFor: Map<RouteStopDirection, Boolean> = mapOf()
        var onCloseCalled = false

        composeTestRule.setContent {
            SaveFavoritesFlow(
                lineOrRoute = line,
                stop = stop,
                directions = listOf(direction0),
                selectedDirection = 0,
                context = EditFavoritesContext.Favorites,
                isFavorite = { true },
                updateFavorites = { updateFavoritesCalledFor = it },
                onClose = { onCloseCalled = true },
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilDefaultTimeout { onCloseCalled }
        assertTrue(onCloseCalled)
        assertEquals(
            updateFavoritesCalledFor,
            mapOf(RouteStopDirection(line.id, stop.id, 0) to false),
        )
    }

    @Test
    fun testFavoritingWhenOnlyDirectionIsOppositePresentsDialog() {
        var updateFavoritesCalledFor: Map<RouteStopDirection, Boolean> = mapOf()
        var onCloseCalled = false

        composeTestRule.setContent {
            SaveFavoritesFlow(
                lineOrRoute = line,
                stop = stop,
                directions = listOf(direction1),
                selectedDirection = 0,
                context = EditFavoritesContext.Favorites,
                isFavorite = { false },
                updateFavorites = { updateFavoritesCalledFor = it },
                onClose = { onCloseCalled = true },
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Eastbound service only").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilDefaultTimeout { onCloseCalled }
        assertTrue(onCloseCalled)
        assertEquals(
            updateFavoritesCalledFor,
            mapOf(RouteStopDirection(line.id, stop.id, 1) to true),
        )
    }

    @Test
    fun testFavoritingSingleDirectionDisplaysToast() {
        val toastVM = MockToastViewModel()
        var displayedToast: ToastViewModel.Toast? = null
        toastVM.onShowToast = { displayedToast = it }

        val koin = testKoinApplication(TestData)

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                SaveFavoritesFlow(
                    lineOrRoute = line,
                    stop = stop,
                    directions = listOf(direction0, direction1),
                    selectedDirection = 0,
                    context = EditFavoritesContext.Favorites,
                    toastViewModel = toastVM,
                    isFavorite = { false },
                    updateFavorites = {},
                    onClose = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Add"))
        composeTestRule.onNodeWithText("Add").performClick()

        composeTestRule.waitUntil {
            displayedToast?.message ==
                "<b>Westbound Green Line</b> at <b>Boylston</b> added to Favorites"
        }
    }

    @Test
    fun testFavoritingBothDirectionsDisplaysToast() {
        val toastVM = MockToastViewModel()
        var displayedToast: ToastViewModel.Toast? = null
        toastVM.onShowToast = { displayedToast = it }

        val koin = testKoinApplication(TestData)

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                SaveFavoritesFlow(
                    lineOrRoute = line,
                    stop = stop,
                    directions = listOf(direction0, direction1),
                    selectedDirection = 0,
                    context = EditFavoritesContext.Favorites,
                    toastViewModel = toastVM,
                    isFavorite = { false },
                    updateFavorites = {},
                    onClose = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("East", substring = true).performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Add"))
        composeTestRule.onNodeWithText("Add").performClick()

        composeTestRule.waitUntil {
            displayedToast?.message == "<b>Green Line</b> at <b>Boylston</b> added to Favorites"
        }
    }

    @Test
    fun testFavoritingDisplaysToastWhenDialogIsSkipped() {
        val toastVM = MockToastViewModel()
        var displayedToast: ToastViewModel.Toast? = null
        toastVM.onShowToast = { displayedToast = it }

        val busRoute = TestData.getRoute("15")
        val busStop = TestData.getStop("17861")

        val koin = testKoinApplication(TestData)

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                SaveFavoritesFlow(
                    lineOrRoute = RouteCardData.LineOrRoute.Route(busRoute),
                    stop = busStop,
                    directions = listOf(direction0),
                    selectedDirection = 0,
                    context = EditFavoritesContext.Favorites,
                    toastViewModel = toastVM,
                    isFavorite = { false },
                    updateFavorites = {},
                    onClose = {},
                )
            }
        }

        composeTestRule.waitUntil {
            displayedToast?.message == "<b>Outbound 15 bus</b> at <b>Ruggles</b> added to Favorites"
        }
    }

    @Test
    @Ignore // TODO: Address flakiness
    fun testFavoritingToastFallbackText() {
        val toastVM = MockToastViewModel()
        var displayedToast: ToastViewModel.Toast? = null
        toastVM.onShowToast = { displayedToast = it }

        val busRoute = TestData.getRoute("15")
        val busStop = TestData.getStop("17861")

        composeTestRule.setContent {
            SaveFavoritesFlow(
                lineOrRoute = RouteCardData.LineOrRoute.Route(busRoute),
                stop = busStop,
                directions = listOf(direction0),
                selectedDirection = 0,
                context = EditFavoritesContext.Favorites,
                toastViewModel = toastVM,
                isFavorite = { false },
                updateFavorites = {},
                onClose = {},
            )
        }

        composeTestRule.waitUntilDefaultTimeout { displayedToast?.message == "Added to Favorites" }
    }

    @Test
    fun testFavoritingWhenDropOffOnlyPresentsDialog() {
        var onCloseCalled = false

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
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("This stop is drop-off only").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add").assertDoesNotExist()
        composeTestRule.onNodeWithText("Okay").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilDefaultTimeout { onCloseCalled }
        assertTrue(onCloseCalled)
    }

    @Test
    fun testDialogTitleFavoritesContext() {
        composeTestRule.setContent {
            SaveFavoritesFlow(
                lineOrRoute = line,
                stop = stop,
                directions = listOf(direction1),
                selectedDirection = 1,
                context = EditFavoritesContext.Favorites,
                isFavorite = { false },
                updateFavorites = {},
                onClose = {},
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Add Green Line at Boylston").assertIsDisplayed()
    }

    @Test
    fun testDialogTitleStopDetailsContext() {
        composeTestRule.setContent {
            SaveFavoritesFlow(
                lineOrRoute = line,
                stop = stop,
                directions = listOf(direction1),
                selectedDirection = 1,
                context = EditFavoritesContext.StopDetails,
                isFavorite = { false },
                updateFavorites = {},
                onClose = {},
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("Add Green Line at Boylston to Favorites")
            .assertIsDisplayed()
    }

    @Test
    fun testFavoritingWhenTwoDirectionsPresentsDialog() {
        var updateFavoritesCalledFor: Map<RouteStopDirection, Boolean> = mapOf()
        var onCloseCalled = false

        composeTestRule.setContent {
            SaveFavoritesFlow(
                lineOrRoute = line,
                stop = stop,
                directions = directions,
                selectedDirection = 0,
                context = EditFavoritesContext.Favorites,
                isFavorite = { rsd -> rsd.direction == 1 },
                updateFavorites = { updateFavoritesCalledFor = it },
                onClose = { onCloseCalled = true },
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilDefaultTimeout { onCloseCalled }
        assertTrue(onCloseCalled)
        assertEquals(
            updateFavoritesCalledFor,
            mapOf(
                RouteStopDirection(line.id, stop.id, 0) to true,
                RouteStopDirection(line.id, stop.id, 1) to true,
            ),
        )
    }
}
