import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.repositories.ConfigRepository
import com.mbta.tid.mbta_app.repositories.GlobalRepository
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
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockTripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockVehicleRepository
import com.mbta.tid.mbta_app.repositories.MockVehiclesRepository
import com.mbta.tid.mbta_app.repositories.NearbyRepository
import com.mbta.tid.mbta_app.repositories.PinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.RailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.SchedulesRepository
import com.mbta.tid.mbta_app.repositories.SearchResultRepository
import com.mbta.tid.mbta_app.repositories.SettingsRepository
import com.mbta.tid.mbta_app.repositories.StopRepository
import com.mbta.tid.mbta_app.repositories.TripRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IRepositories {
    val appCheck: IAppCheckRepository?
    val config: IConfigRepository
    val pinnedRoutes: IPinnedRoutesRepository
    val schedules: ISchedulesRepository
    val settings: ISettingsRepository
    val stop: IStopRepository
    val trip: ITripRepository
    val predictions: IPredictionsRepository?
    val alerts: IAlertsRepository?
    val nearby: INearbyRepository
    val tripPredictions: ITripPredictionsRepository?
    val vehicle: IVehicleRepository?
    val global: IGlobalRepository
    val searchResults: ISearchResultRepository
    val railRouteShapes: IRailRouteShapeRepository
    val vehicles: IVehiclesRepository?
}

class RepositoryDI : IRepositories, KoinComponent {
    override val appCheck: IAppCheckRepository by inject()
    override val config: IConfigRepository by inject()
    override val pinnedRoutes: IPinnedRoutesRepository by inject()
    override val schedules: ISchedulesRepository by inject()
    override val settings: ISettingsRepository by inject()
    override val stop: IStopRepository by inject()
    override val trip: ITripRepository by inject()
    override val predictions: IPredictionsRepository by inject()
    override val alerts: IAlertsRepository by inject()
    override val nearby: INearbyRepository by inject()
    override val tripPredictions: ITripPredictionsRepository by inject()
    override val vehicle: IVehicleRepository by inject()
    override val global: IGlobalRepository by inject()
    override val searchResults: ISearchResultRepository by inject()
    override val railRouteShapes: IRailRouteShapeRepository by inject()
    override val vehicles: IVehiclesRepository by inject()
}

class RealRepositories : IRepositories {
    // initialize repositories with platform-specific dependencies as null.
    // instantiate the real repositories in makeNativeModule
    override val appCheck = null
    override val config = ConfigRepository()
    override val pinnedRoutes = PinnedRoutesRepository()
    override val schedules = SchedulesRepository()
    override val settings = SettingsRepository()
    override val stop = StopRepository()
    override val trip = TripRepository()
    override val predictions = null
    override val alerts = null
    override val nearby = NearbyRepository()
    override val tripPredictions = null
    override val vehicle = null
    override val global = GlobalRepository()
    override val searchResults = SearchResultRepository()
    override val railRouteShapes = RailRouteShapeRepository()
    override val vehicles = null
}

class MockRepositories(
    override val appCheck: IAppCheckRepository,
    override val config: IConfigRepository,
    override val pinnedRoutes: IPinnedRoutesRepository,
    override val schedules: ISchedulesRepository,
    override val settings: ISettingsRepository,
    override val stop: IStopRepository,
    override val trip: ITripRepository,
    override val predictions: IPredictionsRepository,
    override val alerts: IAlertsRepository,
    override val nearby: INearbyRepository,
    override val tripPredictions: ITripPredictionsRepository,
    override val vehicle: IVehicleRepository,
    override val global: IGlobalRepository,
    override val searchResults: ISearchResultRepository,
    override val railRouteShapes: IRailRouteShapeRepository,
    override val vehicles: IVehiclesRepository
) : IRepositories {
    companion object {
        @DefaultArgumentInterop.Enabled
        @DefaultArgumentInterop.MaximumDefaultArgumentCount(99)
        fun buildWithDefaults(
            configRepository: IConfigRepository = MockConfigRepository(),
            pinnedRoutes: IPinnedRoutesRepository = PinnedRoutesRepository(),
            schedules: ISchedulesRepository = IdleScheduleRepository(),
            settings: ISettingsRepository = SettingsRepository(),
            stop: IStopRepository = IdleStopRepository(),
            trip: ITripRepository = IdleTripRepository(),
            predictions: IPredictionsRepository = MockPredictionsRepository(),
            alerts: IAlertsRepository = MockAlertsRepository(),
            nearby: INearbyRepository = IdleNearbyRepository(),
            tripPredictions: ITripPredictionsRepository = MockTripPredictionsRepository(),
            vehicle: IVehicleRepository = MockVehicleRepository(),
            global: IGlobalRepository = IdleGlobalRepository(),
            searchResults: ISearchResultRepository = IdleSearchResultRepository(),
            railRouteShapes: IRailRouteShapeRepository = IdleRailRouteShapeRepository(),
            vehicles: IVehiclesRepository = MockVehiclesRepository()
        ): MockRepositories {
            return MockRepositories(
                appCheck = MockAppCheckRepository(),
                config = configRepository,
                pinnedRoutes = pinnedRoutes,
                schedules = schedules,
                settings = settings,
                stop = stop,
                trip = trip,
                predictions = predictions,
                alerts = alerts,
                nearby = nearby,
                tripPredictions = tripPredictions,
                vehicle = vehicle,
                global = global,
                searchResults = searchResults,
                railRouteShapes = railRouteShapes,
                vehicles = vehicles
            )
        }
    }
}
