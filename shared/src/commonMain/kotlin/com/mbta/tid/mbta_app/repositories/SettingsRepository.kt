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
    suspend fun getSettings(): Map<Settings, Boolean>

    suspend fun setSettings(settings: Map<Settings, Boolean>)
}

class SettingsRepository : ISettingsRepository, KoinComponent {
    private val dataStore: DataStore<Preferences> by inject()

    override suspend fun getSettings(): Map<Settings, Boolean> {
        return dataStore.data
            .map { dataStore ->
                Settings.entries.associateWith { dataStore[it.dataStoreKey] ?: false }
            }
            .first()
    }

    override suspend fun setSettings(settings: Map<Settings, Boolean>) {
        dataStore.edit { dataStore ->
            settings.forEach { dataStore[it.key.dataStoreKey] = it.value }
        }
    }
}

enum class Settings(val dataStoreKey: Preferences.Key<Boolean>) {
    Map(booleanPreferencesKey("map_debug")),
    SearchRouteResults(booleanPreferencesKey("searchRouteResults_featureFlag")),
    HideMaps(booleanPreferencesKey("hide_maps")),
    LocationDeferred(booleanPreferencesKey("location_deferred")),
}

class MockSettingsRepository
@DefaultArgumentInterop.Enabled
constructor(
    private var settings: Map<Settings, Boolean> = emptyMap(),
    private var onSaveSettings: (Map<Settings, Boolean>) -> Unit = {}
) : ISettingsRepository {
    override suspend fun getSettings(): Map<Settings, Boolean> = settings

    override suspend fun setSettings(settings: Map<Settings, Boolean>) = onSaveSettings(settings)
}
