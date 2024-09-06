package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.util.StopDetailsFilter
import com.mbta.tid.mbta_app.model.Coordinate
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Outcome
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.IRailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockRailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.MockScheduleRepository
import com.mbta.tid.mbta_app.usecases.TogglePinnedRouteUsecase
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
                single<IGlobalRepository> { MockGlobalRepository(globalResponse) }
            }
        )
    }

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testStopDetailsViewDisplaysCorrectly() {
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                val filterState = remember { mutableStateOf<StopDetailsFilter?>(null) }
                StopDetailsView(
                    stop = stop,
                    departures =
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
                                                        patterns =
                                                            listOf(routePatternOne, routePatternTwo)
                                                    ),
                                                upcomingTripsMap =
                                                    mapOf(
                                                        RealtimePatterns.UpcomingTripKey.ByHeadsign(
                                                            trip.routeId,
                                                            trip.headsign,
                                                            stop.id
                                                        ) to listOf(UpcomingTrip(trip, prediction))
                                                    ),
                                                stopIds = setOf(stop.id),
                                                alerts = null
                                            )
                                        )
                                )
                            )
                        ),
                    pinnedRoutes = emptySet(),
                    togglePinnedRoute = {},
                    onClose = {},
                    filter = filterState.value,
                    updateStopFilter = filterState::value::set
                )
            }
        }

        composeTestRule.onNodeWithText("Sample Stop").assertExists()
        composeTestRule.onNodeWithText("Sample Route").assertExists()
        composeTestRule.onNodeWithText("Sample Headsign").assertExists()
        composeTestRule.onNodeWithText("1 min").assertExists()
    }
}
