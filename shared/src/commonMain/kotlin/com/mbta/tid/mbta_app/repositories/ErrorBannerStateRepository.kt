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

    protected open fun updateState() {
        if (predictionsStale != null) {
            flow.value = predictionsStale
        } else {
            flow.value = null
        }
    }

    fun checkPredictionsStale(
        predictionsLastUpdated: Instant,
        predictionQuantity: Int,
        action: () -> Unit
    ) {
        predictionsStale =
            if (predictionQuantity > 0 && Clock.System.now() - predictionsLastUpdated > 1.minutes) {
                ErrorBannerState.StalePredictions(predictionsLastUpdated, action)
            } else {
                null
            }
        updateState()
    }

    fun clearState() {
        predictionsStale = null
        flow.value = null
    }
}

class ErrorBannerStateRepository : IErrorBannerStateRepository(), KoinComponent

class MockErrorBannerStateRepository(state: ErrorBannerState? = null) :
    IErrorBannerStateRepository(state) {
    val mutableFlow
        get() = flow
}
