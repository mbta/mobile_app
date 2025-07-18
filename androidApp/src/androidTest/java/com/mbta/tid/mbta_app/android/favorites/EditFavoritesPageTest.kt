package com.mbta.tid.mbta_app.android.favorites

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.pages.EditFavoritesPage
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.model.Favorites
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.IFavoritesRepository
import com.mbta.tid.mbta_app.repositories.MockFavoritesRepository
import com.mbta.tid.mbta_app.usecases.FavoritesUsecases
import dev.mokkery.spy
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext
import org.koin.test.KoinTest

class EditFavoritesPageTest : KoinTest {
    val builder = ObjectCollectionBuilder()
    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
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
            lineId = line.id
            routePatternIds = mutableListOf("pattern_1", "pattern_2")
        }
    val routePatternOne =
        builder.routePattern(route) {
            id = "pattern_1"
            directionId = 0
            name = "Sample Route Pattern"
            routeId = route.id
            representativeTripId = "trip_1"
        }
    val routePatternTwo =
        builder.routePattern(route) {
            id = "pattern_2"
            directionId = 1
            name = "Sample Route Pattern Two"
            routeId = route.id
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
            routeId = route.id
            directionId = 0
            headsign = "Sample Headsign"
            routePatternId = routePatternOne.id
            stopIds = listOf(sampleStop.id)
        }
    val trip2 =
        builder.trip {
            id = "trip_2"
            routeId = route.id
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
            routeId = route.id
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
            lineId = greenLine.id
            routePatternIds = mutableListOf("pattern_gl")
        }
    val greenLineRoutePatternOne =
        builder.routePattern(greenLineRoute) {
            id = "pattern_gl"
            directionId = 0
            name = "Green Line Pattern"
            routeId = greenLineRoute.id
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
            routeId = greenLineRoute.id
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
            routeId = greenLineRoute.id
            stopSequence = 1
            directionId = 0
            arrivalTime = now.plus(5.minutes)
            departureTime = now.plus(5.5.minutes)
        }

    val globalResponse = GlobalResponse(builder)

    val koinApplication = testKoinApplication(builder)

    @get:Rule val composeTestRule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFavoritesDisplayCorrectly(): Unit = runBlocking {
        val favorites = setOf(RouteStopDirection(route.id, sampleStop.id, 0))
        val repository = MockFavoritesRepository(Favorites(favorites))
        val usecase = FavoritesUsecases(repository)
        val viewModel = FavoritesViewModel(usecase)
        viewModel.favorites = favorites

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                EditFavoritesPage(globalResponse, null, viewModel) {}
            }
        }

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
        val repository = MockFavoritesRepository(Favorites(emptySet()))
        val usecase = FavoritesUsecases(repository)
        val viewModel = FavoritesViewModel(usecase)

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                EditFavoritesPage(globalResponse, null, viewModel) {}
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("No stops added"))
        composeTestRule.onNodeWithText("No stops added").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testDeleteFavorite(): Unit = runBlocking {
        val favorites =
            setOf(
                RouteStopDirection(route.id, sampleStop.id, 0),
                RouteStopDirection(greenLine.id, greenLineStop.id, 0),
            )
        val repository = MockFavoritesRepository(Favorites(favorites))
        val spiedRepo = spy<IFavoritesRepository>(repository)
        val usecase = FavoritesUsecases(spiedRepo)
        val viewModel = FavoritesViewModel(usecase)
        viewModel.favorites = favorites

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                EditFavoritesPage(globalResponse, null, viewModel) {}
            }
        }

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Sample Route"))
        composeTestRule.onNodeWithText("Sample Route").assertIsDisplayed()
        composeTestRule.onNodeWithText("Downtown").assertIsDisplayed()

        composeTestRule.onNodeWithText("Green Line Long Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Green Line Stop").assertIsDisplayed()

        composeTestRule.onAllNodesWithTag("trashCan")[0].performClick()
        composeTestRule.awaitIdle()
        verifySuspend(VerifyMode.exactly(1)) {
            spiedRepo.setFavorites(
                Favorites(setOf(RouteStopDirection(greenLine.id, greenLineStop.id, 0)))
            )
        }

        // Should be deleted
        composeTestRule.onNodeWithText("Sample Route").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("Downtown").assertIsNotDisplayed()

        // Should remain
        composeTestRule.onNodeWithText("Green Line Long Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Green Line Stop").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFavoritesOrderStopsBasedOnPosition(): Unit = runBlocking {
        val favorites =
            setOf(
                RouteStopDirection(greenLine.id, greenLineStop.id, 0),
                RouteStopDirection(route.id, farStop.id, 1),
                RouteStopDirection(route.id, sampleStop.id, 0),
            )
        val repository = MockFavoritesRepository(Favorites(favorites))
        val usecase = FavoritesUsecases(repository)
        val viewModel = FavoritesViewModel(usecase)
        viewModel.favorites = favorites

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                EditFavoritesPage(globalResponse, Position(0.0, 0.0), viewModel) {}
            }
        }

        val stops = composeTestRule.onAllNodesWithText("Stop", substring = true)
        stops[0].assertTextEquals("Sample Stop 1")
        stops[1].assertTextEquals("Sample Stop 2")
        stops[2].assertTextEquals("Green Line Stop")
        stops.assertCountEquals(3)
    }
}
