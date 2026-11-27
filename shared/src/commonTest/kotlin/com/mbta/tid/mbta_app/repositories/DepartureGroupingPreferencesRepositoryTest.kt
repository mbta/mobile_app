package com.mbta.tid.mbta_app.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mbta.tid.mbta_app.mocks.MockDatastoreStorage
import com.mbta.tid.mbta_app.model.DepartureGroupingMode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class DepartureGroupingPreferencesRepositoryTest {

    @AfterTest
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun testDefaultModeIsByRoute() = runBlocking {
        startKoinWithPreferences(preferencesOf())
        val repo = DepartureGroupingPreferencesRepository()
        assertEquals(DepartureGroupingMode.ByRoute, repo.getMode())
    }

    @Test
    fun testReadsMode() = runBlocking {
        startKoinWithPreferences(
            preferencesOf(stringPreferencesKey("departure_grouping_mode") to "ByTime"),
        )
        val repo = DepartureGroupingPreferencesRepository()
        assertEquals(DepartureGroupingMode.ByTime, repo.getMode())
    }

    @Test
    fun testUnknownModeFallsBackToByRoute() = runBlocking {
        startKoinWithPreferences(
            preferencesOf(stringPreferencesKey("departure_grouping_mode") to "SomethingElse"),
        )
        val repo = DepartureGroupingPreferencesRepository()
        assertEquals(DepartureGroupingMode.ByRoute, repo.getMode())
    }

    @Test
    fun testSetMode() = runBlocking {
        startKoinWithPreferences(preferencesOf())
        val repo = DepartureGroupingPreferencesRepository()
        repo.setMode(DepartureGroupingMode.ByTime)
        assertEquals(DepartureGroupingMode.ByTime, repo.getMode())
    }

    private fun startKoinWithPreferences(preferences: Preferences) {
        val storage = MockDatastoreStorage()
        storage.preferences = preferences
        val dataStore = DataStoreFactory.create(storage)
        startKoin { modules(module { single<DataStore<Preferences>> { dataStore } }) }
    }
}
