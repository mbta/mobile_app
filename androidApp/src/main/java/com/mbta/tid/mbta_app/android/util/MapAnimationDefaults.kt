package com.mbta.tid.mbta_app.android.util

import android.view.animation.AccelerateDecelerateInterpolator
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object MapAnimationDefaults {
    val duration: Duration = 1.seconds
    val options =
        MapAnimationOptions.Builder()
            .duration(duration.inWholeMilliseconds)
            .interpolator(AccelerateDecelerateInterpolator())
            .build()
}
