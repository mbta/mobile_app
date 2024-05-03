package com.mbta.tid.mbta_app.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ISettingsRepository {
    suspend fun getMapDebug(): Boolean

    suspend fun setMapDebug(mapDebug: Boolean)
}

class SettingsRepository : ISettingsRepository, KoinComponent {
    private val dataStore: DataStore<Preferences> by inject()

    private val mapDebugKey = booleanPreferencesKey("map_debug")

    override suspend fun getMapDebug(): Boolean {
        return dataStore.data.map { it[mapDebugKey] ?: false }.first()
    }

    override suspend fun setMapDebug(mapDebug: Boolean) {
        dataStore.edit { it[mapDebugKey] = mapDebug }
    }
}
