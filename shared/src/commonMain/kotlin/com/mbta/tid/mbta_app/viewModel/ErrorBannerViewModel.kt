package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect

public interface IErrorBannerViewModel {
    public val models: StateFlow<ErrorBannerViewModel.State>

    public fun clearState()

    public fun setIsLoadingWhenPredictionsStale(isLoading: Boolean)

    public fun checkPredictionsStale(
        predictionsLastUpdated: EasternTimeInstant,
        predictionQuantity: Int,
        action: () -> Unit,
    )
}

public class ErrorBannerViewModel(private val errorRepository: IErrorBannerStateRepository) :
    MoleculeViewModel<ErrorBannerViewModel.Event, ErrorBannerViewModel.State>(),
    IErrorBannerViewModel {

    public data class State(
        val loadingWhenPredictionsStale: Boolean = false,
        val errorState: ErrorBannerState? = null,
    )

    public sealed interface Event {
        public data class SetIsLoadingWhenPredictionsStale(val isLoading: Boolean) : Event

        public data object ClearState : Event
    }

    @Composable
    override fun runLogic(events: Flow<Event>): State {
        var awaitingPredictionsAfterBackground: Boolean by remember { mutableStateOf(false) }
        var errorState: ErrorBannerState? by remember { mutableStateOf(null) }

        LaunchedEffect(Unit) {
            errorRepository.subscribeToNetworkStatusChanges()
            errorRepository.state.collect { errorState = it }
        }

        LaunchedEffect(Unit) {
            events.collect { event ->
                when (event) {
                    Event.ClearState -> errorRepository.clearState()
                    is Event.SetIsLoadingWhenPredictionsStale ->
                        awaitingPredictionsAfterBackground = event.isLoading
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
}

public class MockErrorBannerViewModel(
    initialState: ErrorBannerViewModel.State = ErrorBannerViewModel.State()
) : IErrorBannerViewModel {
    override val models: MutableStateFlow<ErrorBannerViewModel.State> =
        MutableStateFlow(initialState)

    override fun clearState() {}

    override fun setIsLoadingWhenPredictionsStale(isLoading: Boolean) {}

    override fun checkPredictionsStale(
        predictionsLastUpdated: EasternTimeInstant,
        predictionQuantity: Int,
        action: () -> Unit,
    ) {}
}
