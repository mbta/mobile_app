package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.network.INetworkConnectivityMonitor
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okio.ArrayIndexOutOfBoundsException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal sealed class NetworkStatus {
    data object Connected : NetworkStatus()

    data object Disconnected : NetworkStatus()
}

public abstract class IErrorBannerStateRepository
internal constructor(initialState: ErrorBannerState? = null) : KoinComponent {

    private val networkConnectivityMonitor: INetworkConnectivityMonitor by inject()

    /*
    Registers platform-specific observer of network status changes.
     */
    public open fun subscribeToNetworkStatusChanges() {
        this.networkConnectivityMonitor.registerListener(
            onNetworkAvailable = { setNetworkStatus(NetworkStatus.Connected) },
            onNetworkLost = { setNetworkStatus(NetworkStatus.Disconnected) },
        )
    }

    protected val flow: MutableStateFlow<ErrorBannerState?> = MutableStateFlow(initialState)
    public val state: StateFlow<ErrorBannerState?> = flow.asStateFlow()

    private var networkStatus: NetworkStatus? = null

    private var sheetRoute: SheetRoutes? = null
    private var predictionsStale: ErrorBannerState.StalePredictions? = null
    private val dataErrors = mutableMapOf<String, ErrorBannerState.DataError>()

    protected open fun updateState() {
        flow.value =
            when {
                networkStatus == NetworkStatus.Disconnected -> ErrorBannerState.NetworkError(null)
                dataErrors.isNotEmpty() ->
                    // encapsulate all the different error actions within one error
                    ErrorBannerState.DataError(
                        messages = dataErrors.keys,
                        details = dataErrors.values.flatMap { it.details }.toSet(),
                        action = { dataErrors.values.forEach { it.action() } },
                    )
                predictionsStale != null -> predictionsStale
                else -> null
            }
    }

    public open fun checkPredictionsStale(
        predictionsLastUpdated: EasternTimeInstant,
        predictionQuantity: Int,
        checkingSheetRoute: SheetRoutes?,
        action: () -> Unit,
    ) {
        if (checkingSheetRoute != sheetRoute || checkingSheetRoute == null) {
            return
        }
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

    public fun setSheetRoute(sheetRoute: SheetRoutes?) {
        this.sheetRoute = sheetRoute
    }

    private fun setNetworkStatus(newStatus: NetworkStatus) {
        networkStatus = newStatus
        updateState()
    }

    public fun setDataError(key: String, details: String, action: () -> Unit) {
        dataErrors[key] = ErrorBannerState.DataError(setOf(key), setOf(details), action)
        updateState()
    }

    public fun clearDataError(key: String) {
        dataErrors.remove(key)
        updateState()
    }

    public fun clearState() {
        predictionsStale = null
        try {
            dataErrors.clear()
        } catch (e: ArrayIndexOutOfBoundsException) {
            // ignore race condition if clearing multiple times
        }
        flow.value = null
    }
}

public class ErrorBannerStateRepository : IErrorBannerStateRepository(), KoinComponent

public class MockErrorBannerStateRepository
@DefaultArgumentInterop.Enabled
constructor(
    state: ErrorBannerState? = null,
    onSubscribeToNetworkChanges: (() -> Unit)? = null,
    onCheckPredictionsStale: (() -> Unit)? = null,
) : IErrorBannerStateRepository(state) {
    private val onSubscribeToNetworkChanges = onSubscribeToNetworkChanges
    private val onCheckPredictionsStale = onCheckPredictionsStale

    public val mutableFlow: MutableStateFlow<ErrorBannerState?>
        get() = flow

    override fun subscribeToNetworkStatusChanges() {
        onSubscribeToNetworkChanges?.invoke()
    }

    override fun checkPredictionsStale(
        predictionsLastUpdated: EasternTimeInstant,
        predictionQuantity: Int,
        checkingSheetRoute: SheetRoutes?,
        action: () -> Unit,
    ) {
        onCheckPredictionsStale?.invoke()
    }
}
