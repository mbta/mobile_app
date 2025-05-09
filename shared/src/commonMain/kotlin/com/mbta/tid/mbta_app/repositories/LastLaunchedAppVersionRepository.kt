package com.mbta.tid.mbta_app.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.AppVersion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ILastLaunchedAppVersionRepository {
    suspend fun getLastLaunchedAppVersion(): AppVersion?

    suspend fun setLastLaunchedAppVersion(version: AppVersion)
}

class LastLaunchedAppVersionRepository : ILastLaunchedAppVersionRepository, KoinComponent {
    private val dataStore: DataStore<Preferences> by inject()

    private val lastLaunchedAppVersionKey = stringPreferencesKey("last_launched_app_version")

    override suspend fun getLastLaunchedAppVersion(): AppVersion? {
        return dataStore.data
            .map { it[lastLaunchedAppVersionKey] }
            .first()
            ?.let { AppVersion.parse(it) }
    }

    override suspend fun setLastLaunchedAppVersion(version: AppVersion) {
        dataStore.edit { it[lastLaunchedAppVersionKey] = version.toString() }
    }
}

class MockLastLaunchedAppVersionRepository
@DefaultArgumentInterop.Enabled
constructor(
    private var lastLaunchedAppVersion: AppVersion?,
    private var onSet: (AppVersion) -> Unit = {},
) : ILastLaunchedAppVersionRepository {
    override suspend fun getLastLaunchedAppVersion() = lastLaunchedAppVersion

    override suspend fun setLastLaunchedAppVersion(version: AppVersion) {
        onSet(version)
    }
}
