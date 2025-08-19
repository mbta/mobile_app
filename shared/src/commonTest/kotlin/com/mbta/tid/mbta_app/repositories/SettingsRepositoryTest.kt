package com.mbta.tid.mbta_app.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import com.mbta.tid.mbta_app.mocks.MockDatastoreStorage
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

internal class SettingsRepositoryTest : KoinTest {
    val defaultSettings = Settings.entries.associateWith { false }

    @AfterTest
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun `can get a setting`() = runBlocking {
        val storage = MockDatastoreStorage()
        storage.preferences =
            preferencesOf(
                Settings.DevDebugMode.dataStoreKey to true,
                Settings.SearchRouteResults.dataStoreKey to false,
            )
        val dataStore = DataStoreFactory.create(storage)
        startKoin { modules(module { single<DataStore<Preferences>> { dataStore } }) }
        val repo = SettingsRepository()

        assertEquals(
            mapOf(
                Settings.DevDebugMode to true,
                Settings.SearchRouteResults to false,
                Settings.StationAccessibility to false,
                Settings.HideMaps to false,
                Settings.EnhancedFavorites to false,
                Settings.TrackThisTrip to false,
            ),
            repo.getSettings(),
        )
    }

    @Test
    fun `settings default to false`() = runBlocking {
        val dataStore = DataStoreFactory.create(MockDatastoreStorage())
        startKoin { modules(module { single<DataStore<Preferences>> { dataStore } }) }
        val repo = SettingsRepository()

        assertEquals(Settings.entries.associateWith { false }, repo.getSettings())
    }

    @Test
    fun `can set a setting`() = runBlocking {
        val storage = MockDatastoreStorage()
        startKoin {
            modules(module { single<DataStore<Preferences>> { DataStoreFactory.create(storage) } })
        }
        val repo = SettingsRepository()
        for (setting in Settings.entries) {
            storage.preferences = emptyPreferences()
            repo.setSettings(mapOf(setting to true))
            assertEquals(preferencesOf(setting.dataStoreKey to true), storage.preferences)
        }
    }
}
