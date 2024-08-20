package com.mbta.tid.mbta_app.android.util

import android.view.animation.AccelerateDecelerateInterpolator
import com.mapbox.maps.plugin.animation.MapAnimationOptions

object MapAnimationDefaults {
    val duration: Long = 1000
    val options =
        MapAnimationOptions.Builder()
            .duration(duration)
            .interpolator(AccelerateDecelerateInterpolator())
            .build()
}
