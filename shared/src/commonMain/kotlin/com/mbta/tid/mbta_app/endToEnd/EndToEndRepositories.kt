package com.mbta.tid.mbta_app.endToEnd

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteType
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import org.koin.core.module.Module
import org.koin.dsl.module

fun endToEndModule(): Module {
    val now = Clock.System.now()
    val objects = ObjectCollectionBuilder()
    val lineRed = objects.line()
    val routeRed =
        objects.route {
            color = "DA291C"
            directionDestinations = listOf("Ashmont/Braintree", "Alewife")
            directionNames = listOf("South", "North")
            longName = "Red Line"
            textColor = "FFFFFF"
            type = RouteType.HEAVY_RAIL
            lineId = lineRed.id
        }
    val patternAlewife =
        objects.routePattern(routeRed) {
            directionId = 1
            representativeTrip { headsign = "Alewife" }
            typicality = RoutePattern.Typicality.Typical
        }
    val patternAshmont =
        objects.routePattern(routeRed) {
            directionId = 0
            representativeTrip { headsign = "Ashmont" }
            typicality = RoutePattern.Typicality.Typical
        }
    val stopParkStreet = objects.stop { name = "Park Street" }
    val tripAlewife = objects.trip(patternAlewife)
    val predictionAlewife =
        objects.prediction {
            trip = tripAlewife
            stopId = stopParkStreet.id
            departureTime = now + 10.minutes
        }
    return module {
        //        single<IAppCheckRepository> { MockAppCheckRepository() }
        //        single<IConfigRepository> { MockConfigRepository() }
        //        single<IPinnedRoutesRepository> {
        //            object : IPinnedRoutesRepository {
        //                override suspend fun getPinnedRoutes() = emptySet<String>()
        //
        //                override suspend fun setPinnedRoutes(routes: Set<String>) {}
        //            }
        //        }
        //        single<ISchedulesRepository> {
        //            object : ISchedulesRepository {
        //                override suspend fun getSchedule(stopIds: List<String>, now: Instant) =
        //                    ScheduleResponse(objects)
        //
        //                override suspend fun getSchedule(stopIds: List<String>) =
        //                    getSchedule(stopIds, Clock.System.now())
        //            }
        //        }
        //        single<ISettingsRepository> { MockSettingsRepository() }
        //        single<IStopRepository> {
        //            object : IStopRepository {
        //                override suspend fun getStopMapData(stopId: String): StopMapResponse =
        //                    StopMapResponse(emptyList(), emptyMap())
        //            }
        //        }
        //        single<ITripRepository> {
        //            object : ITripRepository {
        //                override suspend fun getTripSchedules(tripId: String):
        // TripSchedulesResponse {
        //                    TODO("Not yet implemented")
        //                }
        //
        //                override suspend fun getTrip(tripId: String): ApiResult<TripResponse> {
        //                    TODO("Not yet implemented")
        //                }
        //
        //                override suspend fun getTripShape(tripId: String): ApiResult<TripShape> {
        //                    TODO("Not yet implemented")
        //                }
        //            }
        //        }
        //        single<IPredictionsRepository> {
        //            object : IPredictionsRepository {
        //                override fun connect(
        //                    stopIds: List<String>,
        //                    onReceive: (Outcome<PredictionsStreamDataResponse?, SocketError>) ->
        // Unit
        //                ) {
        //                    onReceive(Outcome(PredictionsStreamDataResponse(objects), null))
        //                }
        //
        //                override fun disconnect() {}
        //            }
        //        }
        //        single<IAlertsRepository> { MockAlertsRepository() }
        //        single<INearbyRepository> {
        //            object : INearbyRepository {
        //                override suspend fun getNearby(global: GlobalResponse, location:
        // Coordinate) =
        //                    NearbyStaticData(global, NearbyResponse(listOf(stopParkStreet.id)))
        //            }
        //        }
        //        single<ITripPredictionsRepository> {
        //            object : ITripPredictionsRepository {
        //                override fun connect(
        //                    tripId: String,
        //                    onReceive: (Outcome<PredictionsStreamDataResponse?, SocketError>) ->
        // Unit
        //                ) {
        //                    TODO("Not yet implemented")
        //                }
        //
        //                override fun disconnect() {
        //                    TODO("Not yet implemented")
        //                }
        //            }
        //        }
        //        single<IVehicleRepository> {
        //            object : IVehicleRepository {
        //                override fun connect(
        //                    vehicleId: String,
        //                    onReceive: (Outcome<VehicleStreamDataResponse?, SocketError>) -> Unit
        //                ) {
        //                    TODO("Not yet implemented")
        //                }
        //
        //                override fun disconnect() {
        //                    TODO("Not yet implemented")
        //                }
        //            }
        //        }
        //        single<IGlobalRepository> {
        //            object : IGlobalRepository {
        //                override suspend fun getGlobalData() =
        //                    GlobalResponse(
        //                        objects,
        //                        mapOf(stopParkStreet.id to listOf(patternAlewife.id,
        // patternAshmont.id))
        //                    )
        //            }
        //        }
        //        single<ISearchResultRepository> { SearchResultRepository() }
        //        single<IRailRouteShapeRepository> { RailRouteShapeRepository() }
        //        single { TogglePinnedRouteUsecase(get()) }
        //        single { GetSettingUsecase(get()) }
        //        single { ConfigUseCase(get(), get()) }
    }
}
