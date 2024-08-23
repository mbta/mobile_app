package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember

/** Returns a dummy MutableState that does not cause render when setting it */
@Composable
fun <T> rememberRef(): MutableState<T?> {
    return remember() {
        object : MutableState<T?> {
            override var value: T? = null

            override fun component1(): T? = value

            override fun component2(): (T?) -> Unit = { value = it }
        }
    }
}
