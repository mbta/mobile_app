package com.mbta.tid.mbta_app.android.nearbyTransit

import android.app.Activity
import android.location.Location
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.rule.GrantPermissionRule
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mbta.tid.mbta_app.android.location.MockFusedLocationProviderClient
import com.mbta.tid.mbta_app.android.location.MockLocationDataManager
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.android.map.IMapViewModel
import com.mbta.tid.mbta_app.android.pages.NearbyTransit
import com.mbta.tid.mbta_app.android.pages.NearbyTransitPage
import com.mbta.tid.mbta_app.android.util.LocalActivity
import com.mbta.tid.mbta_app.android.util.LocalLocationClient
import com.mbta.tid.mbta_app.map.RouteLineData
import com.mbta.tid.mbta_app.model.Coordinate
import com.mbta.tid.mbta_app.model.GlobalMapData
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopMessageResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.IRailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.IVehiclesRepository
import com.mbta.tid.mbta_app.repositories.MockRailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.MockScheduleRepository
import com.mbta.tid.mbta_app.repositories.MockVehiclesRepository
import com.mbta.tid.mbta_app.usecases.TogglePinnedRouteUsecase
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.KoinTest

@OptIn(ExperimentalMaterial3Api::class, MapboxExperimental::class)
class NearbyTransitPageTest : KoinTest {
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
    val trip =
        builder.trip {
            id = "trip_1"
            routeId = "route_1"
            directionId = 0
            headsign = "Sample Headsign"
            routePatternId = "pattern_1"
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

    val globalResponse =
        GlobalResponse(
            builder,
            mutableMapOf(
                stop.id to listOf(routePatternOne.id, routePatternTwo.id),
                greenLineStop.id to listOf(greenLineRoutePatternOne.id)
            )
        )

    val koinApplication = koinApplication {
        modules(
            module {
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
                            onJoin(ApiResult.Ok(PredictionsByStopJoinResponse(builder)))
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
                            location: Coordinate
                        ): ApiResult<NearbyStaticData> {
                            val data = NearbyStaticData(global, NearbyResponse(builder))
                            return ApiResult.Ok(data)
                        }
                    }
                }
                single<IRailRouteShapeRepository> { MockRailRouteShapeRepository() }
                single<TogglePinnedRouteUsecase> { TogglePinnedRouteUsecase(get()) }
                single<IVehiclesRepository> { MockVehiclesRepository() }
            }
        )
    }

    @get:Rule
    val runtimePermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)
    @get:Rule val composeTestRule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testNearbyTransitPageDisplaysCorrectly() {
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                CompositionLocalProvider(
                    LocalActivity provides (LocalContext.current as Activity),
                    LocalLocationClient provides MockFusedLocationProviderClient()
                ) {
                    NearbyTransitPage(
                        Modifier,
                        NearbyTransit(
                            alertData = AlertsStreamDataResponse(builder.alerts),
                            globalResponse = globalResponse,
                            lastNearbyTransitLocationState =
                                remember { mutableStateOf(Position(0.0, 0.0)) },
                            nearbyTransitSelectingLocationState =
                                remember { mutableStateOf(false) },
                            scaffoldState = rememberBottomSheetScaffoldState(),
                            locationDataManager = MockLocationDataManager(Location("mock")),
                            viewportProvider = ViewportProvider(rememberMapViewportState()),
                        ),
                        false,
                        {},
                        {},
                        bottomBar = {}
                    )
                }
            }
        }

        composeTestRule.waitUntilDoesNotExist(hasText("Loading..."))

        composeTestRule.onNodeWithText("Nearby Transit").assertIsDisplayed()

        composeTestRule.onNodeWithText("Green Line Long Name").assertExists()
        composeTestRule.onNodeWithText("Green Line Stop").assertExists()
        composeTestRule.onNodeWithText("Green Line Head Sign").assertExists()
        composeTestRule.onNodeWithText("5 min").assertExists()

        composeTestRule.onNodeWithText("Sample Route").assertExists()
        composeTestRule.onNodeWithText("Sample Stop").assertExists()
        composeTestRule.onNodeWithText("Sample Headsign").assertExists()
        composeTestRule.onNodeWithText("1 min").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testReloadsMapboxConfigOnError() {

        open class MockMapVM : IMapViewModel {
            var mutableLastErrorTimestamp = MutableStateFlow<Instant?>(null)
            override var lastMapboxErrorTimestamp: Flow<Instant?> = mutableLastErrorTimestamp
            override var railRouteLineData: Flow<List<RouteLineData>?> =
                MutableStateFlow(value = null)
            override var stopSourceData: Flow<FeatureCollection?> = MutableStateFlow(value = null)
            override var globalResponse: Flow<GlobalResponse?> = MutableStateFlow(value = null)
            override var railRouteShapes: Flow<MapFriendlyRouteResponse?> =
                MutableStateFlow(value = null)

            var loadConfigCalledCount = 0

            override suspend fun loadConfig() {
                loadConfigCalledCount += 1
            }

            override fun globalMapData(now: Instant): GlobalMapData? {
                return null
            }

            override suspend fun refreshRouteLineData(now: Instant) {}

            override suspend fun refreshStopFeatures(now: Instant, selectedStop: Stop?) {}

            override suspend fun setAlertsData(alertsData: AlertsStreamDataResponse?) {}

            override suspend fun setGlobalResponse(globalResponse: GlobalResponse?) {}
        }

        val mockMapVM = MockMapVM()

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                CompositionLocalProvider(
                    LocalActivity provides (LocalContext.current as Activity),
                    LocalLocationClient provides MockFusedLocationProviderClient()
                ) {
                    NearbyTransitPage(
                        Modifier,
                        NearbyTransit(
                            alertData = AlertsStreamDataResponse(builder.alerts),
                            globalResponse = globalResponse,
                            lastNearbyTransitLocationState =
                                remember { mutableStateOf(Position(0.0, 0.0)) },
                            nearbyTransitSelectingLocationState =
                                remember { mutableStateOf(false) },
                            scaffoldState = rememberBottomSheetScaffoldState(),
                            locationDataManager = MockLocationDataManager(Location("mock")),
                            viewportProvider = ViewportProvider(rememberMapViewportState()),
                        ),
                        false,
                        {},
                        {},
                        bottomBar = {},
                        mapViewModel = mockMapVM
                    )
                }
            }
        }

        composeTestRule.waitUntilDoesNotExist(hasText("Loading..."))

        composeTestRule.waitUntil { mockMapVM.loadConfigCalledCount == 1 }
        mockMapVM.mutableLastErrorTimestamp.value = Clock.System.now()

        composeTestRule.waitUntil { mockMapVM.loadConfigCalledCount == 2 }
    }
}
