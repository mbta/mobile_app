import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UseCaseDI() : KoinComponent {
    val schedules: ISchedulesUseCase by inject()
}
