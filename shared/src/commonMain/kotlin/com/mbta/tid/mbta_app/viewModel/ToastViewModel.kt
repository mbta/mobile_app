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

public interface IToastViewModel {
    public val models: StateFlow<ToastViewModel.State>

    public fun hideToast()

    public fun showToast(toast: Toast)
}

public class ToastViewModel internal constructor() :
    IToastViewModel, MoleculeViewModel<Event, ToastViewModel.State>() {

    /**
     * These are parallel to [androidx.compose.material3.SnackbarDuration], because the Jetpack
     * Compose Material 3 snackbar does not allow you to set a specific time, and no toast/snackbar
     * implementation exists in SwiftUI, so we might as well make the behavior match.
     */
    public enum class Duration {
        Short,
        Long,
        Indefinite,
    }

    @Serializable
    public sealed class ToastAction {
        @Serializable public data class Close(val onClose: (() -> Unit)) : ToastAction()

        @Serializable
        public data class Custom(val actionLabel: String, val onAction: (() -> Unit)) :
            ToastAction()
    }

    @Serializable
    public data class Toast(
        val message: String,
        val duration: Duration = Duration.Indefinite,
        val isTip: Boolean = false,
        val action: ToastAction? = null,
    )

    public sealed interface Event {
        public data object Hide : Event

        public data class ShowToast internal constructor(internal val toast: Toast) : Event
    }

    public sealed class State {
        public data object Hidden : State()

        public data class Visible(val toast: Toast) : State()
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

    override val models: StateFlow<State>
        get() = internalModels

    override fun hideToast(): Unit = fireEvent(Event.Hide)

    override fun showToast(toast: Toast): Unit = fireEvent(Event.ShowToast(toast))
}

public class MockToastViewModel
@DefaultArgumentInterop.Enabled
constructor(initialState: ToastViewModel.State = ToastViewModel.State.Hidden) : IToastViewModel {
    override val models: MutableStateFlow<ToastViewModel.State> = MutableStateFlow(initialState)

    public var onHideToast: () -> Unit = {}
    public var onShowToast: (Toast) -> Unit = { _: Toast -> }

    override fun hideToast(): Unit = onHideToast()

    override fun showToast(toast: Toast): Unit = onShowToast(toast)
}
