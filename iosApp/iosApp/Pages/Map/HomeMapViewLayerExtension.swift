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

        let snappedStopRouteLines = RouteFeaturesBuilder.shared.generateRouteLines(routeData: routeSourceData,
                                                                                   routesById: globalData?.routes,
                                                                                   stopsById: globalData?.stops,
                                                                                   alertsByStop: globalMapData?
                                                                                       .alertsByStop)
        mapVM.snappedStopRouteLines = snappedStopRouteLines

        mapVM.stopSourceData = .init(selectedStopId: lastNavEntry?.stop()?.id)

        mapVM.childStops = nil

        addLayers(layerManager)
    }

    func addLayers(_ layerManager: IMapLayerManager) {
        layerManager.addLayers(
            stopLayerGenerator: StopLayerGenerator(),
            childStopLayerGenerator: ChildStopLayerGenerator()
        )
    }

    func resetDefaultSources() {
        mapVM.stopSourceData = .init(selectedStopId: nil)
        mapVM.routeSourceData = mapVM.allRailSourceData
        mapVM.childStops = nil
    }

    func updateStopDetailsLayers(
        _ stopMapData: StopMapResponse,
        _ filter: StopDetailsFilter?,
        _ departures: StopDetailsDepartures?
    ) {
        mapVM.childStops = stopMapData.childStops
        if let filter {
            mapVM.routeSourceData = MapViewModel.filteredRouteShapesForStop(
                stopMapData: stopMapData,
                filter: filter,
                departures: departures
            )
        } else {
            mapVM.routeSourceData = RouteFeaturesBuilder.shared.forRailAtStop(stopShapes: stopMapData.routeShapes,
                                                                              railShapes: mapVM.allRailSourceData,
                                                                              routesById: globalData?.routes)
        }
    }

    func updateGlobalMapDataSources() {
        updateStopSource(stopData: mapVM.stopSourceData)
        updateRouteSources(routeData: mapVM.routeSourceData)
    }

    func updateRouteSources(routeData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes]) {
        mapVM.updateRouteSource(routeLines: RouteFeaturesBuilder.shared.generateRouteLines(
            routeData: routeData,
            routesById: globalData?.routes,
            stopsById: globalData?.stops,
            alertsByStop: globalMapData?.alertsByStop
        ))
    }

    func updateStopSource(stopData: StopSourceData) {
        mapVM.updateStopSource(StopSourceGenerator.generateStopSource(stopData: stopData,
                                                                      stops: globalMapData?.mapStops ?? [:],
                                                                      linesToSnap: mapVM.snappedStopRouteLines))
    }

    func updateChildStopSource(childStops: [String: Stop]?) {
        mapVM.updateChildStopSource(ChildStopSourceGenerator.generateChildStopSource(childStops: childStops))
    }
}
