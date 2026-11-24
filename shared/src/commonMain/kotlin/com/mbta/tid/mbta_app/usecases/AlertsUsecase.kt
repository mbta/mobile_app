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

    private var currentAlerts: AlertsStreamDataResponse? = null

    private var globalState = globalRepository.state
    private var globalUpdateJob: Job? = null

    public fun connect(onReceive: (ApiResult<AlertsStreamDataResponse>) -> Unit) {
        fun injectAndReceive(alerts: AlertsStreamDataResponse) {
            val injectedAlerts = alerts.injectFacilities(globalState.value)
            currentAlerts = injectedAlerts
            onReceive(ApiResult.Ok(injectedAlerts))
        }
        alertsRepository.connect(
            onJoin = { result ->
                when (result) {
                    is ApiResult.Ok ->
                        injectAndReceive(AlertsStreamDataResponse(result.data.update))
                    is ApiResult.Error -> onReceive(ApiResult.Error(result.code, result.message))
                }
            },
            onReceive = { result ->
                when (result) {
                    is ApiResult.Ok -> injectAndReceive(result.data.mergeInto(currentAlerts))
                    is ApiResult.Error -> onReceive(ApiResult.Error(result.code, result.message))
                }
            },
        )

        globalUpdateJob?.cancel()
        globalUpdateJob =
            CoroutineScope(globalUpdateDispatcher).launch {
                globalState.collect { global ->
                    currentAlerts?.let { alerts ->
                        onReceive(ApiResult.Ok(alerts.injectFacilities(global)))
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
