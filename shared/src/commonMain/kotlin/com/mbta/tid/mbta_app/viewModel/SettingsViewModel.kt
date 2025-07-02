package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsRepository: ISettingsRepository) :
    MoleculeViewModel<SettingsViewModel.Event, Map<Settings, Boolean>>() {
    @Composable
    override fun runLogic(events: Flow<Event>): Map<Settings, Boolean> {
        var cache by remember { mutableStateOf<Map<Settings, Boolean>>(emptyMap()) }

        LaunchedEffect(Unit) {
            cache = settingsRepository.getSettings()

            events.collect { event ->
                when (event) {
                    is Event.Set -> {
                        val newSettings = mapOf(event.setting to event.value)
                        cache = cache + newSettings
                        CoroutineScope(Dispatchers.IO).launch {
                            settingsRepository.setSettings(newSettings)
                        }
                    }
                }
            }
        }

        return cache
    }

    sealed class Event {
        data class Set(val setting: Settings, val value: Boolean) : Event()
    }

    val models = internalModels

    fun set(setting: Settings, value: Boolean) = fireEvent(Event.Set(setting, value))
}
