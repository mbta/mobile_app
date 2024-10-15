import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.repositories.ConfigRepository
import com.mbta.tid.mbta_app.repositories.ErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.GlobalRepository
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
import com.mbta.tid.mbta_app.repositories.IdleGlobalRepository
import com.mbta.tid.mbta_app.repositories.IdleNearbyRepository
import com.mbta.tid.mbta_app.repositories.IdleRailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.IdleScheduleRepository
import com.mbta.tid.mbta_app.repositories.IdleSearchResultRepository
import com.mbta.tid.mbta_app.repositories.IdleStopRepository
import com.mbta.tid.mbta_app.repositories.IdleTripRepository
import com.mbta.tid.mbta_app.repositories.MockAlertsRepository
import com.mbta.tid.mbta_app.repositories.MockAppCheckRepository
import com.mbta.tid.mbta_app.repositories.MockConfigRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.MockTripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockVehicleRepository
import com.mbta.tid.mbta_app.repositories.MockVehiclesRepository
import com.mbta.tid.mbta_app.repositories.NearbyRepository
import com.mbta.tid.mbta_app.repositories.PinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.RailRouteShapeRepository
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
    val alerts: IAlertsRepository?
    val appCheck: IAppCheckRepository?
    val config: IConfigRepository
    val errorBanner: IErrorBannerStateRepository
    val global: IGlobalRepository
    val nearby: INearbyRepository
    val pinnedRoutes: IPinnedRoutesRepository
    val predictions: IPredictionsRepository?
    val railRouteShapes: IRailRouteShapeRepository
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
}

class RepositoryDI : IRepositories, KoinComponent {
    override val alerts: IAlertsRepository by inject()
    override val appCheck: IAppCheckRepository by inject()
    override val config: IConfigRepository by inject()
    override val errorBanner: IErrorBannerStateRepository by inject()
    override val global: IGlobalRepository by inject()
    override val nearby: INearbyRepository by inject()
    override val pinnedRoutes: IPinnedRoutesRepository by inject()
    override val predictions: IPredictionsRepository by inject()
    override val railRouteShapes: IRailRouteShapeRepository by inject()
    override val schedules: ISchedulesRepository by inject()
    override val searchResults: ISearchResultRepository by inject()
    override val settings: ISettingsRepository by inject()
    override val sentry: ISentryRepository by inject()
    override val stop: IStopRepository by inject()
    override val trip: ITripRepository by inject()
    override val tripPredictions: ITripPredictionsRepository by inject()
    override val vehicle: IVehicleRepository by inject()
    override val vehicles: IVehiclesRepository by inject()
    override val visitHistory: IVisitHistoryRepository by inject()
}

class RealRepositories : IRepositories {
    // initialize repositories with platform-specific dependencies as null.
    // instantiate the real repositories in makeNativeModule
    override val alerts = null
    override val appCheck = null
    override val config = ConfigRepository()
    override val errorBanner = ErrorBannerStateRepository()
    override val global = GlobalRepository()
    override val nearby = NearbyRepository()
    override val pinnedRoutes = PinnedRoutesRepository()
    override val predictions = null
    override val railRouteShapes = RailRouteShapeRepository()
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
}

class MockRepositories(
    override val alerts: IAlertsRepository,
    override val appCheck: IAppCheckRepository,
    override val config: IConfigRepository,
    override val errorBanner: IErrorBannerStateRepository,
    override val global: IGlobalRepository,
    override val nearby: INearbyRepository,
    override val pinnedRoutes: IPinnedRoutesRepository,
    override val predictions: IPredictionsRepository,
    override val railRouteShapes: IRailRouteShapeRepository,
    override val schedules: ISchedulesRepository,
    override val searchResults: ISearchResultRepository,
    override val sentry: ISentryRepository,
    override val settings: ISettingsRepository,
    override val stop: IStopRepository,
    override val trip: ITripRepository,
    override val tripPredictions: ITripPredictionsRepository,
    override val vehicle: IVehicleRepository,
    override val vehicles: IVehiclesRepository,
    override val visitHistory: IVisitHistoryRepository
) : IRepositories {
    companion object {
        @DefaultArgumentInterop.Enabled
        fun buildWithDefaults(
            errorBanner: IErrorBannerStateRepository = MockErrorBannerStateRepository(),
            global: IGlobalRepository = IdleGlobalRepository(),
            schedules: ISchedulesRepository = IdleScheduleRepository(),
            stop: IStopRepository = IdleStopRepository(),
            trip: ITripRepository = IdleTripRepository(),
        ): MockRepositories {
            return MockRepositories(
                alerts = MockAlertsRepository(),
                appCheck = MockAppCheckRepository(),
                config = MockConfigRepository(),
                errorBanner = errorBanner,
                global = global,
                nearby = IdleNearbyRepository(),
                pinnedRoutes = PinnedRoutesRepository(),
                predictions = MockPredictionsRepository(),
                railRouteShapes = IdleRailRouteShapeRepository(),
                schedules = schedules,
                searchResults = IdleSearchResultRepository(),
                sentry = MockSentryRepository(),
                settings = MockSettingsRepository(),
                stop = stop,
                trip = trip,
                tripPredictions = MockTripPredictionsRepository(),
                vehicle = MockVehicleRepository(),
                vehicles = MockVehiclesRepository(),
                visitHistory = VisitHistoryRepository()
            )
        }
    }
}
