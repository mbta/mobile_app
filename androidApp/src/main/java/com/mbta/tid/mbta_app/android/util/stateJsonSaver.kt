package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import com.mbta.tid.mbta_app.json
import kotlinx.serialization.encodeToString

/**
 * Persists a [MutableState] of some JSON-serializable data object. Note that the
 * [androidx.compose.runtime.SnapshotMutationPolicy] will be lost when restoring, so if you need a
 * policy other than [androidx.compose.runtime.structuralEqualityPolicy] you should not use this.
 */
inline fun <reified T> stateJsonSaver() =
    Saver<MutableState<T>, String>(
        { json.encodeToString(it.value) },
        { mutableStateOf(json.decodeFromString(it)) },
    )
