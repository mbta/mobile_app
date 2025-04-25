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

class SettingsCache(private val settingsRepository: ISettingsRepository) : KoinComponent {
    private val cache = MutableStateFlow<Map<Settings, Boolean>?>(null)

    fun set(setting: Settings, value: Boolean) {
        val newSettings = mapOf(setting to value)
        cache.update { it.orEmpty() + newSettings }
        CoroutineScope(Dispatchers.IO).launch { settingsRepository.setSettings(newSettings) }
    }

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
        @Composable fun get(setting: Settings) = koinInject<SettingsCache>().get(setting)
    }
}
