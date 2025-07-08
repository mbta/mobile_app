package com.mbta.tid.mbta_app.endToEnd

import com.mbta.tid.mbta_app.model.AppVersion
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.IAccessibilityStatusRepository
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import com.mbta.tid.mbta_app.repositories.IConfigRepository
import com.mbta.tid.mbta_app.repositories.ICurrentAppVersionRepository
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.ILastLaunchedAppVersionRepository
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import com.mbta.tid.mbta_app.repositories.IOnboardingRepository
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.IRailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.IRouteStopsRepository
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
import com.mbta.tid.mbta_app.repositories.MockAccessibilityStatusRepository
import com.mbta.tid.mbta_app.repositories.MockAlertsRepository
import com.mbta.tid.mbta_app.repositories.MockConfigRepository
import com.mbta.tid.mbta_app.repositories.MockCurrentAppVersionRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockLastLaunchedAppVersionRepository
import com.mbta.tid.mbta_app.repositories.MockNearbyRepository
import com.mbta.tid.mbta_app.repositories.MockOnboardingRepository
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockRouteStopsRepository
import com.mbta.tid.mbta_app.repositories.MockScheduleRepository
import com.mbta.tid.mbta_app.repositories.MockSearchResultRepository
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.MockStopRepository
import com.mbta.tid.mbta_app.repositories.MockTripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockTripRepository
import com.mbta.tid.mbta_app.repositories.MockVehicleRepository
import com.mbta.tid.mbta_app.repositories.MockVehiclesRepository
import com.mbta.tid.mbta_app.repositories.MockVisitHistoryRepository
import com.mbta.tid.mbta_app.usecases.AlertsUsecase
import com.mbta.tid.mbta_app.usecases.ConfigUseCase
import com.mbta.tid.mbta_app.usecases.FeaturePromoUseCase
import com.mbta.tid.mbta_app.usecases.TogglePinnedRouteUsecase
import com.mbta.tid.mbta_app.usecases.VisitHistoryUsecase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
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
    val globalData =
        GlobalResponse(
            objects,
            mapOf(stopParkStreet.id to listOf(patternAlewife.id, patternAshmont.id)),
        )
    return module {
        single<IAccessibilityStatusRepository> {
            MockAccessibilityStatusRepository(isScreenReaderEnabled = true)
        }
        single<IAlertsRepository> { MockAlertsRepository(AlertsStreamDataResponse(objects)) }
        single<IConfigRepository> { MockConfigRepository() }
        single<ICurrentAppVersionRepository> {
            MockCurrentAppVersionRepository(AppVersion(0u, 0u, 0u))
        }
        single<IErrorBannerStateRepository> { MockErrorBannerStateRepository() }
        single<IGlobalRepository> { MockGlobalRepository(globalData) }
        single<ILastLaunchedAppVersionRepository> { MockLastLaunchedAppVersionRepository(null) }
        single<INearbyRepository> {
            MockNearbyRepository(
                NearbyResponse(listOf(stopParkStreet.id)),
                listOf(stopParkStreet.id),
            )
        }
        single<IOnboardingRepository> { MockOnboardingRepository() }
        single<IPinnedRoutesRepository> {
            object : IPinnedRoutesRepository {
                override suspend fun getPinnedRoutes() = emptySet<String>()

                override suspend fun setPinnedRoutes(routes: Set<String>) {}
            }
        }
        single<IPredictionsRepository> {
            MockPredictionsRepository(connectV2Response = PredictionsByStopJoinResponse(objects))
        }
        single<IRailRouteShapeRepository> {
            // If this returns a response (or maybe just an empty response, I didn't keep debugging
            // once I found something that worked), the end-to-end test will fail in Xcode Cloud
            // because the main thread stays busy for 30 seconds, and it will be a nightmare to
            // debug.
            IdleRailRouteShapeRepository()
        }
        single<IRouteStopsRepository> { MockRouteStopsRepository(emptyList()) }
        single<ISchedulesRepository> {
            MockScheduleRepository(scheduleResponse = ScheduleResponse(objects))
        }
        single<ISearchResultRepository> { MockSearchResultRepository() }
        single<ISentryRepository> { MockSentryRepository() }
        single<ISettingsRepository> { MockSettingsRepository() }
        single<IStopRepository> { MockStopRepository() }
        single<ITripRepository> { MockTripRepository() }
        single<ITripPredictionsRepository> { MockTripPredictionsRepository() }
        single<IVehicleRepository> { MockVehicleRepository() }
        single<IVehiclesRepository> { MockVehiclesRepository() }
        single<IVisitHistoryRepository> { MockVisitHistoryRepository() }
        single { AlertsUsecase(get(), get()) }
        single { ConfigUseCase(get(), get()) }
        single { FeaturePromoUseCase(get(), get()) }
        single { TogglePinnedRouteUsecase(get()) }
        single { VisitHistoryUsecase(get()) }
    }
}
