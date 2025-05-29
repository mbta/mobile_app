package com.mbta.tid.mbta_app.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.Favorites
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IFavoritesRepository {
    suspend fun getFavorites(): Favorites

    suspend fun setFavorites(favorites: Favorites)
}

class FavoritesRepository : IFavoritesRepository, KoinComponent {

    private val dataStore: DataStore<Preferences> by inject()

    private val favoritesKey = stringPreferencesKey("favorites")

    override suspend fun getFavorites(): Favorites {
        val encoded = dataStore.data.map { it[favoritesKey] }.first()
        if (encoded.isNullOrBlank()) {
            return Favorites()
        }
        return try {
            json.decodeFromString(encoded)
        } catch (e: Exception) {
            Favorites()
        }
    }

    override suspend fun setFavorites(favorites: Favorites) {
        dataStore.edit { it[favoritesKey] = json.encodeToString(favorites) }
    }
}

class MockFavoritesRepository(var favorites: Favorites = Favorites()) : IFavoritesRepository {
    override suspend fun getFavorites(): Favorites {
        return favorites
    }

    override suspend fun setFavorites(favorites: Favorites) {
        this.favorites = favorites
    }
}
