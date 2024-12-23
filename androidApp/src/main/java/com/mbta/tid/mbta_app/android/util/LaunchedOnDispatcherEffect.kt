package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A [LaunchedEffect] that runs on a different dispatcher. For CPU-hungry tasks, try
 * [kotlinx.coroutines.Dispatchers.Default].
 */
@Composable
fun LaunchedOnDispatcherEffect(
    dispatcher: CoroutineDispatcher,
    vararg keys: Any?,
    block: suspend CoroutineScope.() -> Unit
) {
    LaunchedEffect(*keys) { launch(context = dispatcher, block = block) }
}
