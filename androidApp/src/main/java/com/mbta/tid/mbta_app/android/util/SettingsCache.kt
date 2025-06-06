package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.viewModel.SettingsViewModel
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent

/**
 * Stores the state of the [Settings] so that they can be read instantly from anywhere with a hot
 * cache and loaded transparently with a cold cache. Intended to be used as a Koin singleton.
 */
class SettingsCache(private val viewModel: SettingsViewModel) : KoinComponent {

    /** Changes the value of a [Settings]. */
    fun set(setting: Settings, value: Boolean) {
        viewModel.set(setting, value)
    }

    /**
     * Retrieves the value of a [Settings], updating automatically when this cache is updated, and
     * loading in the background if the cache is empty.
     *
     * Will not automatically see changes made directly to the settings repository; make sure all
     * changes are made via [set].
     */
    @Composable
    fun get(setting: Settings): Boolean {
        val state by viewModel.models.collectAsState()

        return state[setting] ?: false
    }

    companion object {
        /** Gets the value of a [setting] from the current Koin context’s [SettingsCache]. */
        @Composable fun get(setting: Settings) = koinInject<SettingsCache>().get(setting)
    }
}
