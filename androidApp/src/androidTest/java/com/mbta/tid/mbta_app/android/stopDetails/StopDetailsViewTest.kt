package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext

class StopDetailsViewTest {
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
    val lineOrRoute = RouteCardData.LineOrRoute.Route(route)
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
    val stop =
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
            stopIds = listOf(stop.id)
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

    val settingsRepository =
        MockSettingsRepository(settings = mapOf(Pair(Settings.StationAccessibility, true)))

    val koinApplication = testKoinApplication(builder) { settings = settingsRepository }

    @get:Rule val composeTestRule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testStopDetailsViewDisplaysUnfilteredCorrectly() {
        val global = GlobalResponse(builder)
        val viewModel = StopDetailsViewModel.mocked()

        viewModel.setGlobalResponse(global)
        viewModel.setUnfilteredRouteCardData(
            listOf(
                RouteCardData(
                    lineOrRoute,
                    listOf(
                        RouteCardData.RouteStopData(
                            route,
                            stop,
                            listOf(
                                RouteCardData.Leaf(
                                    lineOrRoute,
                                    stop,
                                    directionId = 0,
                                    listOf(routePatternOne),
                                    setOf(stop.id),
                                    listOf(UpcomingTrip(trip, prediction)),
                                    alertsHere = emptyList(),
                                    allDataLoaded = false,
                                    hasSchedulesToday = true,
                                    alertsDownstream = emptyList(),
                                    context = RouteCardData.Context.StopDetailsUnfiltered,
                                )
                            ),
                            global,
                        )
                    ),
                    now,
                )
            )
        )

        val errorBannerVM = ErrorBannerViewModel(false, MockErrorBannerStateRepository())
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                val filterState = remember { mutableStateOf<StopDetailsFilter?>(null) }
                StopDetailsView(
                    stopId = stop.id,
                    viewModel = viewModel,
                    isFavorite = { false },
                    updateFavorites = {},
                    onClose = {},
                    stopFilter = null,
                    tripFilter = null,
                    allAlerts = null,
                    updateStopFilter = filterState::value::set,
                    updateTripDetailsFilter = {},
                    tileScrollState = rememberScrollState(),
                    errorBannerViewModel = errorBannerVM,
                    openModal = {},
                    openSheetRoute = {},
                )
            }
        }

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Sample Stop"))

        composeTestRule.onNode(hasText("Sample Stop") and isHeading()).assertIsDisplayed()

        composeTestRule.onNodeWithText("Sample Route").assertExists()
        composeTestRule.onNodeWithText("Sample Headsign").assertExists()
        composeTestRule.onNodeWithText("1 min").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testStopDetailsViewDisplaysFilteredCorrectly() {
        val viewModel = StopDetailsViewModel.mocked()

        viewModel.setFilteredRouteStopData(
            RouteCardData.RouteStopData(
                route,
                stop,
                listOf(
                    RouteCardData.Leaf(
                        lineOrRoute,
                        stop,
                        directionId = 0,
                        listOf(routePatternOne),
                        setOf(stop.id),
                        listOf(UpcomingTrip(trip, prediction)),
                        alertsHere = emptyList(),
                        allDataLoaded = false,
                        hasSchedulesToday = true,
                        alertsDownstream = emptyList(),
                        context = RouteCardData.Context.StopDetailsFiltered,
                    )
                ),
                GlobalResponse(builder),
            )
        )
        val errorBannerViewModel = ErrorBannerViewModel(false, MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                val filterState = remember {
                    mutableStateOf<StopDetailsFilter?>(StopDetailsFilter(route.id, 0))
                }
                StopDetailsView(
                    stopId = stop.id,
                    viewModel = viewModel,
                    isFavorite = { false },
                    updateFavorites = {},
                    onClose = {},
                    stopFilter = filterState.value,
                    tripFilter = null,
                    allAlerts = null,
                    updateStopFilter = filterState::value::set,
                    updateTripDetailsFilter = {},
                    tileScrollState = rememberScrollState(),
                    errorBannerViewModel = errorBannerViewModel,
                    openModal = {},
                    openSheetRoute = {},
                )
            }
        }

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("at Sample Stop"))

        composeTestRule.onNodeWithContentDescription("Star route").assertExists()
        composeTestRule.onNodeWithContentDescription("Close").assertExists()
        composeTestRule.onNode(hasText("at Sample Stop") and isHeading()).assertIsDisplayed()

        composeTestRule.onNodeWithText("1 min").assertExists()
    }

    @Test
    fun testStopDetailsViewDisplaysElevatorAlertsOnUnfiltered() {
        val alert =
            builder.alert {
                effect = Alert.Effect.ElevatorClosure
                header = "Elevator alert header"
            }

        val global = GlobalResponse(builder)
        val viewModel = StopDetailsViewModel.mocked()

        viewModel.setGlobalResponse(global)
        viewModel.setUnfilteredRouteCardData(
            listOf(
                RouteCardData(
                    lineOrRoute,
                    listOf(
                        RouteCardData.RouteStopData(
                            route,
                            stop,
                            listOf(
                                RouteCardData.Leaf(
                                    lineOrRoute,
                                    stop,
                                    directionId = 0,
                                    listOf(routePatternOne),
                                    setOf(stop.id),
                                    listOf(UpcomingTrip(trip, prediction)),
                                    alertsHere = listOf(alert),
                                    allDataLoaded = false,
                                    hasSchedulesToday = true,
                                    alertsDownstream = emptyList(),
                                    RouteCardData.Context.StopDetailsUnfiltered,
                                )
                            ),
                            global,
                        )
                    ),
                    now,
                )
            )
        )
        val errorBannerViewModel = ErrorBannerViewModel(false, MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                val filterState = remember { mutableStateOf<StopDetailsFilter?>(null) }
                StopDetailsView(
                    stopId = stop.id,
                    viewModel = viewModel,
                    isFavorite = { false },
                    updateFavorites = {},
                    onClose = {},
                    stopFilter = filterState.value,
                    tripFilter = null,
                    allAlerts = null,
                    updateStopFilter = filterState::value::set,
                    updateTripDetailsFilter = {},
                    tileScrollState = rememberScrollState(),
                    errorBannerViewModel = errorBannerViewModel,
                    openModal = {},
                    openSheetRoute = {},
                )
            }
        }

        composeTestRule.onNodeWithText(alert.header!!).assertExists()
    }

    @Test
    fun testStopDetailsViewDisplaysElevatorAlertsOnFiltered() {
        val alert =
            builder.alert {
                effect = Alert.Effect.ElevatorClosure
                header = "Elevator alert header"
            }

        val viewModel = StopDetailsViewModel.mocked()

        viewModel.setFilteredRouteStopData(
            RouteCardData.RouteStopData(
                route,
                stop,
                listOf(
                    RouteCardData.Leaf(
                        lineOrRoute,
                        stop,
                        directionId = 0,
                        listOf(routePatternOne),
                        setOf(stop.id),
                        listOf(UpcomingTrip(trip, prediction)),
                        alertsHere = listOf(alert),
                        allDataLoaded = false,
                        hasSchedulesToday = true,
                        alertsDownstream = emptyList(),
                        RouteCardData.Context.StopDetailsFiltered,
                    )
                ),
                GlobalResponse(builder),
            )
        )
        val errorBannerViewModel = ErrorBannerViewModel(false, MockErrorBannerStateRepository())
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                val filterState = remember {
                    mutableStateOf<StopDetailsFilter?>(StopDetailsFilter(route.id, 0))
                }
                StopDetailsView(
                    stopId = stop.id,
                    viewModel = viewModel,
                    isFavorite = { false },
                    updateFavorites = {},
                    onClose = {},
                    stopFilter = filterState.value,
                    tripFilter = null,
                    allAlerts = null,
                    updateStopFilter = filterState::value::set,
                    updateTripDetailsFilter = {},
                    tileScrollState = rememberScrollState(),
                    errorBannerViewModel = errorBannerViewModel,
                    openModal = {},
                    openSheetRoute = {},
                )
            }
        }

        composeTestRule.onNodeWithText(alert.header!!).assertExists()
    }
}
