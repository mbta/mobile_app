package com.mbta.tid.mbta_app.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ISettingsRepository {
    suspend fun getSettings(): Set<Setting>

    suspend fun setSettings(settings: Set<Setting>)
}

class SettingsRepository : ISettingsRepository, KoinComponent {
    private val dataStore: DataStore<Preferences> by inject()

    override suspend fun getSettings(): Set<Setting> {
        return dataStore.data
            .map { dataStore ->
                Settings.entries.map { Setting(it, dataStore[it.dataStoreKey] ?: false) }
            }
            .first()
            .toSet()
    }

    override suspend fun setSettings(settings: Set<Setting>) {
        dataStore.edit { dataStore ->
            settings.forEach { dataStore[it.key.dataStoreKey] = it.isOn }
        }
    }
}

enum class Settings(val dataStoreKey: Preferences.Key<Boolean>) {
    Map(booleanPreferencesKey("map_debug")),
    SearchRouteResults(booleanPreferencesKey("searchRouteResults_featureFlag")),
    HideMaps(booleanPreferencesKey("hide_maps")),
}

data class Setting(val key: Settings, var isOn: Boolean)

class MockSettingsRepository
@DefaultArgumentInterop.Enabled
constructor(
    private var settings: Set<Setting> = setOf(),
    private var onSaveSettings: (Set<Setting>) -> Unit = {}
) : ISettingsRepository {
    override suspend fun getSettings() = settings

    override suspend fun setSettings(settings: Set<Setting>) = onSaveSettings(settings)
}
