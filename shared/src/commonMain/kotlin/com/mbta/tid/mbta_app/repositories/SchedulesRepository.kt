package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.cache.ScheduleCache
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.NextScheduleResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.http.path
import io.ktor.utils.io.ioDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

public interface ISchedulesRepository {
    public suspend fun getSchedule(
        stopIds: List<String>,
        now: EasternTimeInstant,
    ): ApiResult<ScheduleResponse>

    public suspend fun getSchedule(stopIds: List<String>): ApiResult<ScheduleResponse>

    public suspend fun getNextSchedule(
        route: LineOrRoute,
        stopId: String,
        directionId: Int,
        now: EasternTimeInstant,
    ): ApiResult<NextScheduleResponse>
}

internal class CachedSchedulesRepository(private val schedulesRepository: ISchedulesRepository) :
    ISchedulesRepository, KoinComponent {

    private val scheduleCache: ScheduleCache by inject()
    private val ioDispatcher: CoroutineDispatcher by inject(named("coroutineDispatcherIO"))

    override suspend fun getSchedule(
        stopIds: List<String>,
        now: EasternTimeInstant,
    ): ApiResult<ScheduleResponse> {
        val cachedSchedules: MutableList<ScheduleResponse> = mutableListOf()
        val requestStopIds: MutableList<String> = mutableListOf()
        stopIds.forEach { id ->
            val cache = scheduleCache.getSchedule(id, now.serviceDate)
            if (cache != null) cachedSchedules.add(cache) else requestStopIds.add(id)
        }
        lateinit var mergedResponse: ScheduleResponse
        if (requestStopIds.isNotEmpty()) {
            val result =
                try {
                    schedulesRepository.getSchedule(requestStopIds, now)
                } catch (e: Exception) {
                    return ApiResult.Error(null, e.toString())
                }
            val response =
                when (result) {
                    is ApiResult.Ok -> result.data
                    is ApiResult.Error -> return result
                }

            withContext(ioDispatcher) {
                requestStopIds.forEach { id ->
                    val relevantSchedules = response.schedules.filter { it.stopId == id }
                    val relevantTrips =
                        response.trips.filter { tripEntry ->
                            relevantSchedules.any { it.tripId == tripEntry.key }
                        }
                    scheduleCache.putSchedule(
                        id,
                        now.serviceDate,
                        ScheduleResponse(relevantSchedules, relevantTrips),
                    )
                }
            }

            mergedResponse = ScheduleResponse.merge(cachedSchedules + response)
        } else {
            mergedResponse = ScheduleResponse.merge(cachedSchedules)
        }
        return ApiResult.Ok(mergedResponse)
    }

    override suspend fun getSchedule(stopIds: List<String>): ApiResult<ScheduleResponse> =
        getSchedule(stopIds, EasternTimeInstant.now())

    override suspend fun getNextSchedule(
        route: LineOrRoute,
        stopId: String,
        directionId: Int,
        now: EasternTimeInstant,
    ): ApiResult<NextScheduleResponse> =
        schedulesRepository.getNextSchedule(route, stopId, directionId, now)
}

internal class SchedulesRepository : ISchedulesRepository, KoinComponent {

    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getSchedule(
        stopIds: List<String>,
        now: EasternTimeInstant,
    ): ApiResult<ScheduleResponse> =
        ApiResult.runCatching {
            mobileBackendClient
                .get {
                    timeout { requestTimeoutMillis = 4000 }
                    url {
                        path("api/schedules")
                        parameters.append("stop_ids", stopIds.joinToString(separator = ","))
                        parameters.append("date_time", now.toString())
                    }
                }
                .body()
        }

    override suspend fun getSchedule(stopIds: List<String>): ApiResult<ScheduleResponse> {
        return getSchedule(stopIds, EasternTimeInstant.now())
    }

    override suspend fun getNextSchedule(
        route: LineOrRoute,
        stopId: String,
        directionId: Int,
        now: EasternTimeInstant,
    ): ApiResult<NextScheduleResponse> =
        ApiResult.runCatching {
            mobileBackendClient
                .get {
                    timeout { requestTimeoutMillis = 4000 }
                    url {
                        path("api/schedules/next")
                        parameters.append(
                            "route",
                            when (route) {
                                is LineOrRoute.Line ->
                                    route.routes.joinToString(separator = ",") { it.id.idText }
                                is LineOrRoute.Route -> route.id.idText
                            },
                        )
                        parameters.append("stop", stopId)
                        parameters.append("direction", directionId.toString())
                        parameters.append("date_time", now.toString())
                    }
                }
                .body()
        }
}

public class MockScheduleRepository(
    private val response: ApiResult<ScheduleResponse>,
    private val nextResponse: ApiResult<NextScheduleResponse>,
    private val callback: (stopIds: List<String>) -> Unit = {},
) : ISchedulesRepository {

    @DefaultArgumentInterop.Enabled
    public constructor(
        scheduleResponse: ScheduleResponse = ScheduleResponse(listOf(), mapOf()),
        nextScheduleResponse: NextScheduleResponse = NextScheduleResponse(null),
        callback: (stopIds: List<String>) -> Unit = {},
    ) : this(ApiResult.Ok(scheduleResponse), ApiResult.Ok(nextScheduleResponse), callback)

    public constructor() :
        this(
            scheduleResponse = ScheduleResponse(schedules = listOf(), trips = mapOf()),
            callback = {},
        )

    override suspend fun getSchedule(
        stopIds: List<String>,
        now: EasternTimeInstant,
    ): ApiResult<ScheduleResponse> {
        callback(stopIds)
        return response
    }

    override suspend fun getSchedule(stopIds: List<String>): ApiResult<ScheduleResponse> {
        callback(stopIds)
        return response
    }

    override suspend fun getNextSchedule(
        route: LineOrRoute,
        stopId: String,
        directionId: Int,
        now: EasternTimeInstant,
    ): ApiResult<NextScheduleResponse> {
        return nextResponse
    }
}

public class IdleScheduleRepository : ISchedulesRepository {
    override suspend fun getSchedule(
        stopIds: List<String>,
        now: EasternTimeInstant,
    ): ApiResult<ScheduleResponse> {
        return suspendCancellableCoroutine {}
    }

    override suspend fun getSchedule(stopIds: List<String>): ApiResult<ScheduleResponse> {
        return suspendCancellableCoroutine {}
    }

    override suspend fun getNextSchedule(
        route: LineOrRoute,
        stopId: String,
        directionId: Int,
        now: EasternTimeInstant,
    ): ApiResult<NextScheduleResponse> {
        return suspendCancellableCoroutine {}
    }
}
