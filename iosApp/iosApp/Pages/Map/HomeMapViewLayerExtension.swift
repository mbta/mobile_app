//
//  HomeMapViewLayerExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 5/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import MapboxMaps
import shared
import SwiftUI

/*
 Functions for manipulating the layers displayed on the map.
 */
extension HomeMapView {
    func handleTryLayerInit(map: MapboxMap?) {
        guard let map,
              globalData != nil,
              railRouteShapes != nil,
              globalMapData?.mapStops != nil,
              mapVM.layerManager == nil
        else {
            return
        }
        handleLayerInit(map)
    }

    func handleAccessTokenLoaded(_ map: MapboxMap?) {
        map?.mapStyle = .init(uri: appVariant.styleUri(colorScheme: colorScheme))
    }

    func handleLayerInit(_ map: MapboxMap) {
        let layerManager = MapLayerManager(map: map)
        initializeLayers(layerManager)
        mapVM.layerManager = layerManager
    }

    func initializeLayers(_ layerManager: IMapLayerManager) {
        let routeSourceData = railRouteShapes?.routesWithSegmentedShapes ?? []
        mapVM.allRailSourceData = routeSourceData
        mapVM.routeSourceData = routeSourceData

        let snappedStopRouteLines = RouteFeaturesBuilder.shared.generateRouteLines(
            routeData: routeSourceData,
            routesById: globalData?.routes,
            stopsById: globalData?.stops,
            alertsByStop: globalMapData?.alertsByStop
        )
        mapVM.snappedStopRouteLines = snappedStopRouteLines

        mapVM.stopSourceData = .init(selectedStopId: lastNavEntry?.stop()?.id)

        mapVM.childStops = nil

        addLayers(layerManager)
    }

    func addLayers(_ layerManager: IMapLayerManager, recreate: Bool = false) {
        layerManager.addLayers(colorScheme: colorScheme, recreate: recreate)
    }

    func refreshMap() {
        if let layerManager = mapVM.layerManager {
            if layerManager.currentScheme != colorScheme {
                layerManager.addIcons(recreate: true)
                addLayers(layerManager, recreate: true)
            } else {
                addLayers(layerManager)
            }
            updateGlobalMapDataSources()
        }
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
            mapVM.routeSourceData = RouteFeaturesBuilder.shared.filteredRouteShapesForStop(
                stopMapData: stopMapData,
                filter: filter,
                departures: departures
            )
        } else {
            mapVM.routeSourceData = RouteFeaturesBuilder.shared.forRailAtStop(
                stopShapes: stopMapData.routeShapes,
                railShapes: mapVM.allRailSourceData,
                routesById: globalData?.routes
            )
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
        mapVM.updateStopSource(StopFeaturesBuilder.shared.buildCollection(
            stopData: stopData,
            stops: globalMapData?.mapStops ?? [:],
            linesToSnap: mapVM.snappedStopRouteLines
        )
        .toMapbox())
    }

    func updateChildStopSource(childStops: [String: Stop]?) {
        mapVM
            .updateChildStopSource(ChildStopFeaturesBuilder.shared.generateChildStopFeatures(childStops: childStops)
                .toMapbox())
    }
}
