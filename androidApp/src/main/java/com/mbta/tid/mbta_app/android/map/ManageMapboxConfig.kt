package com.mbta.tid.mbta_app.android.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject

@Composable
fun ManageMapboxConfig(mapboxConfigManager: IMapboxConfigManager = koinInject()) {
    val lastErrorTimestamp by
        mapboxConfigManager.lastMapboxErrorTimestamp.collectAsStateWithLifecycle(null)

    LaunchedEffect(lastErrorTimestamp) { mapboxConfigManager.loadConfig() }
}
