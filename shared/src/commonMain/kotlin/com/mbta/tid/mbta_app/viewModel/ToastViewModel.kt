package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.viewModel.ToastViewModel.Event
import com.mbta.tid.mbta_app.viewModel.ToastViewModel.Toast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

interface IToastViewModel {
    val models: StateFlow<ToastViewModel.State>

    fun hideToast()

    fun showToast(toast: Toast)
}

class ToastViewModel : IToastViewModel, MoleculeViewModel<Event, ToastViewModel.State>() {

    /**
     * These are parallel to [androidx.compose.material3.SnackbarDuration], because the Jetpack
     * Compose Material 3 snackbar does not allow you to set a specific time, and no toast/snackbar
     * implementation exists in SwiftUI, so we might as well make the behavior match.
     */
    enum class Duration {
        Short,
        Long,
        Indefinite,
    }

    @Serializable
    sealed class ToastAction {
        @Serializable data class Close(val onClose: (() -> Unit)) : ToastAction()

        @Serializable
        data class Custom(val actionLabel: String, val onAction: (() -> Unit)) : ToastAction()
    }

    @Serializable
    data class Toast(
        val message: String,
        val duration: Duration = Duration.Indefinite,
        val action: ToastAction? = null,
    )

    sealed interface Event {
        data object Hide : Event

        data class ShowToast(val toast: Toast) : Event
    }

    sealed class State {
        data object Hidden : State()

        data class Visible(val toast: Toast) : State()
    }

    @Composable
    override fun runLogic(events: Flow<Event>): State {
        return produceState(State.Hidden as State, events) {
                events.collect { event ->
                    value =
                        when (event) {
                            is Event.Hide -> State.Hidden
                            is Event.ShowToast -> State.Visible(event.toast)
                        }
                }
            }
            .value
    }

    override val models
        get() = internalModels

    override fun hideToast() = fireEvent(Event.Hide)

    override fun showToast(toast: Toast) = fireEvent(Event.ShowToast(toast))
}

class MockToastViewModel
@DefaultArgumentInterop.Enabled
constructor(initialState: ToastViewModel.State = ToastViewModel.State.Hidden) : IToastViewModel {
    override val models = MutableStateFlow(initialState)

    var onHideToast = {}
    var onShowToast = { _: Toast -> }

    override fun hideToast() = onHideToast()

    override fun showToast(toast: Toast) = onShowToast(toast)
}
