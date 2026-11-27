package com.mbta.tid.mbta_app.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.DepartureGroupingMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public interface IDepartureGroupingPreferencesRepository {
    public suspend fun getMode(): DepartureGroupingMode

    public suspend fun setMode(mode: DepartureGroupingMode)
}

internal class DepartureGroupingPreferencesRepository :
    IDepartureGroupingPreferencesRepository, KoinComponent {
    private val dataStore: DataStore<Preferences> by inject()

    private val modeKey = stringPreferencesKey("departure_grouping_mode")

    override suspend fun getMode(): DepartureGroupingMode =
        dataStore.data
            .map { it[modeKey] }
            .first()
            ?.let { saved -> DepartureGroupingMode.entries.find { it.name == saved } }
            ?: DepartureGroupingMode.ByRoute

    override suspend fun setMode(mode: DepartureGroupingMode) {
        dataStore.edit { it[modeKey] = mode.name }
    }
}

public class MockDepartureGroupingPreferencesRepository
@DefaultArgumentInterop.Enabled
constructor(public var mode: DepartureGroupingMode = DepartureGroupingMode.ByRoute) :
    IDepartureGroupingPreferencesRepository {
    override suspend fun getMode(): DepartureGroupingMode = mode

    override suspend fun setMode(mode: DepartureGroupingMode) {
        this.mode = mode
    }
}
