package com.mbta.tid.mbta_app.android.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.mbta.tid.mbta_app.android.util.LazyObjectQueue
import com.mbta.tid.mbta_app.android.util.followPuck
import com.mbta.tid.mbta_app.android.util.getRailRouteShapes
import com.mbta.tid.mbta_app.android.util.isFollowingPuck
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.android.util.toPoint
import com.mbta.tid.mbta_app.map.ColorPalette
import com.mbta.tid.mbta_app.map.RouteFeaturesBuilder
import com.mbta.tid.mbta_app.map.StopFeaturesBuilder
import com.mbta.tid.mbta_app.map.StopSourceData
import com.mbta.tid.mbta_app.model.GlobalMapData
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds

@OptIn(MapboxExperimental::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeMapView(
    modifier: Modifier = Modifier,
    mapViewportState: MapViewportState,
    globalResponse: GlobalResponse?,
    alertsData: AlertsStreamDataResponse?,
    lastNearbyTransitLocation: Position?,
) {
    val layerManager = remember { LazyObjectQueue<MapLayerManager>() }
    val locationPermissionState =
        rememberPermissionState(permission = android.Manifest.permission.ACCESS_FINE_LOCATION)

    val railRouteShapes = getRailRouteShapes()

    val now = timer(updateInterval = 10.seconds)
    val globalMapData =
        remember(globalResponse, alertsData, now) {
            if (globalResponse != null) {
                GlobalMapData(
                    globalResponse,
                    GlobalMapData.getAlertsByStop(globalResponse, alertsData, now)
                )
            } else {
                null
            }
        }

    val isDarkMode = isSystemInDarkTheme()

    Box(modifier) {
        MapboxMap(
            Modifier.fillMaxSize(),
            mapEvents =
                MapEvents(
                    onStyleLoaded = {
                        layerManager.run {
                            addLayers(if (isDarkMode) ColorPalette.dark else ColorPalette.light)
                        }
                    }
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
            style = { MapStyle(style = if (isDarkMode) Style.DARK else Style.LIGHT) }
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

            MapEffect { map ->
                if (layerManager.`object` == null)
                    layerManager.`object` = MapLayerManager(map.mapboxMap, context)
            }

            MapEffect(railRouteShapes, globalResponse, globalMapData) {
                if (railRouteShapes == null || globalResponse == null) return@MapEffect

                val routeSourceData = railRouteShapes.routesWithSegmentedShapes
                val snappedStopRouteLines =
                    RouteFeaturesBuilder.generateRouteLines(
                        routeSourceData,
                        globalResponse.routes,
                        globalResponse.stops,
                        globalMapData?.alertsByStop
                    )

                layerManager.run {
                    updateStopSourceData(
                        StopFeaturesBuilder.buildCollection(
                                StopSourceData(),
                                globalMapData?.mapStops.orEmpty(),
                                snappedStopRouteLines
                            )
                            .toMapbox()
                    )
                }

                layerManager.run {
                    updateRouteSourceData(
                        RouteFeaturesBuilder.buildCollection(
                                RouteFeaturesBuilder.generateRouteLines(
                                    routeSourceData,
                                    globalResponse.routes,
                                    globalResponse.stops,
                                    globalMapData?.alertsByStop
                                )
                            )
                            .toMapbox()
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
