//
//  HomeMapViewLayerExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 5/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@_spi(Experimental) import MapboxMaps
import Shared
import SwiftUI

/*
 Functions for manipulating the layers displayed on the map.
 */
extension HomeMapView {
    func handleTryLayerInit(map: MapboxMap?) {
        guard let map,
              mapVM.globalData != nil,
              railRouteShapes != nil,
              globalMapData != nil,
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

    func handleSetRailSources(railRouteShapes: MapFriendlyRouteResponse?) {
        guard let railRouteShapes else { return }
        let routeSourceData = railRouteShapes.routesWithSegmentedShapes
        mapVM.allRailSourceData = routeSourceData
        mapVM.routeSourceData = routeSourceData
    }

    func handleSetStopSources() {
        guard let globalData = mapVM.globalData else { return }
        Task {
            let snappedStopRouteSources = try await RouteFeaturesBuilder.shared.generateRouteSources(
                routeData: mapVM.allRailSourceData,
                globalData: globalData,
                globalMapData: globalMapData
            )
            mapVM.snappedStopRouteSources = snappedStopRouteSources
            mapVM.stopSourceData = .init(selectedStopId: lastNavEntry?.stopId())
        }
    }

    func initializeLayers(_ layerManager: IMapLayerManager) {
        handleSetRailSources(railRouteShapes: railRouteShapes)
        handleSetStopSources()

        addLayers(layerManager)
    }

    func addLayers(_ layerManager: IMapLayerManager, recreate: Bool = false) {
        guard let globalData = mapVM.globalData else { return }
        layerManager.addLayers(
            routes: mapVM.routeSourceData,
            globalResponse: globalData,
            colorScheme: colorScheme,
            recreate: recreate
        )
    }

    func refreshMap() {
        if let layerManager = mapVM.layerManager {
            updateGlobalMapDataSources()
            if layerManager.currentScheme != colorScheme {
                layerManager.addIcons(recreate: true)
                addLayers(layerManager, recreate: true)
            } else {
                addLayers(layerManager)
            }
        }
    }

    func resetDefaultSources() {
        mapVM.stopSourceData = .init(selectedStopId: nil)
        mapVM.routeSourceData = mapVM.allRailSourceData
    }

    func updateStopDetailsLayers(
        _ stopMapData: StopMapResponse,
        _ filter: StopDetailsFilter?,
        _ routeCardData: [RouteCardData]?
    ) {
        Task {
            if let filter {
                mapVM.routeSourceData = RouteFeaturesBuilder.shared.filteredRouteShapesForStop(
                    stopMapData: stopMapData,
                    filter: filter,
                    routeCardData: routeCardData
                )
            } else {
                mapVM.routeSourceData = RouteFeaturesBuilder.shared.forRailAtStop(
                    stopShapes: stopMapData.routeShapes,
                    railShapes: mapVM.allRailSourceData,
                    globalData: mapVM.globalData
                )
            }
            if let layerManager = mapVM.layerManager {
                addLayers(layerManager)
            }
        }
    }

    func updateGlobalMapDataSources() {
        mapVM.updateSources(globalData: mapVM.globalData, globalMapData: globalMapData)
    }

    func updateRouteSource() {
        mapVM.updateRouteSource(globalData: mapVM.globalData, globalMapData: globalMapData)
    }

    func updateStopSource() {
        mapVM.updateStopSource(globalMapData: globalMapData)
    }
}
