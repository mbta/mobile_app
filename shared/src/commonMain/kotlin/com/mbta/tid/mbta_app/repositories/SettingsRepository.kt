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

enum class Settings(val dataStoreKey: Preferences.Key<Boolean>, val override: Boolean? = null) {
    DevDebugMode(booleanPreferencesKey("dev_debug_mode")),
    EnhancedFavorites(booleanPreferencesKey("enhanced_favorites_feature_flag"), true),
    HideMaps(booleanPreferencesKey("hide_maps")),
    SearchRouteResults(booleanPreferencesKey("searchRouteResults_featureFlag")),
    StationAccessibility(booleanPreferencesKey("elevator_accessibility")),
}

class MockSettingsRepository
@DefaultArgumentInterop.Enabled
constructor(
    private var settings: Map<Settings, Boolean> = emptyMap(),
    private var onGetSettings: () -> Unit = {},
    private var onSaveSettings: (Map<Settings, Boolean>) -> Unit = {},
) : ISettingsRepository {
    override suspend fun getSettings(): Map<Settings, Boolean> {
        onGetSettings()
        return settings
    }

    override suspend fun setSettings(settings: Map<Settings, Boolean>) = onSaveSettings(settings)
}
