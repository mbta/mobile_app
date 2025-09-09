package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.WheelchairBoardingStatus
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.compose.koinInject

class StopDetailsFilteredPickerViewTest {
    val builder = ObjectCollectionBuilder()
    val now = EasternTimeInstant.now()
    val route =
        builder.route {
            id = "route_1"
            type = RouteType.LIGHT_RAIL
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
    val downstreamStop =
        builder.stop {
            id = "stop_2"
            name = "Sample Stop 2"
            locationType = LocationType.STOP
            latitude = 0.0
            longitude = 0.0
        }
    val stop =
        builder.stop {
            id = "stop_1"
            name = "Sample Stop"
            locationType = LocationType.STOP
            latitude = 0.0
            longitude = 0.0
        }
    val inaccessibleStop =
        builder.stop {
            id = "stop_3"
            name = "Sample Stop 3"
            locationType = LocationType.STOP
            latitude = 0.0
            longitude = 0.0
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE
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
            stopIds = listOf(stop.id, downstreamStop.id, inaccessibleStop.id)
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

    private val globalResponse =
        GlobalResponse(
            builder,
            mutableMapOf(
                stop.id to listOf(routePatternOne.id, routePatternTwo.id),
                inaccessibleStop.id to listOf(routePatternOne.id, routePatternTwo.id),
            ),
        )

    private val settings = mutableMapOf<Settings, Boolean>()
    private val settingsRepository =
        object : ISettingsRepository {
            override suspend fun getSettings() = settings

            override suspend fun setSettings(settings: Map<Settings, Boolean>) {}
        }

    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        loadKoinMocks(builder) { settings = settingsRepository }
        settings.clear()
    }

    @Test
    fun testStopDetailsRouteViewDisplaysCorrectly(): Unit = runBlocking {
        val filterState = StopDetailsFilter(routeId = route.id, directionId = 0)
        val routeCardData =
            checkNotNull(
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id),
                    globalResponse,
                    null,
                    null,
                    PredictionsStreamDataResponse(builder),
                    AlertsStreamDataResponse(emptyMap()),
                    now,
                    context = RouteCardData.Context.StopDetailsFiltered,
                )
            )
        val routeStopData = routeCardData.single().stopData.single()

        composeTestRule.setContent {
            StopDetailsFilteredPickerView(
                stopId = stop.id,
                stopFilter = filterState,
                tripFilter = null,
                routeStopData = routeStopData,
                allAlerts = null,
                now = now,
                errorBannerViewModel = koinInject(),
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                isFavorite = { false },
                updateFavorites = { _, _ -> },
                openModal = {},
                openSheetRoute = {},
                onClose = {},
            )
        }

        composeTestRule.onNodeWithText("at ${stop.name}").assertExists()
        composeTestRule.onNodeWithText("1 min").assertExists()
    }

    @Test
    fun testTappingTripSetsFilter() = runBlocking {
        var tripFilter: TripDetailsFilter? = null

        val filterState = StopDetailsFilter(routeId = route.id, directionId = 0)

        val routeCardData =
            checkNotNull(
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id),
                    globalResponse,
                    null,
                    null,
                    PredictionsStreamDataResponse(builder),
                    AlertsStreamDataResponse(emptyMap()),
                    now,
                    context = RouteCardData.Context.StopDetailsFiltered,
                )
            )
        val routeStopData = routeCardData.single().stopData.single()

        composeTestRule.setContent {
            StopDetailsFilteredPickerView(
                stopId = stop.id,
                stopFilter = filterState,
                tripFilter = null,
                routeStopData = routeStopData,
                allAlerts = null,
                now = now,
                errorBannerViewModel = koinInject(),
                updateStopFilter = {},
                updateTripFilter = { tripFilter = it },
                tileScrollState = rememberScrollState(),
                isFavorite = { false },
                updateFavorites = { _, _ -> },
                openModal = {},
                openSheetRoute = {},
                onClose = {},
            )
        }

        composeTestRule.onNodeWithText("at ${stop.name}").assertExists()
        composeTestRule.onNodeWithText("1 min").assertExists().performClick()
        composeTestRule.waitUntil { tripFilter?.tripId == trip.id }

        assertEquals(tripFilter?.tripId, trip.id)
    }

    @Test
    fun testShowsElevatorAlert(): Unit = runBlocking {
        settings[Settings.StationAccessibility] = true
        builder.alert {
            effect = Alert.Effect.ElevatorClosure
            header = "Elevator Alert Header"
            informedEntity(listOf(Alert.InformedEntity.Activity.UsingWheelchair), stop = stop.id)
            activePeriod(EasternTimeInstant(Instant.DISTANT_PAST), null)
        }

        val filterState = StopDetailsFilter(routeId = route.id, directionId = 0)

        val routeCardData =
            checkNotNull(
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id),
                    globalResponse,
                    null,
                    null,
                    PredictionsStreamDataResponse(builder),
                    AlertsStreamDataResponse(builder),
                    now,
                    context = RouteCardData.Context.StopDetailsFiltered,
                )
            )
        val routeStopData = routeCardData.single().stopData.single()

        composeTestRule.setContent {
            StopDetailsFilteredPickerView(
                stopId = stop.id,
                stopFilter = filterState,
                tripFilter = null,
                routeStopData = routeStopData,
                allAlerts = null,
                now = now,
                errorBannerViewModel = koinInject(),
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                isFavorite = { false },
                updateFavorites = { _, _ -> },
                openModal = {},
                openSheetRoute = {},
                onClose = {},
            )
        }

        composeTestRule.onNodeWithText("Elevator Alert Header").assertIsDisplayed()
    }

    @Test
    fun testShowsNotAccessibleAlert(): Unit = runBlocking {
        settings[Settings.StationAccessibility] = true
        val filterState = StopDetailsFilter(routeId = route.id, directionId = 0)

        val routeCardData =
            checkNotNull(
                RouteCardData.routeCardsForStopList(
                    listOf(inaccessibleStop.id),
                    globalResponse,
                    null,
                    null,
                    PredictionsStreamDataResponse(builder),
                    AlertsStreamDataResponse(emptyMap()),
                    now,
                    RouteCardData.Context.StopDetailsFiltered,
                )
            )
        val routeStopData = routeCardData.single().stopData.single()

        composeTestRule.setContent {
            StopDetailsFilteredPickerView(
                stopId = inaccessibleStop.id,
                stopFilter = filterState,
                tripFilter = null,
                routeStopData = routeStopData,
                allAlerts = null,
                now = now,
                errorBannerViewModel = koinInject(),
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                isFavorite = { false },
                updateFavorites = { _, _ -> },
                openModal = {},
                openSheetRoute = {},
                onClose = {},
            )
        }

        composeTestRule.onNodeWithText("This stop is not accessible").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testStarSavesEnhancedFavoritesWithDialogBehindFlag(): Unit = runBlocking {
        val filterState = StopDetailsFilter(routeId = route.id, directionId = 0)

        val routeCardData =
            checkNotNull(
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id),
                    globalResponse,
                    null,
                    null,
                    PredictionsStreamDataResponse(builder),
                    AlertsStreamDataResponse(emptyMap()),
                    now,
                    RouteCardData.Context.StopDetailsFiltered,
                )
            )
        val routeStopData = routeCardData.single().stopData.single()

        var updatedFavorites: Pair<Map<RouteStopDirection, Boolean>, Int>? = null

        composeTestRule.setContent {
            StopDetailsFilteredPickerView(
                stopId = stop.id,
                stopFilter = filterState,
                tripFilter = null,
                routeStopData = routeStopData,
                allAlerts = null,
                now = now,
                errorBannerViewModel = koinInject(),
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                isFavorite = { false },
                updateFavorites = { favMap, direction ->
                    updatedFavorites = Pair(favMap, direction)
                },
                openModal = {},
                openSheetRoute = {},
                onClose = {},
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Star route")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Add"))

        composeTestRule.onNodeWithText("Add").performClick()

        composeTestRule.waitUntil {
            updatedFavorites ==
                Pair(
                    mapOf(
                        RouteStopDirection(route.id, stop.id, 0) to true,
                        RouteStopDirection(route.id, stop.id, 1) to false,
                    ),
                    0,
                )
        }
    }

    @Test
    fun testUnfavoriteWithoutDialogBehindFlag(): Unit = runBlocking {
        val filterState = StopDetailsFilter(routeId = route.id, directionId = 0)

        val routeCardData =
            checkNotNull(
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id),
                    globalResponse,
                    null,
                    null,
                    PredictionsStreamDataResponse(builder),
                    AlertsStreamDataResponse(emptyMap()),
                    now,
                    RouteCardData.Context.StopDetailsFiltered,
                )
            )
        val routeStopData = routeCardData.single().stopData.single()

        var updatedFavorites: Pair<Map<RouteStopDirection, Boolean>, Int>? = null

        composeTestRule.setContent {
            StopDetailsFilteredPickerView(
                stopId = stop.id,
                stopFilter = filterState,
                tripFilter = null,
                routeStopData = routeStopData,
                allAlerts = null,
                now = now,
                errorBannerViewModel = koinInject(),
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                isFavorite = { true },
                updateFavorites = { favMap, direction ->
                    updatedFavorites = Pair(favMap, direction)
                },
                openModal = {},
                openSheetRoute = {},
                onClose = {},
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Star route")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Add").assertDoesNotExist()

        composeTestRule.waitUntil {
            updatedFavorites == Pair(mapOf(RouteStopDirection(route.id, stop.id, 0) to false), 0)
        }
    }
}
