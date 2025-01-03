package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Wraps the common pattern of a [remember]ed [mutableStateOf] that is computed with a `suspend
 * fun`.
 */
@Composable
inline fun <T> rememberSuspend(
    vararg keys: Any?,
    crossinline computation: @DisallowComposableCalls suspend () -> T?
): T? {
    var state by remember { mutableStateOf<T?>(null) }

    LaunchedEffect(*keys) { state = computation() }

    return state
}
