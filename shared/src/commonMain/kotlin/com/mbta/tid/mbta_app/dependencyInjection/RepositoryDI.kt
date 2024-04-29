import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.IdleScheduleRepository
import com.mbta.tid.mbta_app.repositories.PinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.SchedulesRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IRepositories {
    val schedules: ISchedulesRepository
    val pinnedRoutesRepository: IPinnedRoutesRepository
}

class RepositoryDI() : IRepositories, KoinComponent {
    override val schedules: ISchedulesRepository by inject()
    override val pinnedRoutesRepository: IPinnedRoutesRepository by inject()
}

class RealRepositories : IRepositories {
    override val schedules = SchedulesRepository()
    override val pinnedRoutesRepository = PinnedRoutesRepository()
}

class MockRepositories(
    override val schedules: ISchedulesRepository,
    override val pinnedRoutesRepository: IPinnedRoutesRepository
) : IRepositories {
    companion object {
        @DefaultArgumentInterop.Enabled
        fun buildWithDefaults(
            schedules: ISchedulesRepository = IdleScheduleRepository(),
            pinnedRoutesRepository: IPinnedRoutesRepository = PinnedRoutesRepository()
        ): MockRepositories {
            return MockRepositories(
                schedules = schedules,
                pinnedRoutesRepository = pinnedRoutesRepository
            )
        }
    }
}
