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
            mapVM.routeSourceData = filteredRouteShapesForStop(
                stopMapData: stopMapData,
                filter: filter,
                departures: departures
            )
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
    ) -> [MapFriendlyRouteResponse.RouteWithSegmentedShapes] {
        // TODO: When we switch to a more involved filter and pinning ID type system,
        // this should be changed to be less hard coded and do this for any line
        // (we'll then need to figure out how to get corresponding route ids for each)
        let filterRoutes = filter.routeId == "line-Green" ?
            Array(greenRoutes)
            : [filter.routeId]
        let targetRouteData = stopMapData.routeShapes.filter {
            filterRoutes.contains($0.routeId)
        }

        if !targetRouteData.isEmpty {
            if let departures {
                let upcomingRoutePatternIds: [String] = departures.routes
                    .flatMap { $0.allUpcomingTrips() }
                    .compactMap(\.trip.routePatternId)
                let targetRoutePatternIds: Set<String> = Set(upcomingRoutePatternIds)

                return targetRouteData.map { routeData in
                    let filteredShapes = routeData.segmentedShapes.filter {
                        $0.directionId == filter.directionId &&
                            targetRoutePatternIds.contains($0.sourceRoutePatternId)
                    }
                    return .init(routeId: routeData.routeId, segmentedShapes: filteredShapes)
                }
            } else {
                return targetRouteData.map { routeData in
                    let filteredShapes = routeData.segmentedShapes.filter {
                        $0.directionId == filter.directionId
                    }
                    return .init(routeId: routeData.routeId, segmentedShapes: filteredShapes)
                }
            }
        }

        return [.init(routeId: filter.routeId, segmentedShapes: [])]
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
