package com.mbta.tid.mbta_app.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IPinnedRoutesRepository {
    suspend fun getPinnedRoutes(): Set<String>

    suspend fun setPinnedRoutes(routes: Set<String>)
}

class PinnedRoutesRepository : IPinnedRoutesRepository, KoinComponent {

    private val dataStore: DataStore<Preferences> by inject()

    private val pinnedRoutesKey = stringSetPreferencesKey("pinned_routes")

    override suspend fun getPinnedRoutes(): Set<String> {
        return dataStore.data.map { it[pinnedRoutesKey] ?: emptySet() }.first()
    }

    override suspend fun setPinnedRoutes(routes: Set<String>) {
        dataStore.edit { it[pinnedRoutesKey] = routes }
    }
}

class MockPinnedRoutesRepository
@DefaultArgumentInterop.Enabled
constructor(
    initialPinnedRoutes: Set<String> = emptySet(),
    private val onGet: (() -> Unit)? = null,
    private val onSet: ((Set<String>) -> Unit)? = null,
) : IPinnedRoutesRepository, KoinComponent {
    var pinnedRoutes: Set<String> = initialPinnedRoutes

    override suspend fun getPinnedRoutes(): Set<String> {
        onGet?.invoke()
        return pinnedRoutes
    }

    override suspend fun setPinnedRoutes(routes: Set<String>) {
        onSet?.invoke(routes)
        pinnedRoutes = routes
    }
}
