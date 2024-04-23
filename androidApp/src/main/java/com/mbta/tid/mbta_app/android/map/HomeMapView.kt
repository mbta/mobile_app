package com.mbta.tid.mbta_app.android.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapEvents
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.generated.LocationComponentSettings
import com.mbta.tid.mbta_app.Backend
import com.mbta.tid.mbta_app.android.fetcher.GlobalData
import com.mbta.tid.mbta_app.android.fetcher.getRailRouteShapes
import com.mbta.tid.mbta_app.android.util.followPuck
import com.mbta.tid.mbta_app.android.util.isFollowingPuck
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.android.util.toPoint
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds

@OptIn(MapboxExperimental::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeMapView(
    modifier: Modifier = Modifier,
    mapViewportState: MapViewportState,
    backend: Backend,
    globalData: GlobalData,
    alertsData: AlertsStreamDataResponse?,
    lastNearbyTransitLocation: Position?
) {
    var layerManager: MapLayerManager? by remember {
        mutableStateOf(null, referentialEqualityPolicy())
    }
    val locationPermissionState =
        rememberPermissionState(permission = android.Manifest.permission.ACCESS_FINE_LOCATION)

    val railRouteShapes = getRailRouteShapes(backend)

    val now = timer(updateInterval = 10.seconds)

    val alertsByStop =
        remember(globalData, alertsData, alertsData, now) {
            globalData.globalStaticData?.withRealtimeAlertsByStop(alertsData, now)
        }

    Box(modifier) {
        MapboxMap(
            Modifier.fillMaxSize(),
            mapEvents =
                MapEvents(
                    onCameraChanged = { layerManager?.updateStopLayerZoom(it.cameraState.zoom) }
                ),
            gesturesSettings =
                GesturesSettings {
                    rotateEnabled = false
                    pitchEnabled = false
                },
            locationComponentSettings =
                LocationComponentSettings(locationPuck = createDefault2DPuck(withBearing = false)) {
                    puckBearingEnabled = false
                    enabled = true
                    pulsingEnabled = false
                },
            compass = {},
            scaleBar = {},
            mapViewportState = mapViewportState,
            style = { MapStyle(style = Style.LIGHT) }
        ) {
            MapEffect(key1 = null) { mapViewportState.followPuck() }
            if (!mapViewportState.isFollowingPuck && lastNearbyTransitLocation != null) {
                CircleAnnotation(
                    point = lastNearbyTransitLocation.toPoint(),
                    circleColorString = "#ba75c7",
                    circleRadius = 10.0
                )
            }

            val context = LocalContext.current

            MapEffect(railRouteShapes, globalData) { map ->
                if (railRouteShapes == null || globalData.response == null) return@MapEffect

                if (layerManager == null) layerManager = MapLayerManager(map.mapboxMap, context)

                val routeSourceGenerator =
                    RouteSourceGenerator(railRouteShapes, globalData.stops, alertsByStop)

                layerManager!!.addSources(
                    routeSourceGenerator,
                    StopSourceGenerator(
                        globalData.stops,
                        routeSourceGenerator.routeSourceDetails,
                        alertsByStop
                    )
                )

                layerManager!!.addLayers(
                    RouteLayerGenerator(railRouteShapes, globalData.routes),
                    StopLayerGenerator(MapLayerManager.stopLayerTypes)
                )
            }

            MapEffect(alertsByStop) {
                if (railRouteShapes == null || alertsByStop == null) return@MapEffect
                layerManager?.let { manager ->
                    manager.updateSourceData(
                        RouteSourceGenerator(railRouteShapes, globalData.stops, alertsByStop)
                    )
                    manager.updateSourceData(
                        StopSourceGenerator(
                            globalData.stops,
                            manager.routeSourceGenerator?.routeSourceDetails,
                            alertsByStop
                        )
                    )
                }
            }
        }

        if (!mapViewportState.isFollowingPuck || !locationPermissionState.status.isGranted) {
            RecenterButton(
                onClick = {
                    locationPermissionState.launchPermissionRequest()
                    mapViewportState.followPuck()
                },
                Modifier.align(Alignment.TopEnd).padding(16.dp)
            )
        }
    }
}
