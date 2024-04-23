import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RepositoryDI() : KoinComponent {
    val schedules: ISchedulesRepository by inject()
    val pinnedRoutesRepository: IPinnedRoutesRepository by inject()
}
