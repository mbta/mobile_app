package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent

/**
 * Stores the state of the [Settings] so that they can be read instantly from anywhere with a hot
 * cache and loaded transparently with a cold cache. Intended to be used as a Koin singleton.
 */
class SettingsCache(private val settingsRepository: ISettingsRepository) : KoinComponent {
    private val cache = MutableStateFlow<Map<Settings, Boolean>?>(null)

    /** Changes the value of a [Settings] both in the cache and in the [settingsRepository]. */
    fun set(setting: Settings, value: Boolean) {
        val newSettings = mapOf(setting to value)
        cache.update { it.orEmpty() + newSettings }
        CoroutineScope(Dispatchers.IO).launch { settingsRepository.setSettings(newSettings) }
    }

    /**
     * Retrieves the value of a [Settings], updating automatically when this cache is updated, and
     * loading in the background if the cache is empty.
     *
     * Will not automatically see changes made directly to the [settingsRepository]; make sure all
     * changes are made via [set].
     */
    @Composable
    fun get(setting: Settings): Boolean {
        val cachedData by cache.collectAsState()

        LaunchedEffect(cachedData == null) {
            if (cachedData == null) {
                cache.value = settingsRepository.getSettings()
            }
        }

        return cachedData?.get(setting) ?: false
    }

    companion object {
        /** Gets the value of a [setting] from the current Koin context’s [SettingsCache]. */
        @Composable fun get(setting: Settings) = koinInject<SettingsCache>().get(setting)
    }
}
