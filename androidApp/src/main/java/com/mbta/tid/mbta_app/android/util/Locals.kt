package com.mbta.tid.mbta_app.android.util

import android.app.Activity
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.android.gms.location.FusedLocationProviderClient

val LocalActivity =
    staticCompositionLocalOf<Activity> { throw IllegalStateException("no activity") }

val LocalLocationClient =
    staticCompositionLocalOf<FusedLocationProviderClient> {
        throw IllegalStateException("no location client")
    }

/**
 * Whether the sheet contents are loading. Used by low-level child views to determine whether they
 * should be styled as placeholders by `Modifier.placeholderIfLoading()`
 */
val IsLoadingSheetContents = compositionLocalOf { false }
