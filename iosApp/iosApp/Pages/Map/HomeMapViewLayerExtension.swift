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

        let stopSourceGenerator = StopSourceGenerator(
            stops: globalMapData?.mapStops ?? [:],
            selectedStop: lastNavEntry?.stop(),
            // TODO: routeLlines
            routeLines: []
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
            // TODO: route lines
            routeLines: []
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
            let filteredRouteWithShapes = filteredRouteShapesForStop(
                stopMapData: stopMapData,
                filter: filter,
                departures: departures
            )

            mapVM.routeSourceData = [filteredRouteWithShapes]

        } else {
            mapVM.routeSourceData = RouteSourceGenerator.forRailAtStop(stopMapData.routeShapes,
                                                                       mapVM.allRailSourceData,
                                                                       globalData?.routes,
                                                                       globalData?.stops,
                                                                       globalMapData?.alertsByStop)
        }

        let childStopSource = ChildStopSourceGenerator(childStops: stopMapData.childStops)
        mapVM.layerManager?.updateSourceData(childStopSourceGenerator: childStopSource)
    }

    func filteredRouteShapesForStop(
        stopMapData: StopMapResponse,
        filter: StopDetailsFilter,
        departures: StopDetailsDepartures?
    ) -> MapFriendlyRouteResponse.RouteWithSegmentedShapes {
        let targetRouteData = stopMapData.routeShapes.first { $0.routeId == filter.routeId }
        if let targetRouteData {
            if let departures {
                let upcomingRoutePatternIds: [String] = departures.routes
                    .flatMap { $0.allUpcomingTrips() }
                    .compactMap(\.trip.routePatternId)
                let targetRoutePatternIds: Set<String> = Set(upcomingRoutePatternIds)

                let filteredShapes = targetRouteData.segmentedShapes.filter { $0.directionId == filter.directionId &&
                    targetRoutePatternIds.contains($0.sourceRoutePatternId)
                }
                return .init(routeId: filter.routeId, segmentedShapes: filteredShapes)
            } else {
                let filteredShapes = targetRouteData.segmentedShapes.filter { $0.directionId == filter.directionId }
                return .init(routeId: filter.routeId, segmentedShapes: filteredShapes)
            }
        }

        return .init(routeId: filter.routeId, segmentedShapes: [])
    }

    func updateGlobalMapDataSources() {
        let updatedStopSources = StopSourceGenerator(
            stops: globalMapData?.mapStops ?? [:],
            selectedStop: lastNavEntry?.stop(),
            // TODO: routeLines
            routeLines: []
        )
        mapVM.layerManager?.updateSourceData(stopSourceGenerator: updatedStopSources)
        // If routes are already being displayed, keep using those. Otherwise, use the rail shapes
        let routeData = mapVM.routeSourceData
        mapVM.updateRouteSource(routeLines: RouteSourceGenerator.generateRouteLines(
            routeData: routeData,
            routesById: globalData?.routes,
            stopsById: globalData?.stops,
            alertsByStop: globalMapData?.alertsByStop
        ))
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
