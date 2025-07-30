package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.network.INetworkConnectivityMonitor
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

sealed class NetworkStatus {
    data object Connected : NetworkStatus()

    data object Disconnected : NetworkStatus()
}

abstract class IErrorBannerStateRepository(initialState: ErrorBannerState? = null) : KoinComponent {

    private val networkConnectivityMonitor: INetworkConnectivityMonitor by inject()

    /*
    Registers platform-specific observer of network status changes.
     */
    open fun subscribeToNetworkStatusChanges() {
        this.networkConnectivityMonitor.registerListener(
            onNetworkAvailable = { setNetworkStatus(NetworkStatus.Connected) },
            onNetworkLost = { setNetworkStatus(NetworkStatus.Disconnected) },
        )
    }

    protected val flow = MutableStateFlow(initialState)
    val state = flow.asStateFlow()

    private var networkStatus: NetworkStatus? = null

    private var predictionsStale: ErrorBannerState.StalePredictions? = null
    private val dataErrors = mutableMapOf<String, ErrorBannerState.DataError>()

    protected open fun updateState() {
        flow.value =
            when {
                networkStatus == NetworkStatus.Disconnected -> ErrorBannerState.NetworkError(null)
                dataErrors.isNotEmpty() ->
                    // encapsulate all the different error actions within one error
                    ErrorBannerState.DataError(dataErrors.keys) {
                        dataErrors.values.forEach { it.action() }
                    }
                predictionsStale != null -> predictionsStale
                else -> null
            }
    }

    open fun checkPredictionsStale(
        predictionsLastUpdated: EasternTimeInstant,
        predictionQuantity: Int,
        action: () -> Unit,
    ) {
        predictionsStale =
            if (
                predictionQuantity > 0 &&
                    EasternTimeInstant.now() - predictionsLastUpdated > 2.minutes
            ) {
                ErrorBannerState.StalePredictions(predictionsLastUpdated, action)
            } else {
                null
            }
        updateState()
    }

    private fun setNetworkStatus(newStatus: NetworkStatus) {
        networkStatus = newStatus
        updateState()
    }

    fun setDataError(key: String, action: () -> Unit) {
        dataErrors[key] = ErrorBannerState.DataError(setOf(key), action)
        updateState()
    }

    fun clearDataError(key: String) {
        dataErrors.remove(key)
        updateState()
    }

    fun clearState() {
        predictionsStale = null
        dataErrors.clear()
        flow.value = null
    }
}

class ErrorBannerStateRepository : IErrorBannerStateRepository(), KoinComponent

class MockErrorBannerStateRepository
@DefaultArgumentInterop.Enabled
constructor(
    state: ErrorBannerState? = null,
    onSubscribeToNetworkChanges: (() -> Unit)? = null,
    onCheckPredictionsStale: (() -> Unit)? = null,
) : IErrorBannerStateRepository(state) {
    private val onSubscribeToNetworkChanges = onSubscribeToNetworkChanges
    private val onCheckPredictionsStale = onCheckPredictionsStale

    val mutableFlow
        get() = flow

    override fun subscribeToNetworkStatusChanges() {
        onSubscribeToNetworkChanges?.invoke()
    }

    override fun checkPredictionsStale(
        predictionsLastUpdated: EasternTimeInstant,
        predictionQuantity: Int,
        action: () -> Unit,
    ) {
        onCheckPredictionsStale?.invoke()
    }
}
