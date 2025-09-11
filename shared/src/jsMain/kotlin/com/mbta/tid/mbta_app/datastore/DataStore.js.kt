package com.mbta.tid.mbta_app.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal actual interface DataStore<T> {
    val _data: MutableStateFlow<T>

    actual val data: Flow<T>
}

internal actual suspend fun DataStore<Preferences>.edit(
    transform: suspend (MutablePreferences) -> Unit
): Preferences {
    val data = _data.value.toMutablePreferences()
    transform(data)
    _data.value = data
    return data
}
