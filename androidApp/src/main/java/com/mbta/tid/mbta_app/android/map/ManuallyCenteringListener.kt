package com.mbta.tid.mbta_app.android.map

import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.ShoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.OnScaleListener
import com.mapbox.maps.plugin.gestures.OnShoveListener
import com.mbta.tid.mbta_app.android.location.IViewportProvider

class ManuallyCenteringListener(private val viewportProvider: IViewportProvider) :
    OnMoveListener, OnScaleListener, OnShoveListener {
    override fun onMove(detector: MoveGestureDetector) = false

    override fun onMoveBegin(detector: MoveGestureDetector) {
        viewportProvider.setIsManuallyCentering(true)
    }

    override fun onMoveEnd(detector: MoveGestureDetector) {
        viewportProvider.setIsManuallyCentering(false)
    }

    override fun onScale(detector: StandardScaleGestureDetector) {}

    override fun onScaleBegin(detector: StandardScaleGestureDetector) {
        viewportProvider.setIsManuallyCentering(true)
    }

    override fun onScaleEnd(detector: StandardScaleGestureDetector) {
        viewportProvider.setIsManuallyCentering(false)
    }

    override fun onShove(detector: ShoveGestureDetector) {}

    override fun onShoveBegin(detector: ShoveGestureDetector) {
        viewportProvider.setIsManuallyCentering(true)
    }

    override fun onShoveEnd(detector: ShoveGestureDetector) {
        viewportProvider.setIsManuallyCentering(false)
    }
}
