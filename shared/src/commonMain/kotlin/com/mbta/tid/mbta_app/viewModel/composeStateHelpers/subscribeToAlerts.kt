package com.mbta.tid.mbta_app.viewModel.composeStateHelpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.usecases.AlertsUsecase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
internal fun subscribeToAlerts(
    alertsUsecase: AlertsUsecase = koinInject(),
    ioDispatcher: CoroutineDispatcher = koinInject(named("coroutineDispatcherIO")),
): AlertsStreamDataResponse? {
    var alerts by remember { mutableStateOf<AlertsStreamDataResponse?>(null) }

    fun connect() {
        alertsUsecase.connect {
            when (it) {
                is ApiResult.Ok -> {
                    alerts = it.data
                }
                is ApiResult.Error -> {
                    println("subscribeToAlerts got error: $it")
                }
            }
        }
    }

    DisposableEffect(key1 = null) {
        CoroutineScope(ioDispatcher).launch { connect() }

        onDispose { alertsUsecase.disconnect() }
    }

    return alerts
}
