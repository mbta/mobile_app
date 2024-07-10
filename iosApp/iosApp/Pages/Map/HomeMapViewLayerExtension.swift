//
//  HomeMapViewLayerExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 5/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI
@_spi(Experimental) import MapboxMaps

/*
 Functions for manipulating the layers displayed on the map.
 */
extension HomeMapView {
    func handleTryLayerInit(map: MapboxMap?) {
        guard let map,
              globalData != nil,
              railRouteShapeFetcher.response != nil,
              globalMapData?.mapStops != nil,
              mapVM.layerManager == nil
        else {
            return
        }
        handleLayerInit(map)
    }

    func handleLayerInit(_ map: MapboxMap) {
        let layerManager = MapLayerManager(map: map)
        initializeLayers(layerManager)
        mapVM.layerManager = layerManager
    }

    func initializeLayers(_ layerManager: IMapLayerManager) {
        let routeSourceData = railRouteShapeFetcher.response?.routesWithSegmentedShapes ?? []
        mapVM.allRailSourceData = routeSourceData
        mapVM.routeSourceData = routeSourceData

        let snappedStopRouteLines = RouteSourceGenerator.generateRouteLines(routeData: routeSourceData,
                                                                            routesById: globalData?.routes,
                                                                            stopsById: globalData?.stops,
                                                                            alertsByStop: globalMapData?.alertsByStop)
        mapVM.snappedStopRouteLines = snappedStopRouteLines

        let stopSourceGenerator = StopSourceGenerator(
            stops: globalMapData?.mapStops ?? [:],
            selectedStop: lastNavEntry?.stop(),
            routeLines: snappedStopRouteLines
        )
        let childStopSourceGenerator = ChildStopSourceGenerator(childStops: nil)

        layerManager.addSources(
            stopSourceGenerator: stopSourceGenerator,
            childStopSourceGenerator: childStopSourceGenerator
        )

        addLayers(layerManager)
    }

    func addLayers(_ layerManager: IMapLayerManager) {
        layerManager.addLayers(
            routeLayerGenerator: RouteLayerGenerator(),
            stopLayerGenerator: StopLayerGenerator(),
            childStopLayerGenerator: ChildStopLayerGenerator()
        )
    }

    func resetDefaultSources() {
        let updatedStopSources = StopSourceGenerator(
            stops: globalMapData?.mapStops ?? [:],
            selectedStop: nil,
            routeLines: mapVM.snappedStopRouteLines
        )
        mapVM.routeSourceData = mapVM.allRailSourceData
        let updatedChildStopSources = ChildStopSourceGenerator(childStops: nil)
        mapVM.layerManager?.updateSourceData(
            stopSourceGenerator: updatedStopSources,
            childStopSourceGenerator: updatedChildStopSources
        )
    }

    func updateStopDetailsLayers(
        _ stopMapData: StopMapResponse,
        _ filter: StopDetailsFilter?,
        _ departures: StopDetailsDepartures?
    ) {
        if let filter {
            mapVM.routeSourceData = MapViewModel.filteredRouteShapesForStop(
                stopMapData: stopMapData,
                filter: filter,
                departures: departures
            )
        } else {
            mapVM.routeSourceData = RouteSourceGenerator.forRailAtStop(stopMapData.routeShapes,
                                                                       mapVM.allRailSourceData,
                                                                       globalData?.routes)
        }

        let childStopSource = ChildStopSourceGenerator(childStops: stopMapData.childStops)
        mapVM.layerManager?.updateSourceData(childStopSourceGenerator: childStopSource)
    }

    func updateGlobalMapDataSources() {
        let updatedStopSources = StopSourceGenerator(
            stops: globalMapData?.mapStops ?? [:],
            selectedStop: lastNavEntry?.stop(),
            routeLines: mapVM.snappedStopRouteLines
        )
        mapVM.layerManager?.updateSourceData(stopSourceGenerator: updatedStopSources)
        // If routes are already being displayed, keep using those. Otherwise, use the rail shapes
        let routeData = mapVM.routeSourceData
        updateRouteSources(routeData: routeData)
    }

    func updateRouteSources(routeData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes]) {
        mapVM.updateRouteSource(routeLines: RouteSourceGenerator.generateRouteLines(
            routeData: routeData,
            routesById: globalData?.routes,
            stopsById: globalData?.stops,
            alertsByStop: globalMapData?.alertsByStop
        ))
    }
}
