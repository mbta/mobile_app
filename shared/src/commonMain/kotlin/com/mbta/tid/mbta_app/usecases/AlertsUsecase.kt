package com.mbta.tid.mbta_app.usecases

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

public class AlertsUsecase
@DefaultArgumentInterop.Enabled
constructor(
    private val alertsRepository: IAlertsRepository,
    globalRepository: IGlobalRepository,
    private val globalUpdateDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : KoinComponent {

    private var lastOkResult: ApiResult.Ok<AlertsStreamDataResponse>? = null
    private var globalState = globalRepository.state
    private var globalUpdateJob: Job? = null

    public fun connect(onReceive: (ApiResult<AlertsStreamDataResponse>) -> Unit) {
        fun injectAndReceive(result: ApiResult<AlertsStreamDataResponse>) {
            val injectedResult =
                if (result is ApiResult.Ok) {
                    lastOkResult = result
                    result.copy(result.data.injectFacilities(globalState.value))
                } else result
            onReceive(injectedResult)
        }
        alertsRepository.connect(::injectAndReceive)

        globalUpdateJob?.cancel()
        globalUpdateJob =
            CoroutineScope(globalUpdateDispatcher).launch {
                globalState.collect { global ->
                    lastOkResult?.let { result ->
                        onReceive(result.copy(result.data.injectFacilities(global)))
                    }
                }
            }
    }

    public fun disconnect() {
        alertsRepository.disconnect()
        globalUpdateJob?.cancel()
        globalUpdateJob = null
    }
}
