package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.FavoriteSettings.Notifications
import com.mbta.tid.mbta_app.model.FavoriteSettings.Notifications.*
import com.mbta.tid.mbta_app.model.Preset
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public interface INotificationSettingsViewModel {
    public val models: StateFlow<NotificationSettingsViewModel.State>

    public fun loadSavedSettings(settings: Notifications)

    public fun setEnabled(enabled: Boolean)

    public fun setPresetsEnabledFlag(enabled: Boolean)

    public fun setCustomWindows(windows: List<Window>)

    public fun addPlaceholderWindow()

    public fun setPreset(preset: Preset?)

    public fun setNow(now: EasternTimeInstant)
}

public class NotificationSettingsViewModel(private val sentryRepository: ISentryRepository) :
    MoleculeViewModel<NotificationSettingsViewModel.Event, NotificationSettingsViewModel.State>(),
    INotificationSettingsViewModel {
    public sealed class Event {

        public data class LoadSavedSettings(val settings: Notifications) : Event()

        public data class SetEnabled(val enabled: Boolean) : Event()

        public data class SetPresetsEnabledFlag(val enabled: Boolean) : Event()

        public data class SetCustomWindows(val windows: List<Notifications.Window>) : Event()

        public data object AddPlaceholderWindow : Event()

        public data class SetPreset(val preset: Preset?) : Event()

        public data class SetNow(val now: EasternTimeInstant) : Event()
    }

    public data class State(
        val settings: Notifications?,
        val selectedPreset: Preset?,
    ) {
        public constructor() : this(Notifications.disabled, null)
    }

    override val models: StateFlow<State>
        get() = internalModels

    @Composable
    override fun runLogic(): State {
        var now: EasternTimeInstant by remember { mutableStateOf(EasternTimeInstant.now()) }

        var settings: Notifications? by remember {
            mutableStateOf(null)
        }
        var customPreset: List<Window> by remember {
            mutableStateOf(listOf(Window.customFromCurrentTime(now)))
        }

        var presetsEnabledFlag: Boolean by remember {
            mutableStateOf(false)
        }

        EventSink(eventHandlingTimeout = 1.seconds, sentryRepository = sentryRepository) { event ->
            val enabled = settings?.enabled ?: false

            when (event) {
                is Event.SetEnabled -> {
                    val windows =
                        if (!enabled && event.enabled) {
                            (settings?.windows ?: emptyList()).ifEmpty {
                                listOf(Window.default(emptyList(), presetsEnabledFlag, now))
                            }
                        } else {
                            settings?.windows ?: emptyList()
                        }

                    settings = Notifications(enabled = event.enabled, windows = windows)
                }

                is Event.SetCustomWindows -> {
                    settings = Notifications(enabled = enabled, windows = event.windows)
                    customPreset = event.windows
                }
                is Event.SetNow -> now = event.now
                is Event.SetPreset -> {
                    settings =
                        if (event.preset == null) {
                            Notifications(enabled = enabled, windows = customPreset)
                        } else {
                            Notifications(
                                enabled = enabled,
                                windows =
                                    listOf(
                                        Window(
                                            event.preset.startTime,
                                            event.preset.endTime,
                                            Window.defaultDaysOfWeek(now),
                                        )
                                    ),
                            )
                        }
                }

                is Event.SetPresetsEnabledFlag -> presetsEnabledFlag = event.enabled
                is Event.AddPlaceholderWindow -> {
                    val existingWindows = settings?.windows ?: emptyList()
                    settings =
                        Notifications(
                            enabled = enabled,
                            windows =
                                existingWindows +
                                    Window.default(existingWindows, presetsEnabledFlag, now),
                        )
                }

                is Event.LoadSavedSettings -> {
                    settings = event.settings
                    if (Preset.selected(event.settings.windows) == null) {
                        customPreset = event.settings.windows
                    }
                }
            }
        }

        val state =
            remember(settings) {
                State(settings, Preset.selected(settings?.windows ?: emptyList()))
            }
        return state
    }

    override fun loadSavedSettings(settings: Notifications) {
        fireEvent(Event.LoadSavedSettings(settings))
    }

    override fun setEnabled(enabled: Boolean) {
        fireEvent(Event.SetEnabled(enabled))
    }

    override fun setPresetsEnabledFlag(enabled: Boolean) {
        fireEvent(Event.SetPresetsEnabledFlag(enabled))
    }

    override fun setCustomWindows(windows: List<FavoriteSettings.Notifications.Window>) {
        fireEvent(Event.SetCustomWindows(windows))
    }

    override fun addPlaceholderWindow() {
        fireEvent(Event.AddPlaceholderWindow)
    }

    override fun setPreset(preset: Preset?) {
        fireEvent(Event.SetPreset(preset))
    }

    override fun setNow(now: EasternTimeInstant) {
        fireEvent(Event.SetNow(now))
    }
}

public class MockNotificationSettingsViewModel(
    public val initialState: NotificationSettingsViewModel.State =
        NotificationSettingsViewModel.State()
) : INotificationSettingsViewModel {
    override val models: MutableStateFlow<NotificationSettingsViewModel.State>
        get() = MutableStateFlow(initialState)

    override fun loadSavedSettings(settings: Notifications) {}

    public var onSetEnabled: (Boolean) -> Unit = {}
    public var onSetCustomWindows: (List<FavoriteSettings.Notifications.Window>) -> Unit = {}
    public var onSetPreset: (Preset?) -> Unit = {}
    public var onSetNow: (EasternTimeInstant) -> Unit = {}

    override fun setEnabled(enabled: Boolean) {
        onSetEnabled(enabled)
    }

    override fun setPresetsEnabledFlag(enabled: Boolean) {}

    override fun setCustomWindows(windows: List<FavoriteSettings.Notifications.Window>) {
        onSetCustomWindows(windows)
    }

    override fun addPlaceholderWindow() {}

    override fun setPreset(preset: Preset?) {
        onSetPreset(preset)
    }

    override fun setNow(now: EasternTimeInstant) {
        onSetNow(now)
    }
}
