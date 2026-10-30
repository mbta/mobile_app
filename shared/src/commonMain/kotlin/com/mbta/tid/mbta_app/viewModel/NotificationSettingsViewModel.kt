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

    public fun loadSavedSettings(settings: Notifications?)

    public fun savedSettings()

    public fun setEnabled(enabled: Boolean)

    public fun setPresetsEnabledFlag(enabled: Boolean)

    public fun setWindows(windows: List<Window>)

    public fun addPlaceholderWindow()

    public fun setPreset(preset: Preset?)

    public fun setNow(now: EasternTimeInstant)
}

/**
 * ViewModel for editing notification settings per route/stop/direction. This VM is not responsible
 * for persisting those settings, only for managing the state of the settings while the user is
 * editing them. These settings are persisted by [FavoritesViewModel]
 *
 * Intended to be used as a singleton across the app, so that the state of the recently saved window
 * persists through a user session.
 */
public class NotificationSettingsViewModel(private val sentryRepository: ISentryRepository) :
    MoleculeViewModel<NotificationSettingsViewModel.Event, NotificationSettingsViewModel.State>(),
    INotificationSettingsViewModel {
    public sealed class Event {
        public data class LoadSavedSettings(val settings: Notifications?) : Event()

        public data object SavedSettings : Event()

        public data class SetEnabled(val enabled: Boolean) : Event()

        public data class SetPresetsEnabledFlag(val enabled: Boolean) : Event()

        public data class SetWindows(val windows: List<Notifications.Window>) : Event()

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

        var state: State by remember {
            mutableStateOf(State(null, null))
        }

        var lastSavedSettings: Notifications? by remember {
            mutableStateOf(null)
        }
        var customPreset: List<Window>? by remember {
            mutableStateOf(null)
        }

        var presetsEnabledFlag: Boolean by remember {
            mutableStateOf(false)
        }

        EventSink(eventHandlingTimeout = 1.seconds, sentryRepository = sentryRepository) { event ->
            val enabled = state.settings?.enabled ?: false

            when (event) {
                is Event.SetEnabled -> {
                    val windows =
                        if (!enabled && event.enabled) {
                            (state.settings?.windows ?: emptyList())
                                .ifEmpty {
                                    lastSavedSettings?.windows ?: emptyList()
                                }
                                .ifEmpty {
                                    listOf(Window.default(emptyList(), presetsEnabledFlag, now))
                                }
                        } else {
                            state.settings?.windows ?: emptyList()
                        }

                    state =
                        State(
                            Notifications(enabled = event.enabled, windows = windows),
                            Preset.selected(windows),
                        )
                }
                is Event.SetWindows -> {
                    state =
                        State(
                            Notifications(enabled = enabled, windows = event.windows),
                            Preset.selected(event.windows),
                        )
                    customPreset =
                        if (event.windows.size == 1 && Preset.selected(event.windows) != null) {
                            // customized only the days of the week of the windows, so still use the
                            // default custom window time
                            listOf(
                                Window.customFromCurrentTime(now)
                                    .copy(daysOfWeek = event.windows.first().daysOfWeek)
                            )
                        } else {
                            event.windows
                        }
                }
                is Event.SetNow -> {
                    now = event.now
                }
                is Event.SetPreset -> {
                    val daysOfWeek =
                        state.settings?.windows?.firstOrNull()?.daysOfWeek
                            ?: Window.defaultDaysOfWeek(now)
                    val settings =
                        if (event.preset == null) {
                            val windows = customPreset ?: listOf(Window.customFromCurrentTime(now))
                            Notifications(enabled = enabled, windows = windows)
                        } else {
                            val existingWindows = state.settings?.windows ?: emptyList()
                            val newWindows =
                                if (existingWindows.size > 1) {
                                    listOf(
                                        Window(
                                            event.preset.startTime,
                                            event.preset.endTime,
                                            daysOfWeek,
                                        )
                                    ) + existingWindows.drop(1)
                                } else {
                                    listOf(
                                        Window(
                                            event.preset.startTime,
                                            event.preset.endTime,
                                            daysOfWeek,
                                        )
                                    )
                                }
                            Notifications(
                                enabled = enabled,
                                windows = newWindows,
                            )
                        }
                    state = State(settings, event.preset)
                }

                is Event.SetPresetsEnabledFlag -> presetsEnabledFlag = event.enabled
                is Event.AddPlaceholderWindow -> {
                    val existingWindows = state.settings?.windows ?: emptyList()
                    val newWindows =
                        existingWindows + Window.default(existingWindows, presetsEnabledFlag, now)
                    state =
                        State(
                            Notifications(
                                enabled = enabled,
                                windows = newWindows,
                            ),
                            null,
                        )
                    customPreset = newWindows
                }

                is Event.LoadSavedSettings -> {
                    val loadedWindows = event.settings?.windows ?: emptyList()
                    customPreset =
                        if (Preset.selected(loadedWindows) == null && loadedWindows.isNotEmpty()) {
                            loadedWindows
                        } else {
                            null
                        }
                    state =
                        State(
                            event.settings,
                            Preset.selected(event.settings?.windows ?: emptyList()),
                        )
                }

                Event.SavedSettings -> {
                    if (state.settings?.enabled == true) {
                        lastSavedSettings = state.settings
                    }
                    state = State(null, null)
                }
            }
        }
        return state
    }

    override fun loadSavedSettings(settings: Notifications?) {
        fireEvent(Event.LoadSavedSettings(settings))
    }

    override fun savedSettings() {
        fireEvent(Event.SavedSettings)
    }

    override fun setEnabled(enabled: Boolean) {
        fireEvent(Event.SetEnabled(enabled))
    }

    override fun setPresetsEnabledFlag(enabled: Boolean) {
        fireEvent(Event.SetPresetsEnabledFlag(enabled))
    }

    override fun setWindows(windows: List<FavoriteSettings.Notifications.Window>) {
        fireEvent(Event.SetWindows(windows))
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

    public var onLoadSavedSettings: (Notifications?) -> Unit = {}
    public var onSavedSettings: () -> Unit = {}
    public var onSetEnabled: (Boolean) -> Unit = {}
    public var onSetWindows: (List<FavoriteSettings.Notifications.Window>) -> Unit = {}
    public var onSetPreset: (Preset?) -> Unit = {}
    public var onSetNow: (EasternTimeInstant) -> Unit = {}

    override fun loadSavedSettings(settings: Notifications?) {
        onLoadSavedSettings(settings)
    }

    override fun savedSettings() {
        onSavedSettings()
    }

    override fun setEnabled(enabled: Boolean) {
        onSetEnabled(enabled)
    }

    override fun setPresetsEnabledFlag(enabled: Boolean) {}

    override fun setWindows(windows: List<FavoriteSettings.Notifications.Window>) {
        onSetWindows(windows)
    }

    override fun addPlaceholderWindow() {}

    override fun setPreset(preset: Preset?) {
        onSetPreset(preset)
    }

    override fun setNow(now: EasternTimeInstant) {
        onSetNow(now)
    }
}
