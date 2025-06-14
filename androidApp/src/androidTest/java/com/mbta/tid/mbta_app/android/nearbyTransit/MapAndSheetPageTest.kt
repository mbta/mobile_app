package com.mbta.tid.mbta_app.android.nearbyTransit

import android.Manifest
import android.location.Location
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.rule.GrantPermissionRule
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraState
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.component.sheet.rememberBottomSheetScaffoldState
import com.mbta.tid.mbta_app.android.location.LocationDataManager
import com.mbta.tid.mbta_app.android.location.MockFusedLocationProviderClient
import com.mbta.tid.mbta_app.android.location.MockLocationDataManager
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.android.map.IMapViewModel
import com.mbta.tid.mbta_app.android.pages.MapAndSheetPage
import com.mbta.tid.mbta_app.android.pages.NearbyTransit
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.android.util.LocalLocationClient
import com.mbta.tid.mbta_app.android.util.isFollowingPuck
import com.mbta.tid.mbta_app.android.util.isRoughlyEqualTo
import com.mbta.tid.mbta_app.map.RouteSourceData
import com.mbta.tid.mbta_app.model.GlobalMapData
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.repositories.MockNearbyRepository
import io.github.dellisd.spatialk.geojson.Position
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext
import org.koin.test.KoinTest

@OptIn(ExperimentalMaterial3Api::class)
class MapAndSheetPageTest : KoinTest {
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
    val sampleStop =
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
                sampleStop.id to listOf(routePatternOne.id, routePatternTwo.id),
                greenLineStop.id to listOf(greenLineRoutePatternOne.id),
            ),
        )

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

    val viewportProvider = ViewportProvider(MapViewportState())

    @get:Rule
    val runtimePermissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)
    @get:Rule val composeTestRule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testMapAndSheetPageDisplaysCorrectly() {
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                CompositionLocalProvider(
                    LocalLocationClient provides MockFusedLocationProviderClient()
                ) {
                    MapAndSheetPage(
                        Modifier,
                        NearbyTransit(
                            alertData = AlertsStreamDataResponse(builder.alerts),
                            globalResponse = globalResponse,
                            hideMaps = false,
                            lastNearbyTransitLocationState =
                                remember { mutableStateOf(Position(0.0, 0.0)) },
                            nearbyTransitSelectingLocationState =
                                remember { mutableStateOf(false) },
                            scaffoldState = rememberBottomSheetScaffoldState(),
                            locationDataManager = MockLocationDataManager(),
                            viewportProvider = viewportProvider,
                        ),
                        SheetRoutes.NearbyTransit,
                        false,
                        {},
                        {},
                        bottomBar = {},
                    )
                }
            }
        }

        composeTestRule.waitUntilExactlyOneExists(hasContentDescription("Mapbox Attribution"))
        composeTestRule.onNodeWithContentDescription("Mapbox Attribution").assertIsDisplayed()
        composeTestRule.waitUntilDoesNotExist(hasContentDescription("Loading..."))
        composeTestRule
            .onNodeWithContentDescription("Drag handle")
            .performSemanticsAction(SemanticsActions.Expand)

        composeTestRule.onNodeWithText("Nearby Transit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stops").assertIsDisplayed()

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
            override var railRouteSourceData: Flow<List<RouteSourceData>?> =
                MutableStateFlow(value = null)
            override var stopSourceData: Flow<FeatureCollection?> = MutableStateFlow(value = null)
            override var globalResponse: Flow<GlobalResponse?> = MutableStateFlow(value = null)
            override var railRouteShapes: Flow<MapFriendlyRouteResponse?> =
                MutableStateFlow(value = null)
            override val selectedVehicle: StateFlow<Vehicle?> = MutableStateFlow(value = null)
            override val configLoadAttempted: StateFlow<Boolean> = MutableStateFlow(value = false)
            override val globalMapData: Flow<GlobalMapData?> = MutableStateFlow(value = null)
            override val selectedStop: StateFlow<Stop?> = MutableStateFlow(value = null)
            override val stopFilter: StateFlow<StopDetailsFilter?> = MutableStateFlow(value = null)
            override val showRecenterButton: StateFlow<Boolean> = MutableStateFlow(value = false)
            override val showTripCenterButton: StateFlow<Boolean> = MutableStateFlow(value = false)
            var loadConfigCalledCount = 0

            override suspend fun loadConfig() {
                loadConfigCalledCount += 1
            }

            override suspend fun globalMapData(now: Instant): GlobalMapData? {
                return null
            }

            override suspend fun refreshGlobalMapData(now: Instant) {}

            override suspend fun refreshRouteLineData(globalMapData: GlobalMapData?) {}

            override suspend fun refreshStopFeatures(globalMapData: GlobalMapData?) {}

            override suspend fun setAlertsData(alertsData: AlertsStreamDataResponse?) {}

            override suspend fun setGlobalResponse(globalResponse: GlobalResponse?) {}

            override fun setSelectedVehicle(selectedVehicle: Vehicle?) {}

            override fun setSelectedStop(stop: Stop?) {
                TODO("Not yet implemented")
            }

            override fun setStopFilter(stopFilter: StopDetailsFilter?) {
                TODO("Not yet implemented")
            }

            override fun hideCenterButtons() {
                TODO("Not yet implemented")
            }

            override fun updateCenterButtonVisibility(
                currentLocation: Location?,
                locationDataManager: LocationDataManager,
                isSearchExpanded: Boolean,
                viewportProvider: ViewportProvider,
            ) {
                TODO("Not yet implemented")
            }
        }

        val mockMapVM = MockMapVM()

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                CompositionLocalProvider(
                    LocalLocationClient provides MockFusedLocationProviderClient()
                ) {
                    MapAndSheetPage(
                        Modifier,
                        NearbyTransit(
                            alertData = AlertsStreamDataResponse(builder.alerts),
                            globalResponse = globalResponse,
                            hideMaps = false,
                            lastNearbyTransitLocationState =
                                remember { mutableStateOf(Position(0.0, 0.0)) },
                            nearbyTransitSelectingLocationState =
                                remember { mutableStateOf(false) },
                            scaffoldState = rememberBottomSheetScaffoldState(),
                            locationDataManager = MockLocationDataManager(),
                            viewportProvider = viewportProvider,
                        ),
                        SheetRoutes.NearbyTransit,
                        false,
                        {},
                        {},
                        bottomBar = {},
                        mapViewModel = mockMapVM,
                    )
                }
            }
        }

        composeTestRule.waitUntilDoesNotExist(hasContentDescription("Loading...", substring = true))

        composeTestRule.waitUntil { mockMapVM.loadConfigCalledCount == 1 }
        mockMapVM.mutableLastErrorTimestamp.value = Clock.System.now()

        composeTestRule.waitUntil { mockMapVM.loadConfigCalledCount == 2 }
    }

    @Test
    fun testHidesMap() {
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                CompositionLocalProvider(
                    LocalLocationClient provides MockFusedLocationProviderClient()
                ) {
                    MapAndSheetPage(
                        Modifier,
                        NearbyTransit(
                            alertData = AlertsStreamDataResponse(builder.alerts),
                            globalResponse = globalResponse,
                            hideMaps = true,
                            lastNearbyTransitLocationState =
                                remember { mutableStateOf(Position(0.0, 0.0)) },
                            nearbyTransitSelectingLocationState =
                                remember { mutableStateOf(false) },
                            scaffoldState = rememberBottomSheetScaffoldState(),
                            locationDataManager = MockLocationDataManager(),
                            viewportProvider = viewportProvider,
                        ),
                        SheetRoutes.NearbyTransit,
                        false,
                        {},
                        {},
                        bottomBar = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Mapbox Logo").assertDoesNotExist()
    }

    @Test
    @Ignore("flaky test passing locally but failing in CI")
    fun testResetAfter1hour() = runBlocking {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        val mockClock =
            object : Clock {
                var time: Instant = Clock.System.now()

                fun plus(duration: Duration) {
                    time = time.plus(duration)
                }

                override fun now(): Instant = time
            }

        val startLocation = Position(0.0, 0.0)
        val locationDataManager = MockLocationDataManager(startLocation)
        locationDataManager.hasPermission = true

        val koinApplication = testKoinApplication(builder, clock = mockClock)

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                CompositionLocalProvider(
                    LocalLocationClient provides MockFusedLocationProviderClient(),
                    LocalLifecycleOwner provides lifecycleOwner,
                ) {
                    MapAndSheetPage(
                        Modifier,
                        NearbyTransit(
                            alertData = AlertsStreamDataResponse(builder.alerts),
                            globalResponse = globalResponse,
                            hideMaps = false,
                            lastNearbyTransitLocationState =
                                remember { mutableStateOf(Position(0.0, 0.0)) },
                            nearbyTransitSelectingLocationState =
                                remember { mutableStateOf(false) },
                            scaffoldState = rememberBottomSheetScaffoldState(),
                            locationDataManager = locationDataManager,
                            viewportProvider = viewportProvider,
                        ),
                        SheetRoutes.NearbyTransit,
                        false,
                        {},
                        {},
                        bottomBar = {},
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.waitUntil { viewportProvider.getViewportImmediate().cameraState != null }
        val updatedCamera =
            CameraState(Point.fromLngLat(1.1, 1.1), EdgeInsets(0.0, 0.0, 0.0, 0.0), 1.0, 0.0, 0.0)
        viewportProvider.setIsManuallyCentering(true)
        viewportProvider.updateCameraState(updatedCamera)
        runBlocking {
            viewportProvider.withViewport { viewport ->
                viewport.setCameraOptions {
                    center(updatedCamera.center)
                    zoom(updatedCamera.zoom)
                }
            }
        }
        viewportProvider.setIsManuallyCentering(false)

        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(3000) {
            viewportProvider
                .getViewportImmediate()
                .cameraState
                ?.center
                ?.isRoughlyEqualTo(updatedCamera.center) == true
        }

        assertTrue(
            updatedCamera.center.isRoughlyEqualTo(
                viewportProvider.getViewportImmediate().cameraState?.center!!
            )
        )
        assertFalse(viewportProvider.isFollowingPuck)

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE) }

        mockClock.plus(30.minutes)

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }
        composeTestRule.waitForIdle()

        assertTrue(
            updatedCamera.center.isRoughlyEqualTo(
                viewportProvider.getViewportImmediate().cameraState?.center!!
            )
        )
        assertFalse(viewportProvider.isFollowingPuck)

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE) }

        mockClock.plus(2.hours)

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(3000) {
            !updatedCamera.center.isRoughlyEqualTo(
                viewportProvider.getViewportImmediate().cameraState!!.center
            ) && viewportProvider.getViewportImmediate().isFollowingPuck
        }

        assertFalse(
            updatedCamera.center.isRoughlyEqualTo(
                viewportProvider.getViewportImmediate().cameraState?.center!!
            )
        )
        assertTrue(viewportProvider.getViewportImmediate().isFollowingPuck)
        assertTrue(viewportProvider.isFollowingPuck)
    }
}
