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
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopMessageResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.IRailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockRailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.MockScheduleRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.usecases.TogglePinnedRouteUsecase
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module

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

    val globalResponse =
        GlobalResponse(
            builder,
            mutableMapOf(
                stop.id to listOf(routePatternOne.id, routePatternTwo.id),
            )
        )

    val koinApplication = koinApplication {
        modules(
            module {
                single<Analytics> { MockAnalytics() }
                single<IErrorBannerStateRepository> { MockErrorBannerStateRepository() }
                single<ISchedulesRepository> { MockScheduleRepository() }
                single<IPredictionsRepository> {
                    object : IPredictionsRepository {
                        override fun connect(
                            stopIds: List<String>,
                            onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit
                        ) {
                            onReceive(ApiResult.Ok(PredictionsStreamDataResponse(builder)))
                        }

                        override fun connectV2(
                            stopIds: List<String>,
                            onJoin: (ApiResult<PredictionsByStopJoinResponse>) -> Unit,
                            onMessage: (ApiResult<PredictionsByStopMessageResponse>) -> Unit
                        ) {
                            /* no-op */
                        }

                        override var lastUpdated: Instant? = null

                        override fun shouldForgetPredictions(predictionCount: Int) = false

                        override fun disconnect() {
                            /* no-op */
                        }
                    }
                }
                single<IPinnedRoutesRepository> {
                    object : IPinnedRoutesRepository {
                        private var pinnedRoutes: Set<String> = emptySet()

                        override suspend fun getPinnedRoutes(): Set<String> {
                            return pinnedRoutes
                        }

                        override suspend fun setPinnedRoutes(routes: Set<String>) {
                            pinnedRoutes = routes
                        }
                    }
                }
                single<INearbyRepository> {
                    object : INearbyRepository {
                        override suspend fun getNearby(
                            global: GlobalResponse,
                            location: Position
                        ): ApiResult<NearbyStaticData> {
                            val data = NearbyStaticData(global, NearbyResponse(builder))
                            return ApiResult.Ok(data)
                        }
                    }
                }
                single<IRailRouteShapeRepository> { MockRailRouteShapeRepository() }
                single<TogglePinnedRouteUsecase> { TogglePinnedRouteUsecase(get()) }
                single<IGlobalRepository> { MockGlobalRepository(globalResponse) }
            }
        )
    }

    @get:Rule val composeTestRule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testStopDetailsViewDisplaysUnfilteredCorrectly() {
        val viewModel = StopDetailsViewModel.mocked()

        viewModel.setDepartures(
            StopDetailsDepartures(
                listOf(
                    PatternsByStop(
                        route = route,
                        stop = stop,
                        patterns =
                            listOf(
                                RealtimePatterns.ByHeadsign(
                                    staticData =
                                        NearbyStaticData.StaticPatterns.ByHeadsign(
                                            route = route,
                                            headsign = trip.headsign,
                                            line = line,
                                            patterns = listOf(routePatternOne, routePatternTwo),
                                            stopIds = setOf(stop.id),
                                        ),
                                    upcomingTripsMap =
                                        mapOf(
                                            RealtimePatterns.UpcomingTripKey.ByRoutePattern(
                                                trip.routeId,
                                                routePatternOne.id,
                                                stop.id
                                            ) to listOf(UpcomingTrip(trip, prediction))
                                        ),
                                    parentStopId = stop.id,
                                    alertsHere = emptyList(),
                                    alertsDownstream = emptyList(),
                                    hasSchedulesTodayByPattern = null,
                                    allDataLoaded = false
                                )
                            )
                    )
                )
            )
        )
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                val filterState = remember { mutableStateOf<StopDetailsFilter?>(null) }
                StopDetailsView(
                    stopId = stop.id,
                    viewModel = viewModel,
                    pinnedRoutes = emptySet(),
                    togglePinnedRoute = {},
                    onClose = {},
                    stopFilter = null,
                    tripFilter = null,
                    allAlerts = null,
                    updateStopFilter = filterState::value::set,
                    updateTripDetailsFilter = {},
                    tileScrollState = rememberScrollState(),
                    errorBannerViewModel =
                        ErrorBannerViewModel(
                            false,
                            MockErrorBannerStateRepository(),
                            MockSettingsRepository()
                        ),
                    setMapSelectedVehicle = {},
                    openModal = {},
                    openSheetRoute = {}
                )
            }
        }

        composeTestRule.waitUntilExactlyOneExists(hasText("Sample Stop"))

        composeTestRule.onNode(hasText("Sample Stop") and isHeading()).assertIsDisplayed()

        composeTestRule.onNodeWithText("Sample Route").assertExists()
        composeTestRule.onNodeWithText("Sample Headsign").assertExists()
        composeTestRule.onNodeWithText("1 min").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testStopDetailsViewDisplaysFilteredCorrectly() {
        val viewModel = StopDetailsViewModel.mocked()

        viewModel.setDepartures(
            StopDetailsDepartures(
                listOf(
                    PatternsByStop(
                        route = route,
                        stop = stop,
                        patterns =
                            listOf(
                                RealtimePatterns.ByHeadsign(
                                    staticData =
                                        NearbyStaticData.StaticPatterns.ByHeadsign(
                                            route = route,
                                            headsign = trip.headsign,
                                            line = line,
                                            patterns = listOf(routePatternOne, routePatternTwo),
                                            stopIds = setOf(stop.id),
                                        ),
                                    upcomingTripsMap =
                                        mapOf(
                                            RealtimePatterns.UpcomingTripKey.ByRoutePattern(
                                                trip.routeId,
                                                routePatternOne.id,
                                                stop.id
                                            ) to listOf(UpcomingTrip(trip, prediction))
                                        ),
                                    parentStopId = stop.id,
                                    alertsHere = emptyList(),
                                    alertsDownstream = emptyList(),
                                    hasSchedulesTodayByPattern = null,
                                    allDataLoaded = false
                                )
                            )
                    )
                )
            )
        )
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                val filterState = remember {
                    mutableStateOf<StopDetailsFilter?>(StopDetailsFilter(route.id, 0))
                }
                StopDetailsView(
                    stopId = stop.id,
                    viewModel = viewModel,
                    pinnedRoutes = emptySet(),
                    togglePinnedRoute = {},
                    onClose = {},
                    stopFilter = filterState.value,
                    tripFilter = null,
                    allAlerts = null,
                    updateStopFilter = filterState::value::set,
                    updateTripDetailsFilter = {},
                    tileScrollState = rememberScrollState(),
                    errorBannerViewModel =
                        ErrorBannerViewModel(
                            false,
                            MockErrorBannerStateRepository(),
                            MockSettingsRepository()
                        ),
                    setMapSelectedVehicle = {},
                    openModal = {},
                    openSheetRoute = {}
                )
            }
        }

        composeTestRule.waitUntilExactlyOneExists(hasText("at Sample Stop"))

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

        val viewModel =
            StopDetailsViewModel.mocked(
                settingsRepository =
                    MockSettingsRepository(
                        settings = mapOf(Pair(Settings.ElevatorAccessibility, true))
                    )
            )

        viewModel.setDepartures(
            StopDetailsDepartures(
                listOf(
                    PatternsByStop(
                        route = route,
                        stop = stop,
                        patterns =
                            listOf(
                                RealtimePatterns.ByHeadsign(
                                    staticData =
                                        NearbyStaticData.StaticPatterns.ByHeadsign(
                                            route = route,
                                            headsign = trip.headsign,
                                            line = line,
                                            patterns = listOf(routePatternOne, routePatternTwo),
                                            stopIds = setOf(stop.id),
                                        ),
                                    upcomingTripsMap =
                                        mapOf(
                                            RealtimePatterns.UpcomingTripKey.ByRoutePattern(
                                                trip.routeId,
                                                routePatternOne.id,
                                                stop.id
                                            ) to listOf(UpcomingTrip(trip, prediction))
                                        ),
                                    parentStopId = stop.id,
                                    alertsHere = emptyList(),
                                    alertsDownstream = emptyList(),
                                    hasSchedulesTodayByPattern = null,
                                    allDataLoaded = false
                                )
                            ),
                        listOf(alert)
                    )
                )
            )
        )
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                val filterState = remember { mutableStateOf<StopDetailsFilter?>(null) }
                StopDetailsView(
                    stopId = stop.id,
                    viewModel = viewModel,
                    pinnedRoutes = emptySet(),
                    togglePinnedRoute = {},
                    onClose = {},
                    stopFilter = filterState.value,
                    tripFilter = null,
                    allAlerts = null,
                    updateStopFilter = filterState::value::set,
                    updateTripDetailsFilter = {},
                    tileScrollState = rememberScrollState(),
                    errorBannerViewModel =
                        ErrorBannerViewModel(
                            false,
                            MockErrorBannerStateRepository(),
                            MockSettingsRepository()
                        ),
                    setMapSelectedVehicle = {},
                    openModal = {},
                    openSheetRoute = {}
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

        val viewModel =
            StopDetailsViewModel.mocked(
                settingsRepository =
                    MockSettingsRepository(
                        settings = mapOf(Pair(Settings.ElevatorAccessibility, true))
                    )
            )

        viewModel.setDepartures(
            StopDetailsDepartures(
                listOf(
                    PatternsByStop(
                        route = route,
                        stop = stop,
                        patterns =
                            listOf(
                                RealtimePatterns.ByHeadsign(
                                    staticData =
                                        NearbyStaticData.StaticPatterns.ByHeadsign(
                                            route = route,
                                            headsign = trip.headsign,
                                            line = line,
                                            patterns = listOf(routePatternOne, routePatternTwo),
                                            stopIds = setOf(stop.id),
                                        ),
                                    upcomingTripsMap =
                                        mapOf(
                                            RealtimePatterns.UpcomingTripKey.ByRoutePattern(
                                                trip.routeId,
                                                routePatternOne.id,
                                                stop.id
                                            ) to listOf(UpcomingTrip(trip, prediction))
                                        ),
                                    parentStopId = stop.id,
                                    alertsHere = emptyList(),
                                    alertsDownstream = emptyList(),
                                    hasSchedulesTodayByPattern = null,
                                    allDataLoaded = false
                                )
                            ),
                        listOf(alert)
                    )
                )
            )
        )
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                val filterState = remember {
                    mutableStateOf<StopDetailsFilter?>(StopDetailsFilter(route.id, 0))
                }
                StopDetailsView(
                    stopId = stop.id,
                    viewModel = viewModel,
                    pinnedRoutes = emptySet(),
                    togglePinnedRoute = {},
                    onClose = {},
                    stopFilter = filterState.value,
                    tripFilter = null,
                    allAlerts = null,
                    updateStopFilter = filterState::value::set,
                    updateTripDetailsFilter = {},
                    tileScrollState = rememberScrollState(),
                    errorBannerViewModel =
                        ErrorBannerViewModel(
                            false,
                            MockErrorBannerStateRepository(),
                            MockSettingsRepository()
                        ),
                    setMapSelectedVehicle = {},
                    openModal = {},
                    openSheetRoute = {}
                )
            }
        }

        composeTestRule.onNodeWithText(alert.header!!).assertExists()
    }
}
