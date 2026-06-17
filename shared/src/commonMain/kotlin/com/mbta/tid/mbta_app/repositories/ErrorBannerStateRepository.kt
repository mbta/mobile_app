package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.network.INetworkConnectivityMonitor
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.ArrayIndexOutOfBoundsException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal sealed class NetworkStatus {
    data object Connected : NetworkStatus()

    data object Disconnected : NetworkStatus()
}

/**
 * Represent an error, and the page(s) on which it occurred. If the current [SheetRoutes] matches
 * the pages for the error, then it can be shown in the banner and retried. If the error for is a
 * page that is not the current [SheetRoutes], then it should be discarded.
 *
 * An empty value for [ErrorKey.sheets] means that the error should be shown in the banner and
 * retried across all pages.
 */
public data class ErrorKey(public val sheets: Set<KClass<out SheetRoutes>>, public val id: String) {
    public fun withSuffix(suffix: String): ErrorKey {
        return this.copy(id = "$id.$suffix")
    }
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
    private val dataErrors = mutableMapOf<ErrorKey, ErrorBannerState.DataError>()
    private val mutex = Mutex()

    protected open fun updateState() {
        flow.value =
            when {
                networkStatus == NetworkStatus.Disconnected -> ErrorBannerState.NetworkError(null)
                dataErrors.isNotEmpty() ->
                    // encapsulate all the different error actions within one error
                    ErrorBannerState.DataError(
                        messages = dataErrors.keys.map { it.id }.toSet(),
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

    /** Set the current [SheetRoutes] and clear all errors associated with other [SheetRoutes] */
    public suspend fun setSheetRoute(sheetRoute: SheetRoutes?) {
        mutex.withLock {
            val oldSheetRoute = this.sheetRoute
            this.sheetRoute = sheetRoute

            if (oldSheetRoute == null) {
                // sheet route set for the first time, don't clear any errors that happened
                // before the sheet route was set yet
                return
            } else {

                dataErrors.keys.removeAll() { (sheets, _) ->
                    val targetKeyType = sheetRoute?.let { it::class }
                    !sheets.contains(targetKeyType)
                }
            }
        }
        updateState()
    }

    private fun setNetworkStatus(newStatus: NetworkStatus) {
        networkStatus = newStatus
        updateState()
    }

    public suspend fun setDataError(key: ErrorKey, details: String, action: () -> Unit) {
        mutex.withLock {
            val sheetRouteClass = sheetRoute?.let { it::class }
            if (
                sheetRouteClass != null &&
                    key.sheets.isNotEmpty() &&
                    !key.sheets.contains(sheetRouteClass)
            ) {
                // the error is for a page different from the one being presented; throw it out
                return@withLock
            }
            dataErrors[key] = ErrorBannerState.DataError(setOf(key.id), setOf(details), action)
        }
        updateState()
    }

    public suspend fun clearDataError(key: ErrorKey) {
        mutex.withLock { dataErrors.remove(key) }
        updateState()
    }

    public suspend fun clearState() {
        predictionsStale = null
        try {
            mutex.withLock { dataErrors.clear() }
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
