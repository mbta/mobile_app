package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.ErrorBannerState
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent

abstract class IErrorBannerStateRepository
protected constructor(initialState: ErrorBannerState? = null) {
    protected val flow = MutableStateFlow(initialState)
    val state = flow.asStateFlow()

    private var predictionsStale: ErrorBannerState.StalePredictions? = null
    private val dataErrors = mutableMapOf<String, ErrorBannerState.DataError>()

    protected open fun updateState() {
        flow.value =
            when {
                dataErrors.isNotEmpty() ->
                    // encapsulate all the different error actions within one error
                    ErrorBannerState.DataError { dataErrors.values.forEach { it.action() } }
                predictionsStale != null -> predictionsStale
                else -> null
            }
    }

    fun checkPredictionsStale(
        predictionsLastUpdated: Instant,
        predictionQuantity: Int,
        action: () -> Unit
    ) {
        predictionsStale =
            if (predictionQuantity > 0 && Clock.System.now() - predictionsLastUpdated > 2.minutes) {
                ErrorBannerState.StalePredictions(predictionsLastUpdated, action)
            } else {
                null
            }
        updateState()
    }

    fun setDataError(key: String, action: () -> Unit) {
        dataErrors[key] = ErrorBannerState.DataError(action)
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

class MockErrorBannerStateRepository(state: ErrorBannerState? = null) :
    IErrorBannerStateRepository(state) {
    val mutableFlow
        get() = flow
}
