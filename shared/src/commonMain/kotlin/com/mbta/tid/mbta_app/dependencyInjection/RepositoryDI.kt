import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.IStopRepository
import com.mbta.tid.mbta_app.repositories.ITripSchedulesRepository
import com.mbta.tid.mbta_app.repositories.IdleScheduleRepository
import com.mbta.tid.mbta_app.repositories.IdleStopRepository
import com.mbta.tid.mbta_app.repositories.IdleTripSchedulesRepository
import com.mbta.tid.mbta_app.repositories.MockAlertsRepository
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import com.mbta.tid.mbta_app.repositories.PinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.SchedulesRepository
import com.mbta.tid.mbta_app.repositories.SettingsRepository
import com.mbta.tid.mbta_app.repositories.StopRepository
import com.mbta.tid.mbta_app.repositories.TripSchedulesRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IRepositories {
    val pinnedRoutes: IPinnedRoutesRepository
    val schedules: ISchedulesRepository
    val settings: ISettingsRepository
    val stop: IStopRepository
    val tripSchedules: ITripSchedulesRepository
    val predictions: IPredictionsRepository?
    val alerts: IAlertsRepository?
}

class RepositoryDI : IRepositories, KoinComponent {
    override val pinnedRoutes: IPinnedRoutesRepository by inject()
    override val schedules: ISchedulesRepository by inject()
    override val settings: ISettingsRepository by inject()
    override val stop: IStopRepository by inject()
    override val tripSchedules: ITripSchedulesRepository by inject()
    override val predictions: IPredictionsRepository by inject()
    override val alerts: IAlertsRepository by inject()
}

class RealRepositories : IRepositories {
    override val pinnedRoutes = PinnedRoutesRepository()
    override val schedules = SchedulesRepository()
    override val settings = SettingsRepository()
    override val stop = StopRepository()
    override val tripSchedules = TripSchedulesRepository()
    override val predictions = null
    override val alerts = null
}

class MockRepositories(
    override val pinnedRoutes: IPinnedRoutesRepository,
    override val schedules: ISchedulesRepository,
    override val settings: ISettingsRepository,
    override val stop: IStopRepository,
    override val tripSchedules: ITripSchedulesRepository,
    override val predictions: IPredictionsRepository,
    override val alerts: IAlertsRepository
) : IRepositories {
    companion object {
        @DefaultArgumentInterop.Enabled
        fun buildWithDefaults(
            pinnedRoutes: IPinnedRoutesRepository = PinnedRoutesRepository(),
            schedules: ISchedulesRepository = IdleScheduleRepository(),
            settings: ISettingsRepository = SettingsRepository(),
            stop: IStopRepository = IdleStopRepository(),
            tripSchedules: ITripSchedulesRepository = IdleTripSchedulesRepository(),
            predictions: IPredictionsRepository = MockPredictionsRepository(),
            alerts: IAlertsRepository = MockAlertsRepository()
        ): MockRepositories {
            return MockRepositories(
                pinnedRoutes = pinnedRoutes,
                schedules = schedules,
                settings = settings,
                stop = stop,
                tripSchedules = tripSchedules,
                predictions = predictions,
                alerts = alerts
            )
        }
    }
}
