package com.mbta.tid.mbta_app.android.nearbyTransit

import android.Manifest
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
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraState
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mbta.tid.mbta_app.android.component.sheet.rememberBottomSheetScaffoldState
import com.mbta.tid.mbta_app.android.location.IViewportProvider
import com.mbta.tid.mbta_app.android.location.MockFusedLocationProviderClient
import com.mbta.tid.mbta_app.android.location.MockLocationDataManager
import com.mbta.tid.mbta_app.android.location.ViewportProvider
import com.mbta.tid.mbta_app.android.map.IMapboxConfigManager
import com.mbta.tid.mbta_app.android.pages.MapAndSheetPage
import com.mbta.tid.mbta_app.android.pages.NearbyTransit
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDoesNotExistDefaultTimeout
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.android.util.LocalLocationClient
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SheetRoutes
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.repositories.MockNearbyRepository
import com.mbta.tid.mbta_app.viewModel.IMapViewModel
import com.mbta.tid.mbta_app.viewModel.MapViewModel
import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.spy
import io.github.dellisd.spatialk.geojson.Position
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
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
                        mock<IMapViewModel>(MockMode.autofill),
                    )
                }
            }
        }

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasContentDescription("Mapbox Attribution")
        )
        composeTestRule.onNodeWithContentDescription("Mapbox Attribution").assertIsDisplayed()
        composeTestRule.waitUntilDoesNotExistDefaultTimeout(hasContentDescription("Loading..."))
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

        open class MockConfigManager : IMapboxConfigManager {
            var mutableLastErrorTimestamp = MutableStateFlow<Instant?>(null)
            override var lastMapboxErrorTimestamp: Flow<Instant?> = mutableLastErrorTimestamp
            override val configLoadAttempted: StateFlow<Boolean> = MutableStateFlow(value = false)
            var loadConfigCalledCount = 0

            override suspend fun loadConfig() {
                loadConfigCalledCount += 1
            }
        }
        val mockMapVM = mock<IMapViewModel>(MockMode.autofill)
        every { mockMapVM.models } returns MutableStateFlow(MapViewModel.State.Unfiltered)
        val mockConfigManager = MockConfigManager()

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
                        mapboxConfigManager = mockConfigManager,
                    )
                }
            }
        }

        composeTestRule.waitUntilDoesNotExistDefaultTimeout(
            hasContentDescription("Loading...", substring = true)
        )

        composeTestRule.waitUntil { mockConfigManager.loadConfigCalledCount == 1 }
        mockConfigManager.mutableLastErrorTimestamp.value = Clock.System.now()

        composeTestRule.waitUntil { mockConfigManager.loadConfigCalledCount == 2 }
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
                        mock<IMapViewModel>(MockMode.autofill),
                    )
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Mapbox Logo").assertDoesNotExist()
    }

    @Test
    fun testResetToFollowingAfter1hour() = runBlocking {
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
        val mockMapVM = mock<IMapViewModel>(MockMode.autofill)
        every { mockMapVM.models } returns MutableStateFlow(MapViewModel.State.Unfiltered)
        val viewportProvider =
            spy<IViewportProvider>(
                ViewportProvider(
                    MapViewportState(
                        CameraState(
                            // Specifically setting zoom so that we don't fall back to using the
                            // default
                            // center location. If default center is used, then since `hasPermission
                            // = true`,
                            // A MapEffect will also call follow and throw off the count of calls to
                            // follow.
                            Point.fromLngLat(sampleStop.longitude, sampleStop.latitude),
                            EdgeInsets(0.0, 0.0, 0.0, 0.0),
                            /* zoom = */ ViewportProvider.Companion.Defaults.zoom,
                            /* bearing = */ 0.0,
                            /* pitch = */ 0.0,
                        )
                    )
                )
            )
        // setting to false so that the only calls to `follow()` will come from backgrounding.
        // Otherwise, HomeMapView LaunchedEffect will sometimes also call `follow()` within the
        // duration of the test and throw off the count.
        viewportProvider.isFollowingPuck = false
        var followCallCount = 0

        everySuspend { viewportProvider.follow(any()) } calls { followCallCount += 1 }

        val koinApplication = testKoinApplication(builder, clock = mockClock)
        locationDataManager.hasPermission = true
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
                        mockMapVM,
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.waitUntilDefaultTimeout { followCallCount == 0 }

        assertEquals(followCallCount, 0)

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE) }

        mockClock.plus(30.minutes)

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }
        composeTestRule.waitForIdle()

        assertEquals(followCallCount, 0)

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE) }

        mockClock.plus(2.hours)

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilDefaultTimeout { followCallCount == 1 }
        assertEquals(1, followCallCount)
    }
}
