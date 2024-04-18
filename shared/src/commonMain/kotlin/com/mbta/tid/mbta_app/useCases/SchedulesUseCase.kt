import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface ISchedulesUseCase {

    suspend fun getSchedule(stopIds: List<String>, now: Instant): ScheduleResponse

    suspend fun getSchedule(stopIds: List<String>): ScheduleResponse
}

class SchedulesUseCase(repository: ISchedulesRepository) : ISchedulesUseCase {
    private val repository = repository

    override suspend fun getSchedule(
        stopIds: List<String>,
    ): ScheduleResponse {
        return getSchedule(stopIds, Clock.System.now())
    }

    override suspend fun getSchedule(stopIds: List<String>, now: Instant): ScheduleResponse {
        return repository.getSchedule(stopIds, now)
    }
}

class EmptySchedulesUseCase() : ISchedulesUseCase {
    override suspend fun getSchedule(stopIds: List<String>, now: Instant): ScheduleResponse {
        return ScheduleResponse(schedules = listOf(), trips = mapOf())
    }

    override suspend fun getSchedule(stopIds: List<String>): ScheduleResponse {
        return ScheduleResponse(schedules = listOf(), trips = mapOf())
    }
}
