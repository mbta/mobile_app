package com.mbta.tid.mbta_app.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

enum class DefaultTab {
    Favorites,
    Nearby,
}

interface ITabPreferencesRepository {

    suspend fun getDefaultTab(): DefaultTab

    suspend fun setDefaultTab(defaultTab: DefaultTab)

    suspend fun hasSeenFavorites(): Boolean

    suspend fun setHasSeenFavorites(hasSeenFavorites: Boolean)
}

class TabPreferencesRepository : ITabPreferencesRepository, KoinComponent {
    private val dataStore: DataStore<Preferences> by inject()

    private val defaultTabKey = stringPreferencesKey("default_tab")
    private val hasSeenFavoritesKey = stringPreferencesKey("has_seen_favoirtes")

    override suspend fun getDefaultTab(): DefaultTab {
        return dataStore.data.map { it[defaultTabKey] }.first()?.let { DefaultTab.valueOf(it) }
            ?: DefaultTab.Favorites
    }

    override suspend fun setDefaultTab(defaultTab: DefaultTab) {
        dataStore.edit { it[defaultTabKey] = defaultTab.name }
    }

    override suspend fun hasSeenFavorites(): Boolean {
        return dataStore.data.map { it[hasSeenFavoritesKey] }.first()?.let { it == "true" } ?: false
    }

    override suspend fun setHasSeenFavorites(hasSeenFavorites: Boolean) {
        dataStore.edit { it[hasSeenFavoritesKey] = hasSeenFavorites.toString() }
    }
}

class MockTabPreferencesRepository : ITabPreferencesRepository {
    override suspend fun getDefaultTab(): DefaultTab {
        return DefaultTab.Nearby
    }

    override suspend fun setDefaultTab(defaultTab: DefaultTab) {}

    override suspend fun hasSeenFavorites(): Boolean {
        return true
    }

    override suspend fun setHasSeenFavorites(hasSeenFavorites: Boolean) {}
}
