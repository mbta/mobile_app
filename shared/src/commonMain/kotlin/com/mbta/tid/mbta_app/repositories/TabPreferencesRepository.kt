package com.mbta.tid.mbta_app.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.routes.SheetRoutes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public enum class DefaultTab(public val entrypoint: SheetRoutes.Entrypoint) {
    Favorites(SheetRoutes.Favorites),
    Nearby(SheetRoutes.NearbyTransit),
}

public interface ITabPreferencesRepository {

    public suspend fun getDefaultTab(): DefaultTab

    public suspend fun setDefaultTab(defaultTab: DefaultTab)
}

internal class TabPreferencesRepository : ITabPreferencesRepository, KoinComponent {
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

public class MockTabPreferencesRepository
@DefaultArgumentInterop.Enabled
constructor(public var defaultTab: DefaultTab = DefaultTab.Nearby) : ITabPreferencesRepository {
    override suspend fun getDefaultTab(): DefaultTab {
        return defaultTab
    }

    override suspend fun setDefaultTab(defaultTab: DefaultTab) {
        this.defaultTab = defaultTab
    }
}
