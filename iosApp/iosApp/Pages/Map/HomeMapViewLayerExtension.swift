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
              let _ = globalData,
              let _ = railRouteShapeFetcher.response,
              let _ = globalMapData?.mapStops,
              layerManager == nil
        else {
            return
        }
        handleLayerInit(map)
    }

    func handleLayerInit(_ map: MapboxMap) {
        let layerManager = MapLayerManager(map: map)
        initializeLayers(layerManager)
        self.layerManager = layerManager
    }

    func initializeLayers(_ layerManager: IMapLayerManager) {
        let routeSourceGenerator = RouteSourceGenerator(
            routeData: railRouteShapeFetcher.response?.routesWithSegmentedShapes ?? [],
            routesById: globalData?.routes,
            stopsById: globalData?.stops,
            alertsByStop: globalMapData?.alertsByStop
        )
        let stopSourceGenerator = StopSourceGenerator(
            stops: globalMapData?.mapStops ?? [:],
            selectedStop: lastNavEntry?.stop(),
            routeLines: routeSourceGenerator.routeLines
        )
        let childStopSourceGenerator = ChildStopSourceGenerator(childStops: nil)

        layerManager.addSources(
            routeSourceGenerator: routeSourceGenerator,
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
        let updatedRouteSources = RouteSourceGenerator(
            routeData: railRouteShapeFetcher.response?.routesWithSegmentedShapes ?? [],
            routesById: globalData?.routes,
            stopsById: globalData?.stops,
            alertsByStop: globalMapData?.alertsByStop
        )
        let updatedStopSources = StopSourceGenerator(
            stops: globalMapData?.mapStops ?? [:],
            selectedStop: nil,
            routeLines: updatedRouteSources.routeLines
        )
        let updatedChildStopSources = ChildStopSourceGenerator(childStops: nil)
        layerManager?.updateSourceData(
            routeSourceGenerator: updatedRouteSources,
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
            let filteredSource = RouteSourceGenerator(
                routeData: [filteredRouteWithShapes],
                routesById: globalData?.routes,
                stopsById: globalData?.stops,
                alertsByStop: globalMapData?.alertsByStop
            )
            layerManager?.updateSourceData(routeSourceGenerator: filteredSource)

        } else {
            let railRouteSource = RouteSourceGenerator.forRailAtStop(
                stopMapData.routeShapes,
                railRouteShapeFetcher.response?.routesWithSegmentedShapes ?? [],
                globalData?.routes,
                globalData?.stops,
                globalMapData?.alertsByStop
            )
            layerManager?.updateSourceData(routeSourceGenerator: railRouteSource)
        }

        let childStopSource = ChildStopSourceGenerator(childStops: stopMapData.childStops)
        layerManager?.updateSourceData(childStopSourceGenerator: childStopSource)
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
            routeLines: layerManager?.routeSourceGenerator?.routeLines
        )
        layerManager?.updateSourceData(stopSourceGenerator: updatedStopSources)
        // If routes are already being displayed, keep using those. Otherwise, use the rail shapes
        let routeData = layerManager?.routeSourceGenerator?.routeData ??
            railRouteShapeFetcher.response?.routesWithSegmentedShapes ??
            []
        let updatedRouteSources = RouteSourceGenerator(
            routeData: routeData,
            routesById: globalData?.routes,
            stopsById: globalData?.stops,
            alertsByStop: globalMapData?.alertsByStop
        )
        layerManager?.updateSourceData(routeSourceGenerator: updatedRouteSources)
    }
}
