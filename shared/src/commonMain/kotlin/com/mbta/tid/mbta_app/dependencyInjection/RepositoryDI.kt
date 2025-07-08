package com.mbta.tid.mbta_app.dependencyInjection

import com.mbta.tid.mbta_app.model.AppVersion
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.model.response.TripResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import com.mbta.tid.mbta_app.model.response.VehicleStreamDataResponse
import com.mbta.tid.mbta_app.repositories.ConfigRepository
import com.mbta.tid.mbta_app.repositories.ErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.FavoritesRepository
import com.mbta.tid.mbta_app.repositories.GlobalRepository
import com.mbta.tid.mbta_app.repositories.IAccessibilityStatusRepository
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import com.mbta.tid.mbta_app.repositories.IConfigRepository
import com.mbta.tid.mbta_app.repositories.ICurrentAppVersionRepository
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IFavoritesRepository
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
import com.mbta.tid.mbta_app.repositories.IdleGlobalRepository
import com.mbta.tid.mbta_app.repositories.IdleNearbyRepository
import com.mbta.tid.mbta_app.repositories.IdleRailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.IdleScheduleRepository
import com.mbta.tid.mbta_app.repositories.IdleSearchResultRepository
import com.mbta.tid.mbta_app.repositories.IdleStopRepository
import com.mbta.tid.mbta_app.repositories.IdleTripRepository
import com.mbta.tid.mbta_app.repositories.LastLaunchedAppVersionRepository
import com.mbta.tid.mbta_app.repositories.MockAccessibilityStatusRepository
import com.mbta.tid.mbta_app.repositories.MockAlertsRepository
import com.mbta.tid.mbta_app.repositories.MockConfigRepository
import com.mbta.tid.mbta_app.repositories.MockCurrentAppVersionRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockFavoritesRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockLastLaunchedAppVersionRepository
import com.mbta.tid.mbta_app.repositories.MockNearbyRepository
import com.mbta.tid.mbta_app.repositories.MockOnboardingRepository
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockRailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.MockRouteStopsRepository
import com.mbta.tid.mbta_app.repositories.MockScheduleRepository
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.MockTripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockTripRepository
import com.mbta.tid.mbta_app.repositories.MockVehicleRepository
import com.mbta.tid.mbta_app.repositories.MockVehiclesRepository
import com.mbta.tid.mbta_app.repositories.NearbyRepository
import com.mbta.tid.mbta_app.repositories.OnboardingRepository
import com.mbta.tid.mbta_app.repositories.PinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.RailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.RouteStopsRepository
import com.mbta.tid.mbta_app.repositories.SchedulesRepository
import com.mbta.tid.mbta_app.repositories.SearchResultRepository
import com.mbta.tid.mbta_app.repositories.SentryRepository
import com.mbta.tid.mbta_app.repositories.SettingsRepository
import com.mbta.tid.mbta_app.repositories.StopRepository
import com.mbta.tid.mbta_app.repositories.TripRepository
import com.mbta.tid.mbta_app.repositories.VisitHistoryRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IRepositories {
    val accessibilityStatus: IAccessibilityStatusRepository?
    val alerts: IAlertsRepository?
    val config: IConfigRepository
    val currentAppVersion: ICurrentAppVersionRepository?
    val errorBanner: IErrorBannerStateRepository
    val global: IGlobalRepository
    val lastLaunchedAppVersion: ILastLaunchedAppVersionRepository
    val nearby: INearbyRepository
    val onboarding: IOnboardingRepository
    val pinnedRoutes: IPinnedRoutesRepository
    val predictions: IPredictionsRepository?
    val railRouteShapes: IRailRouteShapeRepository
    val routeStops: IRouteStopsRepository
    val schedules: ISchedulesRepository
    val searchResults: ISearchResultRepository
    val sentry: ISentryRepository
    val settings: ISettingsRepository
    val stop: IStopRepository
    val trip: ITripRepository
    val tripPredictions: ITripPredictionsRepository?
    val vehicle: IVehicleRepository?
    val vehicles: IVehiclesRepository?
    val visitHistory: IVisitHistoryRepository
    val favorites: IFavoritesRepository
}

class RepositoryDI : IRepositories, KoinComponent {
    override val accessibilityStatus: IAccessibilityStatusRepository by inject()
    override val alerts: IAlertsRepository by inject()
    override val config: IConfigRepository by inject()
    override val currentAppVersion: ICurrentAppVersionRepository by inject()
    override val errorBanner: IErrorBannerStateRepository by inject()
    override val global: IGlobalRepository by inject()
    override val lastLaunchedAppVersion: ILastLaunchedAppVersionRepository by inject()
    override val nearby: INearbyRepository by inject()
    override val onboarding: IOnboardingRepository by inject()
    override val pinnedRoutes: IPinnedRoutesRepository by inject()
    override val predictions: IPredictionsRepository by inject()
    override val railRouteShapes: IRailRouteShapeRepository by inject()
    override val routeStops: IRouteStopsRepository by inject()
    override val schedules: ISchedulesRepository by inject()
    override val searchResults: ISearchResultRepository by inject()
    override val sentry: ISentryRepository by inject()
    override val settings: ISettingsRepository by inject()
    override val stop: IStopRepository by inject()
    override val trip: ITripRepository by inject()
    override val tripPredictions: ITripPredictionsRepository by inject()
    override val vehicle: IVehicleRepository by inject()
    override val vehicles: IVehiclesRepository by inject()
    override val visitHistory: IVisitHistoryRepository by inject()
    override val favorites: IFavoritesRepository by inject()
}

class RealRepositories : IRepositories {
    // initialize repositories with platform-specific dependencies as null.
    // instantiate the real repositories in makeNativeModule

    override val accessibilityStatus = null
    override val alerts = null
    override val config = ConfigRepository()
    override val currentAppVersion = null
    override val errorBanner = ErrorBannerStateRepository()
    override val global = GlobalRepository()
    override val lastLaunchedAppVersion = LastLaunchedAppVersionRepository()
    override val nearby = NearbyRepository()
    override val onboarding = OnboardingRepository()
    override val pinnedRoutes = PinnedRoutesRepository()
    override val predictions = null
    override val railRouteShapes = RailRouteShapeRepository()
    override val routeStops = RouteStopsRepository()
    override val schedules = SchedulesRepository()
    override val searchResults = SearchResultRepository()
    override val sentry = SentryRepository()
    override val settings = SettingsRepository()
    override val stop = StopRepository()
    override val trip = TripRepository()
    override val tripPredictions = null
    override val vehicle = null
    override val vehicles = null
    override val visitHistory = VisitHistoryRepository()
    override val favorites = FavoritesRepository()
}

class MockRepositories : IRepositories {
    override var accessibilityStatus: IAccessibilityStatusRepository =
        MockAccessibilityStatusRepository(isScreenReaderEnabled = false)
    override var alerts: IAlertsRepository = MockAlertsRepository()
    override var config: IConfigRepository = MockConfigRepository()
    override var currentAppVersion: ICurrentAppVersionRepository =
        MockCurrentAppVersionRepository(AppVersion(0u, 0u, 0u))
    override var errorBanner: IErrorBannerStateRepository = MockErrorBannerStateRepository()
    override var global: IGlobalRepository = IdleGlobalRepository()
    override var lastLaunchedAppVersion: ILastLaunchedAppVersionRepository =
        MockLastLaunchedAppVersionRepository(null)
    override var nearby: INearbyRepository = IdleNearbyRepository()
    override var onboarding: IOnboardingRepository = MockOnboardingRepository()
    override var pinnedRoutes: IPinnedRoutesRepository = PinnedRoutesRepository()
    override var predictions: IPredictionsRepository = MockPredictionsRepository()
    override var railRouteShapes: IRailRouteShapeRepository = IdleRailRouteShapeRepository()
    override var routeStops: IRouteStopsRepository = MockRouteStopsRepository(emptyList())
    override var schedules: ISchedulesRepository = IdleScheduleRepository()
    override var searchResults: ISearchResultRepository = IdleSearchResultRepository()
    override var sentry: ISentryRepository = MockSentryRepository()
    override var settings: ISettingsRepository = MockSettingsRepository()
    override var stop: IStopRepository = IdleStopRepository()
    override var trip: ITripRepository = IdleTripRepository()
    override var tripPredictions: ITripPredictionsRepository = MockTripPredictionsRepository()
    override var vehicle: IVehicleRepository = MockVehicleRepository()
    override var vehicles: IVehiclesRepository = MockVehiclesRepository()
    override var visitHistory: IVisitHistoryRepository = VisitHistoryRepository()
    override var favorites: IFavoritesRepository = MockFavoritesRepository()

    fun useObjects(objects: ObjectCollectionBuilder) {
        alerts = MockAlertsRepository(AlertsStreamDataResponse(objects))
        global = MockGlobalRepository(GlobalResponse(objects))
        nearby = MockNearbyRepository(NearbyResponse(objects))
        predictions =
            MockPredictionsRepository(connectV2Response = PredictionsByStopJoinResponse(objects))
        schedules = MockScheduleRepository(ScheduleResponse(objects))
        trip =
            MockTripRepository(
                TripSchedulesResponse.Schedules(objects.schedules.values.toList()),
                TripResponse(
                    objects.trips.values.singleOrNull() ?: ObjectCollectionBuilder.Single.trip()
                ),
            )
        tripPredictions =
            MockTripPredictionsRepository(response = PredictionsStreamDataResponse(objects))
        vehicle =
            MockVehicleRepository(
                outcome =
                    ApiResult.Ok(VehicleStreamDataResponse(objects.vehicles.values.singleOrNull()))
            )
        vehicles = MockVehiclesRepository(objects)
        railRouteShapes = MockRailRouteShapeRepository(objects)
    }
}
