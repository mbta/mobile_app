package com.mbta.tid.mbta_app.android.routeStop

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.android.testUtils.assertCanBeDisplayed
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopFilter
import com.mbta.tid.mbta_app.model.RouteStopPageFilters
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.WheelchairBoardingStatus
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.NavigationCallbacks
import com.mbta.tid.mbta_app.viewModel.IStopCentricViewModel
import com.mbta.tid.mbta_app.viewModel.MockStopCentricViewModel
import com.mbta.tid.mbta_app.viewModel.StopCentricViewModel
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.compose.koinInject
import org.koin.core.context.loadKoinModules
import org.koin.dsl.bind
import org.koin.dsl.module

class RouteStopViewTest {
    val builder = ObjectCollectionBuilder()
    val now = EasternTimeInstant.now()
    val route = builder.route {
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
    val lineOrRoute = LineOrRoute.Route(route)
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
    val downstreamStop = builder.stop {
        id = "stop_2"
        name = "Sample Stop 2"
        locationType = LocationType.STOP
        latitude = 0.0
        longitude = 0.0
    }
    val stop = builder.stop {
        id = "stop_1"
        name = "Sample Stop"
        locationType = LocationType.STOP
        latitude = 0.0
        longitude = 0.0
    }
    val inaccessibleStop = builder.stop {
        id = "stop_3"
        name = "Sample Stop 3"
        locationType = LocationType.STOP
        latitude = 0.0
        longitude = 0.0
        wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE
    }
    val line = builder.line {
        id = "line_1"
        color = "FF0000"
        textColor = "FFFFFF"
    }
    val trip = builder.trip {
        id = "trip_1"
        routeId = "route_1"
        directionId = 0
        headsign = "Sample Headsign"
        routePatternId = "pattern_1"
        stopIds = listOf(stop.id, downstreamStop.id, inaccessibleStop.id)
    }
    val prediction = builder.prediction {
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

    val settingsRepository = MockSettingsRepository(mapOf(Settings.StationAccessibility to true))

    @get:Rule val composeTestRule = createComposeRule(effectContext = UnconfinedTestDispatcher())

    @Before
    fun setUp() {
        loadKoinMocks(builder) { settings = settingsRepository }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testDisplaysCorrectly() {
        val vm =
            MockStopCentricViewModel(
                StopCentricViewModel.State(
                    StopCentricViewModel.RouteData.RouteStop(
                        RouteStopPageFilters(stop.id, RouteStopFilter(route.id, 0), null),
                        RouteCardData.RouteStopData(
                            route,
                            stop,
                            listOf(
                                RouteCardData.Leaf(
                                    lineOrRoute,
                                    stop,
                                    Direction(0, route),
                                    listOf(routePatternOne),
                                    setOf(stop.id),
                                    listOf(UpcomingTrip(trip, prediction)),
                                    alertsHere = emptyList(),
                                    allDataLoaded = false,
                                    hasSchedulesToday = true,
                                    subwayServiceStartTime = null,
                                    alertsDownstream = emptyList(),
                                    context = RouteCardData.Context.RouteStop,
                                ),
                            ),
                        ),
                    ),
                ),
            )

        loadKoinModules(module { single { vm }.bind(IStopCentricViewModel::class) })

        composeTestRule.setContent {
            val filterState = remember {
                mutableStateOf(RouteStopFilter(route.id, 0))
            }
            RouteStopView(
                stopId = stop.id,
                isFavorite = { false },
                routeStopFilter = filterState.value,
                tripFilter = null,
                allAlerts = null,
                now = now,
                navCallbacks =
                    NavigationCallbacks(
                        onBack = null,
                        onClose = {},
                        backButtonPresentation =
                            NavigationCallbacks.BackButtonPresentation.Floating,
                    ),
                updateRouteStopFilter = filterState::value::set,
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                errorBannerViewModel = koinInject(),
                openModal = {},
                openSheetRoute = {},
            )
        }

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("at Sample Stop"))

        composeTestRule.onNodeWithContentDescription("Star route").assertExists()
        composeTestRule.onNodeWithContentDescription("Close").assertExists()
        composeTestRule.onNode(hasText("at Sample Stop") and isHeading()).assertCanBeDisplayed()

        composeTestRule.onNodeWithText("1 min").assertExists()
    }

    @Test
    fun testDisplaysElevatorAlerts() {
        val alert = builder.alert {
            effect = Alert.Effect.ElevatorClosure
            header = "Elevator alert header"
            informedEntity()
        }

        val global = GlobalResponse(builder)
        val vm =
            MockStopCentricViewModel(
                StopCentricViewModel.State(
                    StopCentricViewModel.RouteData.RouteStop(
                        RouteStopPageFilters(stop.id, RouteStopFilter(route.id, 0), null),
                        RouteCardData.RouteStopData(
                            route,
                            stop,
                            listOf(
                                RouteCardData.Leaf(
                                    lineOrRoute,
                                    stop,
                                    Direction(0, route),
                                    listOf(routePatternOne),
                                    setOf(stop.id),
                                    listOf(UpcomingTrip(trip, prediction)),
                                    alertsHere = listOf(alert),
                                    allDataLoaded = false,
                                    hasSchedulesToday = true,
                                    subwayServiceStartTime = null,
                                    alertsDownstream = emptyList(),
                                    context = RouteCardData.Context.RouteStop,
                                ),
                            ),
                        ),
                    ),
                ),
            )
        loadKoinModules(module { single { vm }.bind(IStopCentricViewModel::class) })
        composeTestRule.setContent {
            val filterState = remember {
                mutableStateOf(RouteStopFilter(route.id, 0))
            }
            RouteStopView(
                stopId = stop.id,
                routeStopFilter = filterState.value,
                tripFilter = null,
                allAlerts = AlertsStreamDataResponse(builder),
                now = now,
                isFavorite = { false },
                navCallbacks = NavigationCallbacks.empty,
                updateRouteStopFilter = filterState::value::set,
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                openModal = {},
                openSheetRoute = {},
                errorBannerViewModel = koinInject(),
            )
        }

        composeTestRule.onNodeWithText(alert.header!!).assertExists()
    }

    @Test
    fun testTappingTripSetsFilter() = runBlocking {
        var tripFilter: TripDetailsFilter? = null

        val filterState = RouteStopFilter(routeId = route.id, directionId = 0)

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
                    context = RouteCardData.Context.RouteStop,
                ),
            )
        val routeStopData = routeCardData.single().stopData.single()

        composeTestRule.setContent {
            RouteStopView(
                stopId = stop.id,
                routeStopFilter = filterState,
                tripFilter = null,
                allAlerts = null,
                now = now,
                errorBannerViewModel = koinInject(),
                updateRouteStopFilter = {},
                updateTripFilter = { tripFilter = it },
                tileScrollState = rememberScrollState(),
                isFavorite = { false },
                openModal = {},
                openSheetRoute = {},
                navCallbacks = NavigationCallbacks.empty,
                stopCentricViewModel =
                    MockStopCentricViewModel(
                        StopCentricViewModel.State(
                            StopCentricViewModel.RouteData.RouteStop(
                                RouteStopPageFilters(stop.id, filterState, null),
                                routeStopData,
                            ),
                        ),
                    ),
            )
        }

        composeTestRule.onNodeWithText("at ${stop.name}").assertExists()
        composeTestRule.onNodeWithText("1 min").assertExists().performClick()
        composeTestRule.waitUntilDefaultTimeout { tripFilter?.tripId == trip.id }

        assertEquals(tripFilter?.tripId, trip.id)
    }

    @Test
    fun testShowsNotAccessibleAlert(): Unit = runBlocking {
        val filterState = RouteStopFilter(routeId = route.id, directionId = 0)

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
                    RouteCardData.Context.RouteStop,
                ),
            )
        val routeStopData = routeCardData.single().stopData.single()

        composeTestRule.setContent {
            RouteStopView(
                stopId = inaccessibleStop.id,
                routeStopFilter = filterState,
                tripFilter = null,
                allAlerts = null,
                now = now,
                errorBannerViewModel = koinInject(),
                updateRouteStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                isFavorite = { false },
                openModal = {},
                openSheetRoute = {},
                navCallbacks = NavigationCallbacks.empty,
                stopCentricViewModel =
                    MockStopCentricViewModel(
                        StopCentricViewModel.State(
                            StopCentricViewModel.RouteData.RouteStop(
                                RouteStopPageFilters(stop.id, filterState, null),
                                routeStopData,
                            ),
                        ),
                    ),
            )
        }

        composeTestRule.onNodeWithText("This stop is not accessible").assertCanBeDisplayed()
    }
}
