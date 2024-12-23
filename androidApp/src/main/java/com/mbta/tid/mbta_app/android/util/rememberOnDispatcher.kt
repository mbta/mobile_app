package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineDispatcher

/**
 * An alternative to [remember] that performs the actual computation on a different
 * [CoroutineDispatcher]. For CPU-hungry tasks, try [kotlinx.coroutines.Dispatchers.Default].
 *
 * Since the computation isn't performed synchronously, the state will be `null` until [computation]
 * has finished.
 */
@Composable
inline fun <T> rememberOnDispatcher(
    dispatcher: CoroutineDispatcher,
    vararg keys: Any?,
    crossinline computation: @DisallowComposableCalls () -> T?
): T? {
    var state by remember { mutableStateOf<T?>(null) }

    LaunchedOnDispatcherEffect(dispatcher, *keys) { state = computation() }

    return state
}
