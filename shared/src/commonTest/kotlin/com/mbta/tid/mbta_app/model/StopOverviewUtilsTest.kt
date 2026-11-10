package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Month

class StopOverviewUtilsTest {
    @Test
    fun `autoStopFilter provides a default RouteStopFilter given a single route and direction`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()
            val stop = objects.stop()
            val route = objects.route()
            val routePattern =
                objects.routePattern(route) {
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { headsign = "A" }
                }
            val time = EasternTimeInstant(2024, Month.MARCH, 19, 14, 16, 17)

            val routeCardData =
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id),
                    GlobalResponse(objects, mapOf(stop.id to listOf(routePattern.id))),
                    sortByDistanceFrom = null,
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    AlertsStreamDataResponse(objects),
                    time,
                    RouteCardData.Context.StopOverview,
                )

            assertEquals(
                RouteStopFilter(
                    routeId = route.id,
                    directionId = routePattern.directionId,
                    autoFilter = true,
                ),
                StopOverviewUtils.autoRouteStopFilter(routeCardData),
            )
        }

    @Test
    fun `autoStopFilter provides a null stop filter value given multiple routes and directions`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()
            val stop = objects.stop()
            val route1 = objects.route()
            val routePattern1 =
                objects.routePattern(route1) {
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { headsign = "A" }
                }
            val route2 = objects.route()
            val routePattern2 =
                objects.routePattern(route2) {
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { headsign = "B" }
                }
            val time = EasternTimeInstant(2024, Month.MARCH, 19, 14, 16, 17)

            val routeCardData =
                RouteCardData.routeCardsForStopList(
                    listOf(stop.id),
                    GlobalResponse(
                        objects,
                        mapOf(stop.id to listOf(routePattern1.id, routePattern2.id)),
                    ),
                    sortByDistanceFrom = null,
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    AlertsStreamDataResponse(objects),
                    time,
                    RouteCardData.Context.StopOverview,
                )

            assertEquals(null, StopOverviewUtils.autoRouteStopFilter(checkNotNull(routeCardData)))
        }
}
