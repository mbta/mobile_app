import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.IStopRepository
import com.mbta.tid.mbta_app.repositories.ITripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ITripRepository
import com.mbta.tid.mbta_app.repositories.IVehicleRepository
import com.mbta.tid.mbta_app.repositories.IdleNearbyRepository
import com.mbta.tid.mbta_app.repositories.IdleScheduleRepository
import com.mbta.tid.mbta_app.repositories.IdleStopRepository
import com.mbta.tid.mbta_app.repositories.IdleTripRepository
import com.mbta.tid.mbta_app.repositories.MockAlertsRepository
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockTripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockVehicleRepository
import com.mbta.tid.mbta_app.repositories.NearbyRepository
import com.mbta.tid.mbta_app.repositories.PinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.SchedulesRepository
import com.mbta.tid.mbta_app.repositories.SettingsRepository
import com.mbta.tid.mbta_app.repositories.StopRepository
import com.mbta.tid.mbta_app.repositories.TripRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IRepositories {
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
}

class RepositoryDI : IRepositories, KoinComponent {
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
}

class RealRepositories : IRepositories {
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
}

class MockRepositories(
    override val pinnedRoutes: IPinnedRoutesRepository,
    override val schedules: ISchedulesRepository,
    override val settings: ISettingsRepository,
    override val stop: IStopRepository,
    override val trip: ITripRepository,
    override val predictions: IPredictionsRepository,
    override val alerts: IAlertsRepository,
    override val nearby: INearbyRepository,
    override val tripPredictions: ITripPredictionsRepository,
    override val vehicle: IVehicleRepository
) : IRepositories {
    companion object {
        @DefaultArgumentInterop.Enabled
        @DefaultArgumentInterop.MaximumDefaultArgumentCount(99)
        fun buildWithDefaults(
            pinnedRoutes: IPinnedRoutesRepository = PinnedRoutesRepository(),
            schedules: ISchedulesRepository = IdleScheduleRepository(),
            settings: ISettingsRepository = SettingsRepository(),
            stop: IStopRepository = IdleStopRepository(),
            trip: ITripRepository = IdleTripRepository(),
            predictions: IPredictionsRepository = MockPredictionsRepository(),
            alerts: IAlertsRepository = MockAlertsRepository(),
            nearby: INearbyRepository = IdleNearbyRepository(),
            tripPredictions: ITripPredictionsRepository = MockTripPredictionsRepository(),
            vehicle: IVehicleRepository = MockVehicleRepository()
        ): MockRepositories {
            return MockRepositories(
                pinnedRoutes = pinnedRoutes,
                schedules = schedules,
                settings = settings,
                stop = stop,
                trip = trip,
                predictions = predictions,
                alerts = alerts,
                nearby = nearby,
                tripPredictions = tripPredictions,
                vehicle = vehicle
            )
        }
    }
}
