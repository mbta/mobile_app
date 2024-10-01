package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraState
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mbta.tid.mbta_app.android.pages.NearbyTransit
import com.mbta.tid.mbta_app.android.pages.NearbyTransitPage
import com.mbta.tid.mbta_app.model.Coordinate
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Outcome
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
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
                            onReceive:
                                (Outcome<PredictionsStreamDataResponse?, SocketError>) -> Unit
                        ) {
                            onReceive(Outcome(PredictionsStreamDataResponse(builder), null))
                        }

                        override fun connectV2(
                            stopIds: List<String>,
                            onJoin: (Outcome<PredictionsByStopJoinResponse?, SocketError>) -> Unit,
                            onMessage:
                                (Outcome<PredictionsByStopMessageResponse?, SocketError>) -> Unit
                        ) {
                            /* no-op */
                        }

                        override var lastUpdated: Instant? = null

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
                        ): NearbyStaticData {
                            val data = NearbyStaticData(global, NearbyResponse(builder))
                            return data
                        }
                    }
                }
                single<IRailRouteShapeRepository> { MockRailRouteShapeRepository() }
                single<TogglePinnedRouteUsecase> { TogglePinnedRouteUsecase(get()) }
                single<IVehiclesRepository> { MockVehiclesRepository() }
            }
        )
    }

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testNearbyTransitPageDisplaysCorrectly() {
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                NearbyTransitPage(
                    Modifier,
                    NearbyTransit(
                        alertData = AlertsStreamDataResponse(builder.alerts),
                        globalResponse = globalResponse,
                        targetLocation = Position(0.0, 0.0),
                        lastNearbyTransitLocation = Position(0.0, 0.0),
                        mapCenter = Position(0.0, 0.0),
                        mapViewportState =
                            MapViewportState(
                                CameraState(
                                    Point.fromLngLat(0.0, 0.0),
                                    EdgeInsets(0.0, 0.0, 0.0, 0.0),
                                    1.0,
                                    0.0,
                                    0.0
                                )
                            ),
                        scaffoldState = rememberBottomSheetScaffoldState(),
                    ),
                    false,
                    {},
                    {},
                    bottomBar = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Nearby transit").assertIsDisplayed()

        composeTestRule.onNodeWithText("Green Line Long Name").assertExists()
        composeTestRule.onNodeWithText("Green Line Stop").assertExists()
        composeTestRule.onNodeWithText("Green Line Head Sign").assertExists()
        composeTestRule.onNodeWithText("5 min").assertExists()

        composeTestRule.onNodeWithText("Sample Route").assertExists()
        composeTestRule.onNodeWithText("Sample Stop").assertExists()
        composeTestRule.onNodeWithText("Sample Headsign").assertExists()
        composeTestRule.onNodeWithText("1 min").assertExists()
    }
}
