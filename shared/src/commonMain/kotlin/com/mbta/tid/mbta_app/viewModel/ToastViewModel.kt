package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

class ToastViewModel : MoleculeViewModel<ToastViewModel.Event, ToastViewModel.State>() {

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
    data class Toast(
        val message: String,
        val duration: Duration = Duration.Indefinite,
        val onClose: (() -> Unit)? = null,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null,
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

    val models
        get() = internalModels

    fun hideToast() = fireEvent(Event.Hide)

    fun showToast(toast: Toast) = fireEvent(Event.ShowToast(toast))
}
