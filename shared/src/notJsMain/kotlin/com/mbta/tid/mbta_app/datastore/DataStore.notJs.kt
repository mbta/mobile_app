package com.mbta.tid.mbta_app.datastore

import androidx.datastore.core.DataStore as RealDataStore
import androidx.datastore.preferences.core.edit as realEdit

internal actual typealias DataStore<T> = RealDataStore<T>

internal actual suspend fun DataStore<Preferences>.edit(
    transform: suspend (MutablePreferences) -> Unit
): Preferences = this.realEdit(transform)
