package com.mbta.tid.mbta_app.android.util

import android.app.Activity
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.android.gms.location.FusedLocationProviderClient

val LocalActivity =
    staticCompositionLocalOf<Activity> { throw IllegalStateException("no activity") }

val LocalLocationClient =
    staticCompositionLocalOf<FusedLocationProviderClient> {
        throw IllegalStateException("no location client")
    }
