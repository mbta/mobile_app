package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.model.Favorites
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockFavoritesRepository
import com.mbta.tid.mbta_app.repositories.MockNearbyRepository
import com.mbta.tid.mbta_app.repositories.MockPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import io.github.dellisd.spatialk.geojson.Position
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext
import org.koin.test.KoinTest

class NearbyTransitViewTest : KoinTest {
    val builder = ObjectCollectionBuilder()
    val now = EasternTimeInstant.now()
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
            sortOrder = 1
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
            sortOrder = 0
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

    val koinApplication =
        testKoinApplication(
            builder,
            repositoryOverrides = {
                nearby =
                    MockNearbyRepository(
                        stopIds = listOf(sampleStop.id, greenLineStop.id),
                        response = NearbyResponse(builder),
                    )
            },
        )

    @get:Rule val composeTestRule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testNearbyTransitViewDisplaysCorrectly() {
        val errorBannerVM = ErrorBannerViewModel(false, MockErrorBannerStateRepository())
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                NearbyTransitView(
                    alertData = AlertsStreamDataResponse(emptyMap()),
                    globalResponse = globalResponse,
                    targetLocation = Position(0.0, 0.0),
                    setLastLocation = {},
                    setIsTargeting = {},
                    onOpenStopDetails = { _, _ -> },
                    noNearbyStopsView = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.onNodeWithText("Nearby Transit").assertIsDisplayed()
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
    fun testSortsPinnedRoutesToTopByDefault() {
        val errorBannerVM = ErrorBannerViewModel(false, MockErrorBannerStateRepository())

        val koinApplication =
            testKoinApplication(
                builder,
                repositoryOverrides = {
                    nearby =
                        MockNearbyRepository(
                            stopIds = listOf(sampleStop.id, greenLineStop.id),
                            response = NearbyResponse(builder),
                        )
                    pinnedRoutes = MockPinnedRoutesRepository(setOf(route.id))
                },
            )

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                NearbyTransitView(
                    alertData = AlertsStreamDataResponse(emptyMap()),
                    globalResponse = globalResponse,
                    targetLocation = Position(0.0, 0.0),
                    setLastLocation = {},
                    setIsTargeting = {},
                    onOpenStopDetails = { _, _ -> },
                    noNearbyStopsView = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.onNodeWithText("Nearby Transit").assertIsDisplayed()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Sample Route"))

        composeTestRule
            .onAllNodesWithTag("RouteCard")[0]
            .onChildren()
            .assertAny(hasText("Sample Route"))
        composeTestRule
            .onAllNodesWithTag("RouteCard")[1]
            .onChildren()
            .assertAny(hasText("Green Line Long Name"))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testDoesntSortsPinnedRoutesToTopEnhancedFavorites() {
        val errorBannerVM = ErrorBannerViewModel(false, MockErrorBannerStateRepository())

        val koinApplication =
            testKoinApplication(
                builder,
                repositoryOverrides = {
                    nearby =
                        MockNearbyRepository(
                            stopIds = listOf(sampleStop.id, greenLineStop.id),
                            response = NearbyResponse(builder),
                        )
                    settings =
                        MockSettingsRepository(settings = mapOf(Settings.EnhancedFavorites to true))
                    pinnedRoutes = MockPinnedRoutesRepository(setOf(route.id))
                },
            )

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                NearbyTransitView(
                    alertData = AlertsStreamDataResponse(emptyMap()),
                    globalResponse = globalResponse,
                    targetLocation = Position(0.0, 0.0),
                    setLastLocation = {},
                    setIsTargeting = {},
                    onOpenStopDetails = { _, _ -> },
                    noNearbyStopsView = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.onNodeWithText("Nearby Transit").assertIsDisplayed()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Sample Route"))

        composeTestRule
            .onAllNodesWithTag("RouteCard")[0]
            .onChildren()
            .assertAny(hasText("Green Line Long Name"))
        composeTestRule
            .onAllNodesWithTag("RouteCard")[1]
            .onChildren()
            .assertAny(hasText("Sample Route"))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testRouteCardAnalyticsPinnedWithoutEnhanced() {
        val errorBannerVM = ErrorBannerViewModel(false, MockErrorBannerStateRepository())

        var analyticsLoggedProps: Map<String, String> = mapOf()

        val koinApplication =
            testKoinApplication(
                builder,
                analytics = MockAnalytics({ _event, props -> analyticsLoggedProps = props }),
                repositoryOverrides = {
                    nearby =
                        MockNearbyRepository(
                            stopIds = listOf(sampleStop.id, greenLineStop.id),
                            response = NearbyResponse(builder),
                        )
                    settings =
                        MockSettingsRepository(
                            settings = mapOf(Settings.EnhancedFavorites to false)
                        )
                    pinnedRoutes = MockPinnedRoutesRepository(setOf(route.id))
                },
            )

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                NearbyTransitView(
                    alertData = AlertsStreamDataResponse(emptyMap()),
                    globalResponse = globalResponse,
                    targetLocation = Position(0.0, 0.0),
                    setLastLocation = {},
                    setIsTargeting = {},
                    onOpenStopDetails = { _, _ -> },
                    noNearbyStopsView = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.onNodeWithText("Nearby Transit").assertIsDisplayed()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Sample Route"))
        composeTestRule.onNodeWithText("Sample Headsign").performClick()

        assertEquals(analyticsLoggedProps.getValue("pinned"), "true")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testRouteCardAnalyticsPinnedWithEnhanced() {
        val errorBannerVM = ErrorBannerViewModel(false, MockErrorBannerStateRepository())

        var analyticsLoggedProps: Map<String, String> = mapOf()

        val koinApplication =
            testKoinApplication(
                builder,
                analytics = MockAnalytics({ _event, props -> analyticsLoggedProps = props }),
                repositoryOverrides = {
                    nearby =
                        MockNearbyRepository(
                            stopIds = listOf(sampleStop.id, greenLineStop.id),
                            response = NearbyResponse(builder),
                        )
                    settings =
                        MockSettingsRepository(settings = mapOf(Settings.EnhancedFavorites to true))
                    favorites =
                        MockFavoritesRepository(
                            Favorites(
                                setOf(
                                    RouteStopDirection(
                                        route.id,
                                        sampleStop.id,
                                        routePatternOne.directionId,
                                    )
                                )
                            )
                        )
                },
            )

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                NearbyTransitView(
                    alertData = AlertsStreamDataResponse(emptyMap()),
                    globalResponse = globalResponse,
                    targetLocation = Position(0.0, 0.0),
                    setLastLocation = {},
                    setIsTargeting = {},
                    onOpenStopDetails = { _, _ -> },
                    noNearbyStopsView = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.onNodeWithText("Nearby Transit").assertIsDisplayed()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Sample Route"))
        composeTestRule.onNodeWithText("Sample Headsign").performClick()

        assertEquals(analyticsLoggedProps.getValue("pinned"), "true")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testNearbyTransitViewNoNearbyStops() {
        val emptyNearbyKoinApplication = testKoinApplication()
        val errorBannerVM = ErrorBannerViewModel(false, MockErrorBannerStateRepository())
        composeTestRule.setContent {
            KoinContext(emptyNearbyKoinApplication.koin) {
                NearbyTransitView(
                    alertData = AlertsStreamDataResponse(emptyMap()),
                    globalResponse = globalResponse,
                    targetLocation = Position(0.0, 0.0),
                    setLastLocation = {},
                    setIsTargeting = {},
                    onOpenStopDetails = { _, _ -> },
                    noNearbyStopsView = { Text("This would be the no nearby stops view") },
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasText("This would be the no nearby stops view")
        )
        composeTestRule.onNodeWithText("This would be the no nearby stops view").assertIsDisplayed()
    }
}
