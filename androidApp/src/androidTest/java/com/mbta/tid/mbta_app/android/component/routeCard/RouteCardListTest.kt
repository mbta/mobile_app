package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import io.github.dellisd.spatialk.geojson.Position
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext
import org.koin.test.KoinTest

class RouteCardListTest : KoinTest {
    val builder = ObjectCollectionBuilder()
    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
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
            lineId = "line_1"
            routePatternIds = mutableListOf("pattern_1", "pattern_2")
        }
    val routePatternOne =
        builder.routePattern(route) {
            id = "pattern_1"
            directionId = 0
            name = "Sample Route Pattern"
            routeId = "route_1"
            representativeTripId = "trip_1"
        }
    val routePatternTwo =
        builder.routePattern(route) {
            id = "pattern_2"
            directionId = 1
            name = "Sample Route Pattern Two"
            routeId = "route_1"
            representativeTripId = "trip_1"
        }
    val sampleStop =
        builder.stop {
            id = "stop_1"
            name = "Sample Stop"
            locationType = LocationType.STOP
            latitude = 0.0
            longitude = 0.0
        }
    val line =
        builder.line {
            id = "line_1"
            color = "FF0000"
            textColor = "FFFFFF"
        }
    val trip =
        builder.trip {
            id = "trip_1"
            routeId = "route_1"
            directionId = 0
            headsign = "Sample Headsign"
            routePatternId = "pattern_1"
            stopIds = listOf(sampleStop.id)
        }
    val prediction =
        builder.prediction {
            id = "prediction_1"
            revenue = true
            stopId = "stop_1"
            tripId = "trip_1"
            routeId = "route_1"
            stopSequence = 1
            directionId = 0
            arrivalTime = now.plus(1.minutes)
            departureTime = now.plus(1.5.minutes)
        }

    val greenLineRoute =
        builder.route {
            id = "route_2"
            type = RouteType.LIGHT_RAIL
            color = "008000"
            directionNames = listOf("Inbound", "Outbound")
            directionDestinations = listOf("Park Street", "Lechmere")
            longName = "Green Line Long Name"
            shortName = "Green Line"
            textColor = "FFFFFF"
            lineId = "line-Green"
            routePatternIds = mutableListOf("pattern_3", "pattern_4")
        }
    val greenLineRoutePatternOne =
        builder.routePattern(greenLineRoute) {
            id = "pattern_3"
            directionId = 0
            name = "Green Line Pattern"
            routeId = "route_2"
            representativeTripId = "trip_2"
        }
    val greenLine =
        builder.line {
            id = "line-Green"
            shortName = "Green Line"
            longName = "Green Line Long Name"
            color = "008000"
            textColor = "FFFFFF"
        }
    val greenLineStop =
        builder.stop {
            id = "stop_2"
            name = "Green Line Stop"
            locationType = LocationType.STOP
            latitude = 0.0
            longitude = 0.0
        }
    val greenLineTrip =
        builder.trip {
            id = "trip_2"
            routeId = "route_2"
            directionId = 0
            headsign = "Green Line Head Sign"
            routePatternId = "pattern_3"
            stopIds = listOf(greenLineStop.id)
        }
    val greenLinePrediction =
        builder.prediction {
            id = "prediction_2"
            revenue = true
            stopId = "stop_2"
            tripId = "trip_2"
            routeId = "route_2"
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
    fun testRouteCardListDisplaysCorrectly(): Unit = runBlocking {
        val routeCardData =
            RouteCardData.routeCardsForStopList(
                listOf(sampleStop.id, greenLineStop.id),
                globalResponse,
                Position(0.0, 0.0),
                ScheduleResponse(builder),
                predictions = PredictionsStreamDataResponse(builder),
                AlertsStreamDataResponse(emptyMap()),
                now,
                emptySet(),
                RouteCardData.Context.NearbyTransit,
            )

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                Column {
                    RouteCardList(
                        routeCardData = routeCardData,
                        emptyView = { Text("This would be the empty view") },
                        global = globalResponse,
                        now = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                        isFavorite = { false },
                        togglePinnedRoute = { _ -> },
                        showStationAccessibility = false,
                        onOpenStopDetails = { _, _ -> },
                    )
                }
            }
        }

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Sample Route"))
        composeTestRule.onNodeWithText("Sample Route").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sample Headsign").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 min").assertIsDisplayed()

        composeTestRule.onNodeWithText("Green Line Long Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Green Line Stop").assertIsDisplayed()
        composeTestRule.onNodeWithText("Green Line Head Sign").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 min").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testRouteCardListShowsEmptyView() {
        val emptyNearbyKoinApplication = testKoinApplication()
        composeTestRule.setContent {
            KoinContext(emptyNearbyKoinApplication.koin) {
                Column {
                    RouteCardList(
                        routeCardData = emptyList(),
                        emptyView = { Text("This would be the empty view") },
                        global = globalResponse,
                        now = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                        isFavorite = { false },
                        togglePinnedRoute = { _ -> },
                        showStationAccessibility = false,
                        onOpenStopDetails = { _, _ -> },
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasText("This would be the empty view")
        )
        composeTestRule.onNodeWithText("This would be the empty view").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testOpenStopDetails(): Unit = runBlocking {
        val routeCardData =
            RouteCardData.routeCardsForStopList(
                listOf(sampleStop.id),
                globalResponse,
                Position(0.0, 0.0),
                ScheduleResponse(builder),
                predictions = PredictionsStreamDataResponse(builder),
                AlertsStreamDataResponse(emptyMap()),
                now,
                emptySet(),
                RouteCardData.Context.NearbyTransit,
            )

        var clickedStopId: String? = null
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                Column {
                    RouteCardList(
                        routeCardData = routeCardData,
                        emptyView = { Text("This would be the empty view") },
                        global = globalResponse,
                        now = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                        isFavorite = { false },
                        togglePinnedRoute = { _ -> },
                        showStationAccessibility = false,
                        onOpenStopDetails = { stopId, _ -> clickedStopId = stopId },
                    )
                }
            }
        }

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Sample Route"))
        composeTestRule.onNodeWithText("Sample Route").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sample Headsign").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("1 min").assertIsDisplayed()
        assertEquals(clickedStopId, sampleStop.id)
    }
}
