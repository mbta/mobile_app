package com.mbta.tid.mbta_app.android.fetcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.Backend

@Composable
internal fun <T : Any> getBackendData(
    backend: Backend,
    effectKey: Any?,
    backendCall: suspend Backend.() -> T?
): T? {
    var data by remember { mutableStateOf<T?>(null) }

    LaunchedEffect(effectKey) { data = backend.backendCall() }

    return data
}
