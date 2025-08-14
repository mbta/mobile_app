package com.mbta.tid.mbta_app.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.Favorites
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public interface IFavoritesRepository {
    public suspend fun getFavorites(): Favorites

    public suspend fun setFavorites(favorites: Favorites)
}

internal class FavoritesRepository : IFavoritesRepository, KoinComponent {

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

public class MockFavoritesRepository
@DefaultArgumentInterop.Enabled
constructor(
    private var favorites: Favorites = Favorites(),
    private val onGet: (() -> Unit)? = null,
    private val onSet: ((Favorites) -> Unit)? = null,
) : IFavoritesRepository {
    override suspend fun getFavorites(): Favorites {
        onGet?.invoke()
        return favorites
    }

    override suspend fun setFavorites(favorites: Favorites) {
        onSet?.invoke(favorites)
        this.favorites = favorites
    }
}
