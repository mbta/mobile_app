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

public interface ISettingsRepository {

    public suspend fun getSettings(): Map<Settings, Boolean>

    public suspend fun setSettings(settings: Map<Settings, Boolean>)
}

internal class SettingsRepository : ISettingsRepository, KoinComponent {
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

public enum class Settings(
    internal val dataStoreKey: Preferences.Key<Boolean>,
    public val override: Boolean? = null,
) {
    DevDebugMode(booleanPreferencesKey("dev_debug_mode")),
    HideMaps(booleanPreferencesKey("hide_maps")),
    Notifications(booleanPreferencesKey("notifications")),
    SearchRouteResults(booleanPreferencesKey("searchRouteResults_featureFlag")),
    StationAccessibility(booleanPreferencesKey("elevator_accessibility")),
}

public class MockSettingsRepository
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

    override suspend fun setSettings(settings: Map<Settings, Boolean>): Unit =
        onSaveSettings(settings)
}
