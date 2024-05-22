import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.IStopRepository
import com.mbta.tid.mbta_app.repositories.ITripSchedulesRepository
import com.mbta.tid.mbta_app.repositories.IdleScheduleRepository
import com.mbta.tid.mbta_app.repositories.IdleStopRepository
import com.mbta.tid.mbta_app.repositories.IdleTripSchedulesRepository
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import com.mbta.tid.mbta_app.repositories.PinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.SchedulesRepository
import com.mbta.tid.mbta_app.repositories.SettingsRepository
import com.mbta.tid.mbta_app.repositories.StopRepository
import com.mbta.tid.mbta_app.repositories.TripSchedulesRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IRepositories {
    val pinnedRoutesRepository: IPinnedRoutesRepository
    val schedules: ISchedulesRepository
    val settings: ISettingsRepository
    val stop: IStopRepository
    val tripSchedules: ITripSchedulesRepository
    val predictions: IPredictionsRepository?
}

class RepositoryDI : IRepositories, KoinComponent {
    override val pinnedRoutesRepository: IPinnedRoutesRepository by inject()
    override val schedules: ISchedulesRepository by inject()
    override val settings: ISettingsRepository by inject()
    override val stop: IStopRepository by inject()
    override val tripSchedules: ITripSchedulesRepository by inject()
    override val predictions: IPredictionsRepository by inject()
}

class RealRepositories : IRepositories {
    override val pinnedRoutesRepository = PinnedRoutesRepository()
    override val schedules = SchedulesRepository()
    override val settings = SettingsRepository()
    override val stop = StopRepository()
    override val tripSchedules = TripSchedulesRepository()
    override val predictions = null
}

class MockRepositories(
    override val pinnedRoutesRepository: IPinnedRoutesRepository,
    override val schedules: ISchedulesRepository,
    override val settings: ISettingsRepository,
    override val stop: IStopRepository,
    override val tripSchedules: ITripSchedulesRepository,
    override val predictions: IPredictionsRepository
) : IRepositories {
    companion object {
        @DefaultArgumentInterop.Enabled
        fun buildWithDefaults(
            pinnedRoutesRepository: IPinnedRoutesRepository = PinnedRoutesRepository(),
            schedules: ISchedulesRepository = IdleScheduleRepository(),
            settings: ISettingsRepository = SettingsRepository(),
            stop: IStopRepository = IdleStopRepository(),
            tripSchedules: ITripSchedulesRepository = IdleTripSchedulesRepository(),
            predictions: IPredictionsRepository = MockPredictionsRepository()
        ): MockRepositories {
            return MockRepositories(
                pinnedRoutesRepository = pinnedRoutesRepository,
                schedules = schedules,
                settings = settings,
                stop = stop,
                tripSchedules = tripSchedules,
                predictions = predictions
            )
        }
    }
}
