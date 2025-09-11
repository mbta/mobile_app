package com.mbta.tid.mbta_app.datastore

import kotlinx.coroutines.flow.Flow

internal expect interface DataStore<T> {
    val data: Flow<T>
}

internal expect suspend fun DataStore<Preferences>.edit(
    transform: suspend (MutablePreferences) -> Unit
): Preferences
