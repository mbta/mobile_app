package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public interface IErrorBannerViewModel {
    public val models: StateFlow<ErrorBannerViewModel.State>

    public fun clearState()

    public fun setIsLoadingWhenPredictionsStale(isLoading: Boolean)

    public fun checkPredictionsStale(
        predictionsLastUpdated: EasternTimeInstant,
        predictionQuantity: Int,
        action: () -> Unit,
    )

    public fun setSheetRoute(sheetRoute: SheetRoutes?)
}

public class ErrorBannerViewModel(
    private val errorRepository: IErrorBannerStateRepository,
    private val sentryRepository: ISentryRepository,
    private val clock: Clock,
) :
    MoleculeViewModel<ErrorBannerViewModel.Event, ErrorBannerViewModel.State>(),
    IErrorBannerViewModel {

    public data class State(
        val loadingWhenPredictionsStale: Boolean = false,
        val errorState: ErrorBannerState? = null,
    ) {
        public constructor() : this(false, null)
    }

    public sealed interface Event {
        public data class SetIsLoadingWhenPredictionsStale(val isLoading: Boolean) : Event

        public data class SetSheetRoute(val sheetRoute: SheetRoutes?) : Event

        public data object ClearState : Event
    }

    private data class ClearedDataError(
        val keys: Set<String>,
        val details: Set<String>,
        val clearedAt: EasternTimeInstant,
    )

    @Composable
    override fun runLogic(events: Flow<Event>): State {
        var awaitingPredictionsAfterBackground: Boolean by remember { mutableStateOf(false) }
        var sheetRoute: SheetRoutes? by remember { mutableStateOf(null) }

        var errorState: ErrorBannerState? by remember { mutableStateOf(null) }

        var clearedError: ClearedDataError? by remember { mutableStateOf(null) }

        LaunchedEffect(Unit) {
            errorRepository.subscribeToNetworkStatusChanges()
            errorRepository.state.collect { newErrorState ->
                val previousErrorState = errorState
                val previousClearedError = clearedError
                if (previousErrorState is ErrorBannerState.DataError && newErrorState == null) {
                    clearedError =
                        ClearedDataError(
                            previousErrorState.messages,
                            previousErrorState.details,
                            EasternTimeInstant.now(clock),
                        )
                } else if (
                    previousErrorState == null &&
                        newErrorState is ErrorBannerState.DataError &&
                        previousClearedError != null
                ) {
                    // data error was cleared and then came back instantly, that’s not good
                    val oldKeys = previousClearedError.keys
                    val newKeys = newErrorState.messages
                    val commonKeys = oldKeys intersect newKeys
                    if (
                        EasternTimeInstant.now(clock) - previousClearedError.clearedAt <
                            1.minutes && commonKeys.isNotEmpty()
                    ) {
                        sentryRepository.captureMessage(
                            "Recurring DataError ${commonKeys.sorted()}"
                        ) {
                            addBreadcrumb(
                                Breadcrumb(
                                    SentryLevel.ERROR,
                                    type = "error",
                                    message = "Recurring DataError",
                                    category = null,
                                    mutableMapOf(
                                        "previousClearedError.keys" to previousClearedError.keys,
                                        "previousClearedError.details" to
                                            previousClearedError.details,
                                        "previousClearedError.clearedAt" to
                                            previousClearedError.clearedAt,
                                        "newErrorState.messages" to newErrorState.messages,
                                        "newErrorState.details" to newErrorState.details,
                                    ),
                                )
                            )
                        }
                    }
                    clearedError = null
                }
                errorState = newErrorState
            }
        }

        LaunchedEffect(Unit) {
            events.collect { event ->
                when (event) {
                    is Event.ClearState -> errorRepository.clearState()
                    is Event.SetIsLoadingWhenPredictionsStale ->
                        awaitingPredictionsAfterBackground = event.isLoading

                    is Event.SetSheetRoute -> {
                        if (SheetRoutes.pageChanged(sheetRoute, event.sheetRoute)) {
                            errorRepository.clearState()
                        }
                        sheetRoute = event.sheetRoute
                    }
                }
            }
        }

        return State(awaitingPredictionsAfterBackground, errorState)
    }

    override val models: StateFlow<State>
        get() = internalModels

    override fun clearState() {
        fireEvent(Event.ClearState)
    }

    override fun setIsLoadingWhenPredictionsStale(isLoading: Boolean) {
        fireEvent(Event.SetIsLoadingWhenPredictionsStale(isLoading))
    }

    override fun checkPredictionsStale(
        predictionsLastUpdated: EasternTimeInstant,
        predictionQuantity: Int,
        action: () -> Unit,
    ) {
        errorRepository.checkPredictionsStale(predictionsLastUpdated, predictionQuantity, action)
    }

    override fun setSheetRoute(sheetRoute: SheetRoutes?) {
        fireEvent(Event.SetSheetRoute(sheetRoute))
    }
}

public class MockErrorBannerViewModel
@DefaultArgumentInterop.Enabled
constructor(initialState: ErrorBannerViewModel.State = ErrorBannerViewModel.State()) :
    IErrorBannerViewModel {
    override val models: MutableStateFlow<ErrorBannerViewModel.State> =
        MutableStateFlow(initialState)

    override fun clearState() {}

    override fun setIsLoadingWhenPredictionsStale(isLoading: Boolean) {}

    override fun checkPredictionsStale(
        predictionsLastUpdated: EasternTimeInstant,
        predictionQuantity: Int,
        action: () -> Unit,
    ) {}

    override fun setSheetRoute(sheetRoutes: SheetRoutes?) {}
}
