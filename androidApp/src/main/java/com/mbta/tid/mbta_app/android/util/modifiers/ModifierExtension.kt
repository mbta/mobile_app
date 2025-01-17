package com.mbta.tid.mbta_app.android.util.modifiers

import androidx.compose.ui.Modifier

fun Modifier.thenIf(
    condition: Boolean,
    other: Modifier.() -> Modifier,
) = if (condition) other() else this

fun <T> Modifier.thenIfNotNull(
    value: T?,
    other: Modifier.(T) -> Modifier,
) = if (value != null) other(value) else this
