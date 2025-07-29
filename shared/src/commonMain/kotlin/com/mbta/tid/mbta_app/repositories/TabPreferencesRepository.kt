package com.mbta.tid.mbta_app.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mbta.tid.mbta_app.model.SheetRoutes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

enum class DefaultTab(val entrypoint: SheetRoutes.Entrypoint) {
    Favorites(SheetRoutes.Favorites),
    Nearby(SheetRoutes.NearbyTransit),
}

interface ITabPreferencesRepository {

    suspend fun getDefaultTab(): DefaultTab

    suspend fun setDefaultTab(defaultTab: DefaultTab)
}

class TabPreferencesRepository : ITabPreferencesRepository, KoinComponent {
    private val dataStore: DataStore<Preferences> by inject()

    private val defaultTabKey = stringPreferencesKey("default_tab")

    override suspend fun getDefaultTab(): DefaultTab {
        return dataStore.data.map { it[defaultTabKey] }.first()?.let { DefaultTab.valueOf(it) }
            ?: DefaultTab.Nearby
    }

    override suspend fun setDefaultTab(defaultTab: DefaultTab) {
        dataStore.edit { it[defaultTabKey] = defaultTab.name }
    }
}

class MockTabPreferencesRepository : ITabPreferencesRepository {
    override suspend fun getDefaultTab(): DefaultTab {
        return DefaultTab.Nearby
    }

    override suspend fun setDefaultTab(defaultTab: DefaultTab) {}
}
