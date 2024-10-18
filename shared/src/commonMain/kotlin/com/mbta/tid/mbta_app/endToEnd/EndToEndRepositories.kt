package com.mbta.tid.mbta_app.endToEnd

import com.mbta.tid.mbta_app.model.Coordinate
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripShape
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopMessageResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.model.response.StopMapResponse
import com.mbta.tid.mbta_app.model.response.TripResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import com.mbta.tid.mbta_app.model.response.VehicleStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import com.mbta.tid.mbta_app.repositories.IAppCheckRepository
import com.mbta.tid.mbta_app.repositories.IConfigRepository
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.IRailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.IStopRepository
import com.mbta.tid.mbta_app.repositories.ITripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ITripRepository
import com.mbta.tid.mbta_app.repositories.IVehicleRepository
import com.mbta.tid.mbta_app.repositories.IVehiclesRepository
import com.mbta.tid.mbta_app.repositories.IVisitHistoryRepository
import com.mbta.tid.mbta_app.repositories.IdleRailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.MockAlertsRepository
import com.mbta.tid.mbta_app.repositories.MockAppCheckRepository
import com.mbta.tid.mbta_app.repositories.MockConfigRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockSearchResultRepository
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.MockVehiclesRepository
import com.mbta.tid.mbta_app.repositories.MockVisitHistoryRepository
import com.mbta.tid.mbta_app.usecases.ConfigUseCase
import com.mbta.tid.mbta_app.usecases.GetSettingUsecase
import com.mbta.tid.mbta_app.usecases.TogglePinnedRouteUsecase
import com.mbta.tid.mbta_app.usecases.VisitHistoryUsecase
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
        single<IAlertsRepository> { MockAlertsRepository() }
        single<IAppCheckRepository> { MockAppCheckRepository() }
        single<IConfigRepository> { MockConfigRepository() }
        single<IErrorBannerStateRepository> { MockErrorBannerStateRepository() }
        single<IGlobalRepository> {
            object : IGlobalRepository {
                override suspend fun getGlobalData(): ApiResult<GlobalResponse> =
                    ApiResult.Ok(
                        GlobalResponse(
                            objects,
                            mapOf(stopParkStreet.id to listOf(patternAlewife.id, patternAshmont.id))
                        )
                    )
            }
        }
        single<INearbyRepository> {
            object : INearbyRepository {
                override suspend fun getNearby(
                    global: GlobalResponse,
                    location: Coordinate
                ): ApiResult<NearbyStaticData> =
                    ApiResult.Ok(
                        NearbyStaticData(global, NearbyResponse(listOf(stopParkStreet.id)))
                    )
            }
        }
        single<IPinnedRoutesRepository> {
            object : IPinnedRoutesRepository {
                override suspend fun getPinnedRoutes() = emptySet<String>()

                override suspend fun setPinnedRoutes(routes: Set<String>) {}
            }
        }
        single<IPredictionsRepository> {
            object : IPredictionsRepository {
                override fun connect(
                    stopIds: List<String>,
                    onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit
                ) {
                    onReceive(ApiResult.Ok(PredictionsStreamDataResponse(objects)))
                }

                override var lastUpdated: Instant? = null

                override fun shouldForgetPredictions(predictionCount: Int) = false

                override fun connectV2(
                    stopIds: List<String>,
                    onJoin: (ApiResult<PredictionsByStopJoinResponse>) -> Unit,
                    onMessage: (ApiResult<PredictionsByStopMessageResponse>) -> Unit
                ) {
                    onJoin(ApiResult.Ok(PredictionsByStopJoinResponse(objects)))
                }

                override fun disconnect() {}
            }
        }
        single<IRailRouteShapeRepository> {
            // If this returns a response (or maybe just an empty response, I didn't keep debugging
            // once I found something that worked), the end-to-end test will fail in Xcode Cloud
            // because the main thread stays busy for 30 seconds, and it will be a nightmare to
            // debug.
            IdleRailRouteShapeRepository()
        }
        single<ISchedulesRepository> {
            object : ISchedulesRepository {
                override suspend fun getSchedule(
                    stopIds: List<String>,
                    now: Instant
                ): ApiResult<ScheduleResponse> = ApiResult.Ok(ScheduleResponse(objects))

                override suspend fun getSchedule(
                    stopIds: List<String>
                ): ApiResult<ScheduleResponse> = getSchedule(stopIds, Clock.System.now())
            }
        }
        single<ISearchResultRepository> { MockSearchResultRepository() }
        single<ISentryRepository> { MockSentryRepository() }
        single<ISettingsRepository> { MockSettingsRepository() }
        single<IStopRepository> {
            object : IStopRepository {
                override suspend fun getStopMapData(stopId: String): ApiResult<StopMapResponse> =
                    ApiResult.Ok(StopMapResponse(emptyList(), emptyMap()))
            }
        }
        single<ITripRepository> {
            object : ITripRepository {
                override suspend fun getTripSchedules(
                    tripId: String
                ): ApiResult<TripSchedulesResponse> {
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
        single<ITripPredictionsRepository> {
            object : ITripPredictionsRepository {
                override fun connect(
                    tripId: String,
                    onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit
                ) {
                    TODO("Not yet implemented")
                }

                override var lastUpdated: Instant? = null

                override fun shouldForgetPredictions(predictionCount: Int) = false

                override fun disconnect() {
                    TODO("Not yet implemented")
                }
            }
        }
        single<IVehicleRepository> {
            object : IVehicleRepository {
                override fun connect(
                    vehicleId: String,
                    onReceive: (ApiResult<VehicleStreamDataResponse>) -> Unit
                ) {
                    TODO("Not yet implemented")
                }

                override fun disconnect() {
                    TODO("Not yet implemented")
                }
            }
        }
        single<IVehiclesRepository> { MockVehiclesRepository() }
        single<IVisitHistoryRepository> { MockVisitHistoryRepository() }
        single { ConfigUseCase(get(), get(), get()) }
        single { GetSettingUsecase(get()) }
        single { TogglePinnedRouteUsecase(get()) }
        single { VisitHistoryUsecase(get()) }
    }
}
