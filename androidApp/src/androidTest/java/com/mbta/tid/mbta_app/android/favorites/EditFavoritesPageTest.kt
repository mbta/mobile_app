package com.mbta.tid.mbta_app.android.favorites

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.android.pages.EditFavoritesPage
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.FavoritesViewModel
import com.mbta.tid.mbta_app.viewModel.MockFavoritesViewModel
import com.mbta.tid.mbta_app.viewModel.MockToastViewModel
import com.mbta.tid.mbta_app.viewModel.ToastViewModel
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest

class EditFavoritesPageTest : KoinTest {
    val builder = ObjectCollectionBuilder()
    val now = EasternTimeInstant.now()
    val line =
        builder.line {
            id = "line_1"
            color = "FF0000"
            textColor = "FFFFFF"
        }
    val route =
        builder.route {
            id = "route_1"
            type = RouteType.BUS
            color = "FF0000"
            directionNames = listOf("North", "South")
            directionDestinations = listOf("Downtown", "Uptown")
            longName = "Sample Route Long Name"
            shortName = "Sample Route"
            textColor = "000000"
            lineId = line.id.idText
            routePatternIds = mutableListOf("pattern_1", "pattern_2")
        }
    val lineOrRoute = LineOrRoute.Route(route)
    val routePatternOne =
        builder.routePattern(route) {
            id = "pattern_1"
            directionId = 0
            name = "Sample Route Pattern"
            routeId = route.id.idText
            representativeTripId = "trip_1"
        }
    val routePatternTwo =
        builder.routePattern(route) {
            id = "pattern_2"
            directionId = 1
            name = "Sample Route Pattern Two"
            routeId = route.id.idText
            representativeTripId = "trip_2"
        }
    val sampleStop =
        builder.stop {
            id = "stop_1"
            name = "Sample Stop 1"
            locationType = LocationType.STOP
            latitude = 0.0
            longitude = 0.0
        }
    val farStop =
        builder.stop {
            id = "stop_2"
            name = "Sample Stop 2"
            locationType = LocationType.STOP
            latitude = 1.0
            longitude = 1.0
        }
    val trip1 =
        builder.trip {
            id = "trip_1"
            routeId = route.id.idText
            directionId = 0
            headsign = "Sample Headsign"
            routePatternId = routePatternOne.id
            stopIds = listOf(sampleStop.id)
        }
    val trip2 =
        builder.trip {
            id = "trip_2"
            routeId = route.id.idText
            directionId = 1
            headsign = "Other Headsign"
            routePatternId = routePatternTwo.id
            stopIds = listOf(farStop.id)
        }
    val prediction =
        builder.prediction {
            id = "prediction_1"
            revenue = true
            stopId = sampleStop.id
            tripId = trip1.id
            routeId = route.id.idText
            stopSequence = 1
            directionId = 0
            arrivalTime = now.plus(1.minutes)
            departureTime = now.plus(1.5.minutes)
        }

    val greenLine =
        builder.line {
            id = "line-Green"
            shortName = "Green Line"
            longName = "Green Line Long Name"
            color = "008000"
            textColor = "FFFFFF"
        }
    val greenLineRoute =
        builder.route {
            id = "route_gl"
            type = RouteType.LIGHT_RAIL
            color = "008000"
            directionNames = listOf("Inbound", "Outbound")
            directionDestinations = listOf("Park Street", "Lechmere")
            longName = "Green Line Long Name"
            shortName = "Green Line"
            textColor = "FFFFFF"
            lineId = greenLine.id.idText
            routePatternIds = mutableListOf("pattern_gl")
        }
    val greenLineOrRoute = LineOrRoute.Line(greenLine, setOf(greenLineRoute))
    val greenLineRoutePatternOne =
        builder.routePattern(greenLineRoute) {
            id = "pattern_gl"
            directionId = 0
            name = "Green Line Pattern"
            routeId = greenLineRoute.id.idText
            representativeTripId = "trip_gl"
        }
    val greenLineStop =
        builder.stop {
            id = "stop_gl"
            name = "Green Line Stop"
            locationType = LocationType.STOP
            latitude = 2.0
            longitude = 2.0
        }
    val greenLineTrip =
        builder.trip {
            id = "trip_gl"
            routeId = greenLineRoute.id.idText
            directionId = 0
            headsign = "Green Line Head Sign"
            routePatternId = greenLineRoutePatternOne.id
            stopIds = listOf(greenLineStop.id)
        }
    val greenLinePrediction =
        builder.prediction {
            id = "prediction_gl"
            revenue = true
            stopId = greenLineStop.id
            tripId = greenLineTrip.id
            routeId = greenLineRoute.id.idText
            stopSequence = 1
            directionId = 0
            arrivalTime = now.plus(5.minutes)
            departureTime = now.plus(5.5.minutes)
        }

    val routeCard =
        RouteCardData(
            lineOrRoute,
            listOf(
                RouteCardData.RouteStopData(
                    lineOrRoute,
                    sampleStop,
                    listOf(Direction(0, route)),
                    listOf(
                        RouteCardData.Leaf(
                            lineOrRoute,
                            sampleStop,
                            0,
                            listOf(routePatternOne),
                            setOf(sampleStop.id),
                            listOf(UpcomingTrip(trip1, prediction)),
                            emptyList(),
                            true,
                            true,
                            emptyList(),
                            RouteCardData.Context.Favorites,
                        )
                    ),
                )
            ),
            now,
        )

    val greenLineRouteCard =
        RouteCardData(
            greenLineOrRoute,
            listOf(
                RouteCardData.RouteStopData(
                    greenLineOrRoute,
                    greenLineStop,
                    listOf(Direction(0, greenLineRoute)),
                    listOf(
                        RouteCardData.Leaf(
                            greenLineOrRoute,
                            greenLineStop,
                            0,
                            listOf(greenLineRoutePatternOne),
                            setOf(greenLineStop.id),
                            listOf(UpcomingTrip(greenLineTrip, greenLinePrediction)),
                            emptyList(),
                            true,
                            true,
                            emptyList(),
                            RouteCardData.Context.Favorites,
                        )
                    ),
                )
            ),
            now,
        )

    val routeCardData = listOf(routeCard)
    val greenLineRouteCardData = listOf(greenLineRouteCard)
    val combinedRouteCardData = listOf(routeCard, greenLineRouteCard)

    val globalResponse = GlobalResponse(builder)

    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        loadKoinMocks(builder)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFavoritesDisplayCorrectly(): Unit = runBlocking {
        val favorites = mapOf(RouteStopDirection(route.id, sampleStop.id, 0) to FavoriteSettings())
        val viewModel =
            MockFavoritesViewModel(
                FavoritesViewModel.State(
                    false,
                    favorites,
                    false,
                    routeCardData,
                    routeCardData,
                    null,
                )
            )

        composeTestRule.setContent { EditFavoritesPage(globalResponse, viewModel) {} }

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Sample Route"))
        composeTestRule.onNodeWithText("Sample Route").assertIsDisplayed()
        composeTestRule.onNodeWithText("Downtown").assertIsDisplayed()

        // Shouldn't be shown because it is not a favorite
        composeTestRule.onNodeWithText("Green Line Long Name").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("Green Line Stop").assertIsNotDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testShowsEmptyView() {
        val viewModel =
            MockFavoritesViewModel(
                FavoritesViewModel.State(false, emptyMap(), false, emptyList(), emptyList(), null)
            )

        composeTestRule.setContent { EditFavoritesPage(globalResponse, viewModel) {} }

        composeTestRule.waitForIdle()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("No stops added"))
        composeTestRule.onNodeWithText("No stops added").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testDeleteFavorite(): Unit = runBlocking {
        val favorites =
            mapOf(
                RouteStopDirection(route.id, sampleStop.id, 0) to FavoriteSettings(),
                RouteStopDirection(greenLine.id, greenLineStop.id, 0) to FavoriteSettings(),
            )
        var updatedWith: Map<RouteStopDirection, FavoriteSettings?>? = null

        val viewModel =
            MockFavoritesViewModel(
                FavoritesViewModel.State(
                    false,
                    favorites,
                    false,
                    combinedRouteCardData,
                    combinedRouteCardData,
                    null,
                )
            )

        viewModel.onUpdateFavorites = { update ->
            viewModel.models.update {
                FavoritesViewModel.State(
                    false,
                    mapOf(
                        RouteStopDirection(greenLine.id, greenLineStop.id, 0) to FavoriteSettings()
                    ),
                    false,
                    greenLineRouteCardData,
                    greenLineRouteCardData,
                    null,
                )
            }
            updatedWith = update
        }

        composeTestRule.setContent { EditFavoritesPage(globalResponse, viewModel) {} }

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Sample Route"))
        composeTestRule.onNodeWithText("Sample Route").assertIsDisplayed()
        composeTestRule.onNodeWithText("Downtown").assertIsDisplayed()

        composeTestRule.onNodeWithText("Green Line Long Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Green Line Stop").assertIsDisplayed()

        composeTestRule.onAllNodesWithTag("trashCan")[0].performClick()
        composeTestRule.awaitIdle()

        val update = mapOf(RouteStopDirection(route.id, sampleStop.id, 0) to null)
        verifySuspend(VerifyMode.exactly(1)) {
            viewModel.updateFavorites(update, EditFavoritesContext.Favorites, 0, null, false)
        }

        composeTestRule.waitUntilDefaultTimeout { update == updatedWith }

        // Should be deleted
        composeTestRule.onNodeWithText("Sample Route").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("Downtown").assertIsNotDisplayed()

        // Should remain
        composeTestRule.onNodeWithText("Green Line Long Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Green Line Stop").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testToastUndoDeleteFavorite(): Unit = runBlocking {
        val favorites =
            mapOf(
                RouteStopDirection(route.id, sampleStop.id, 0) to FavoriteSettings(),
                RouteStopDirection(greenLine.id, greenLineStop.id, 0) to FavoriteSettings(),
            )
        var updatedWith: Map<RouteStopDirection, FavoriteSettings?>? = null

        val viewModel =
            MockFavoritesViewModel(
                FavoritesViewModel.State(
                    false,
                    favorites,
                    false,
                    combinedRouteCardData,
                    combinedRouteCardData,
                    null,
                )
            )

        viewModel.onUpdateFavorites = { update ->
            val updatedFavorites =
                favorites.filterNot { update.containsKey(it.key) && update[it.key] == null }
            viewModel.models.update {
                FavoritesViewModel.State(
                    false,
                    updatedFavorites,
                    false,
                    if (updatedFavorites.size == 1) greenLineRouteCardData
                    else combinedRouteCardData,
                    if (updatedFavorites.size == 1) greenLineRouteCardData
                    else combinedRouteCardData,
                    null,
                )
            }
            updatedWith = update
        }

        val toastVM = MockToastViewModel()
        var displayedToast: ToastViewModel.Toast? = null
        toastVM.onHideToast = { displayedToast = null }
        toastVM.onShowToast = { displayedToast = it }

        composeTestRule.setContent { EditFavoritesPage(globalResponse, viewModel, toastVM) {} }

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Sample Route"))
        composeTestRule.onNodeWithText("Green Line Long Name").assertIsDisplayed()

        composeTestRule.onAllNodesWithTag("trashCan")[0].performClick()
        composeTestRule.awaitIdle()

        val update = mapOf(RouteStopDirection(route.id, sampleStop.id, 0) to null)
        verifySuspend(VerifyMode.exactly(1)) {
            viewModel.updateFavorites(update, EditFavoritesContext.Favorites, 0, null, false)
        }

        composeTestRule.waitUntilDefaultTimeout { update == updatedWith }

        composeTestRule.onNodeWithText("Sample Route").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("Green Line Long Name").assertIsDisplayed()
        assertEquals(
            "<b>Northbound Sample Route bus</b> at <b>Sample Stop 1</b> removed from Favorites",
            displayedToast?.message,
        )

        val customAction = displayedToast?.action as ToastViewModel.ToastAction.Custom
        assertEquals("Undo", customAction.actionLabel)
        customAction.onAction()

        val undo = mapOf(RouteStopDirection(route.id, sampleStop.id, 0) to FavoriteSettings())
        verifySuspend(VerifyMode.exactly(1)) {
            viewModel.updateFavorites(undo, EditFavoritesContext.Favorites, 0, null, false)
        }
        composeTestRule.waitUntilDefaultTimeout { undo == updatedWith }

        composeTestRule.onNodeWithText("Sample Route").assertIsDisplayed()
        composeTestRule.onNodeWithText("Green Line Long Name").assertIsDisplayed()
        assertNull(displayedToast)
    }
}
