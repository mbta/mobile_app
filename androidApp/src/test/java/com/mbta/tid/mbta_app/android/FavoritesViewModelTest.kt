package com.mbta.tid.mbta_app.android

import com.mbta.tid.mbta_app.android.favorites.FavoritesViewModel
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.MockFavoritesRepository
import com.mbta.tid.mbta_app.usecases.FavoritesUsecases
import io.github.dellisd.spatialk.geojson.Position
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Test

class FavoritesViewModelTest {

    val builder = ObjectCollectionBuilder()
    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
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
    val stop1 =
        builder.stop {
            id = "stop_1"
            name = "Sample Stop"
            locationType = LocationType.STOP
            latitude = 0.0
            longitude = 0.0
        }
    val stop2 =
        builder.stop {
            id = "stop_2"
            name = "Sample Stop 2"
            locationType = LocationType.STOP
            latitude = 0.0
            longitude = 0.0
        }

    val stop3 =
        builder.stop {
            id = "stop_3"
            name = "Sample Stop 3"
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
            stopIds = listOf(stop1.id, stop2.id, stop3.id)
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
                stop1.id to listOf(routePatternOne.id, routePatternTwo.id),
                stop2.id to listOf(routePatternOne.id, routePatternTwo.id),
            ),
        )

    @Test
    fun testFiltering() = runBlocking {
        val usecases = FavoritesUsecases(MockFavoritesRepository())
        val favoritesVM = FavoritesViewModel(usecases)
        val position = Position(0.0, 0.0)
        favoritesVM.favorites = setOf(RouteStopDirection("route_1", "stop_1", 0))
        favoritesVM.loadRealtimeRouteCardData(
            globalResponse,
            position,
            null,
            PredictionsStreamDataResponse(builder),
            AlertsStreamDataResponse(emptyMap()),
            now,
        )
        val lineOrRoute = RouteCardData.LineOrRoute.Route(route)

        val expectedRouteCardData =
            listOf(
                RouteCardData(
                    lineOrRoute = lineOrRoute,
                    stopData =
                        listOf(
                            RouteCardData.RouteStopData(
                                route,
                                stop1,
                                listOf(
                                    RouteCardData.Leaf(
                                        lineOrRoute = lineOrRoute,
                                        stop = stop1,
                                        directionId = 0,
                                        routePatterns = listOf(routePatternOne),
                                        stopIds = setOf(stop1.id),
                                        upcomingTrips = listOf(builder.upcomingTrip(prediction)),
                                        alertsHere = emptyList(),
                                        allDataLoaded = false,
                                        hasSchedulesToday = false,
                                        alertsDownstream = emptyList(),
                                        context = RouteCardData.Context.Favorites,
                                    )
                                ),
                                globalResponse,
                            )
                        ),
                    now,
                )
            )
        assertEquals(expectedRouteCardData, favoritesVM.routeCardData)
    }
}
