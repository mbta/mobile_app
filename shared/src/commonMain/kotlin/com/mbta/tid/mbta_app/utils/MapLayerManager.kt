package com.mbta.tid.mbta_app.utils

import com.mbta.tid.mbta_app.map.ColorPalette
import com.mbta.tid.mbta_app.map.RouteSourceData
import com.mbta.tid.mbta_app.map.StopLayerGenerator
import com.mbta.tid.mbta_app.map.style.FeatureCollection
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse

public interface IMapLayerManager {
    public suspend fun addLayers(
        mapFriendlyRouteResponse: MapFriendlyRouteResponse,
        state: StopLayerGenerator.State,
        globalResponse: GlobalResponse,
        colorPalette: ColorPalette,
    )

    public suspend fun addLayers(
        routes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        state: StopLayerGenerator.State,
        globalResponse: GlobalResponse,
        colorPalette: ColorPalette,
    )

    public fun resetPuckPosition()

    public suspend fun updateRouteSourceData(routeData: List<RouteSourceData>)

    public suspend fun updateStopSourceData(stopData: FeatureCollection)
}
