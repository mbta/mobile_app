package com.mbta.tid.mbta_app.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mbta.tid.mbta_app.mocks.MockDatastoreStorage
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class TabPreferencesRepositoryTest {

    @AfterTest
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun testDefaultTabIsFavorites() = runBlocking {
        startKoinWithPreferences(preferencesOf())
        val repo = TabPreferencesRepository()
        assertEquals(DefaultTab.Favorites, repo.getDefaultTab())
    }

    @Test
    fun testReadsDefaultTab() = runBlocking {
        startKoinWithPreferences(
            preferencesOf(stringPreferencesKey("default_tab") to DefaultTab.Nearby.name)
        )
        val repo = TabPreferencesRepository()
        assertEquals(DefaultTab.Nearby, repo.getDefaultTab())
    }

    @Test
    fun testSetDefaultTab() = runBlocking {
        startKoinWithPreferences(
            preferencesOf(stringPreferencesKey("default_tab") to DefaultTab.Nearby.name)
        )
        val repo = TabPreferencesRepository()
        repo.setDefaultTab(DefaultTab.Favorites)
        assertEquals(DefaultTab.Favorites, repo.getDefaultTab())
    }

    @Test
    fun testHasSeenFavorites() = runBlocking {
        startKoinWithPreferences(preferencesOf(booleanPreferencesKey("has_seen_favorites") to true))
        val repo = TabPreferencesRepository()
        assertEquals(true, repo.hasSeenFavorites())
    }

    fun startKoinWithPreferences(preferences: Preferences) {
        val storage = MockDatastoreStorage()
        storage.preferences = preferences
        val dataStore = DataStoreFactory.create(storage)
        startKoin { modules(module { single<DataStore<Preferences>> { dataStore } }) }
    }
}
