package com.mbta.tid.mbta_app.endToEnd

import com.mbta.tid.mbta_app.model.Coordinate
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Outcome
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SearchResults
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.TripShape
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.model.response.StopMapResponse
import com.mbta.tid.mbta_app.model.response.TripResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import com.mbta.tid.mbta_app.model.response.VehicleStreamDataResponse
import com.mbta.tid.mbta_app.model.response.VehiclesStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import com.mbta.tid.mbta_app.repositories.IAppCheckRepository
import com.mbta.tid.mbta_app.repositories.IConfigRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.IRailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.IStopRepository
import com.mbta.tid.mbta_app.repositories.ITripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ITripRepository
import com.mbta.tid.mbta_app.repositories.IVehicleRepository
import com.mbta.tid.mbta_app.repositories.IVehiclesRepository
import com.mbta.tid.mbta_app.repositories.MockAlertsRepository
import com.mbta.tid.mbta_app.repositories.MockAppCheckRepository
import com.mbta.tid.mbta_app.repositories.MockConfigRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.RailRouteShapeRepository
import com.mbta.tid.mbta_app.usecases.ConfigUseCase
import com.mbta.tid.mbta_app.usecases.GetSettingUsecase
import com.mbta.tid.mbta_app.usecases.TogglePinnedRouteUsecase
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
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
        single<IAppCheckRepository> { MockAppCheckRepository() }
        single<IConfigRepository> { MockConfigRepository() }
        single<IPinnedRoutesRepository> {
            object : IPinnedRoutesRepository {
                override suspend fun getPinnedRoutes() = emptySet<String>()

                override suspend fun setPinnedRoutes(routes: Set<String>) {}
            }
        }
        single<ISchedulesRepository> {
            object : ISchedulesRepository {
                override suspend fun getSchedule(stopIds: List<String>, now: Instant) =
                    ScheduleResponse(objects)

                override suspend fun getSchedule(stopIds: List<String>) =
                    getSchedule(stopIds, Clock.System.now())
            }
        }
        single<ISettingsRepository> { MockSettingsRepository() }
        single<IStopRepository> {
            object : IStopRepository {
                override suspend fun getStopMapData(stopId: String): StopMapResponse =
                    StopMapResponse(emptyList(), emptyMap())
            }
        }
        single<ITripRepository> {
            object : ITripRepository {
                override suspend fun getTripSchedules(tripId: String): TripSchedulesResponse {
                    TODO("Not yet implemented")
                }

                override suspend fun getTrip(tripId: String): ApiResult<TripResponse> {
                    TODO("Not yet implemented")
                }

                override suspend fun getTripShape(tripId: String): ApiResult<TripShape> {
                    TODO("Not yet implemented")
                }
            }
        }
        single<IPredictionsRepository> {
            object : IPredictionsRepository {
                override fun connect(
                    stopIds: List<String>,
                    onReceive: (Outcome<PredictionsStreamDataResponse?, SocketError>) -> Unit
                ) {
                    onReceive(Outcome(PredictionsStreamDataResponse(objects), null))
                }

                override fun disconnect() {}
            }
        }
        single<IAlertsRepository> { MockAlertsRepository() }
        single<INearbyRepository> {
            object : INearbyRepository {
                override suspend fun getNearby(global: GlobalResponse, location: Coordinate) =
                    NearbyStaticData(global, NearbyResponse(listOf(stopParkStreet.id)))
            }
        }
        single<ITripPredictionsRepository> {
            object : ITripPredictionsRepository {
                override fun connect(
                    tripId: String,
                    onReceive: (Outcome<PredictionsStreamDataResponse?, SocketError>) -> Unit
                ) {
                    TODO("Not yet implemented")
                }

                override fun disconnect() {
                    TODO("Not yet implemented")
                }
            }
        }
        single<IVehicleRepository> {
            object : IVehicleRepository {
                override fun connect(
                    vehicleId: String,
                    onReceive: (Outcome<VehicleStreamDataResponse?, SocketError>) -> Unit
                ) {
                    TODO("Not yet implemented")
                }

                override fun disconnect() {
                    TODO("Not yet implemented")
                }
            }
        }
        single<IGlobalRepository> {
            object : IGlobalRepository {
                override suspend fun getGlobalData() =
                    GlobalResponse(
                        objects,
                        mapOf(stopParkStreet.id to listOf(patternAlewife.id, patternAshmont.id))
                    )
            }
        }
        single<ISearchResultRepository> {
            object : ISearchResultRepository {
                override suspend fun getSearchResults(query: String): SearchResults? {
                    TODO("Not yet implemented")
                }
            }
        }
        single<IRailRouteShapeRepository> { RailRouteShapeRepository() }
        single<IVehiclesRepository> {
            object : IVehiclesRepository {
                override fun connect(
                    routeId: String,
                    directionId: Int,
                    onReceive: (Outcome<VehiclesStreamDataResponse?, SocketError>) -> Unit
                ) {}

                override fun disconnect() {}
            }
        }
        single { TogglePinnedRouteUsecase(get()) }
        single { GetSettingUsecase(get()) }
        single { ConfigUseCase(get(), get()) }
    }
}
