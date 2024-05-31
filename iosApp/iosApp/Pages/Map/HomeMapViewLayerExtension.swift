//
//  HomeMapViewLayerExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 5/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

/*
 Functions for manipulating the layers displayed on the map.
 */
extension HomeMapView {
    func initializeLayers(_ layerManager: IMapLayerManager) {
        let routeSourceGenerator = RouteSourceGenerator(
            routeData: railRouteShapeFetcher.response?.routesWithSegmentedShapes ?? [],
            routesById: globalFetcher.routes,
            stopsById: globalFetcher.stops,
            alertsByStop: currentStopAlerts
        )
        let stopSourceGenerator = StopSourceGenerator(
            stops: globalMapData?.mapStops ?? [:],
            selectedStop: lastNavEntry?.stop(),
            routeLines: routeSourceGenerator.routeLines,
            alertsByStop: currentStopAlerts
        )

        layerManager.addSources(
            routeSourceGenerator: routeSourceGenerator,
            stopSourceGenerator: stopSourceGenerator
        )

        layerManager.addLayers(
            routeLayerGenerator: RouteLayerGenerator(),
            stopLayerGenerator: StopLayerGenerator()
        )
    }

    func resetDefaultSources() {
        let updatedRouteSources = RouteSourceGenerator(
            routeData: railRouteShapeFetcher.response?.routesWithSegmentedShapes ?? [],
            routesById: globalFetcher.routes,
            stopsById: globalFetcher.stops,
            alertsByStop: currentStopAlerts
        )
        let updatedStopSources = StopSourceGenerator(
            stops: globalMapData?.mapStops ?? [:],
            selectedStop: nil,
            routeLines: updatedRouteSources.routeLines,
            alertsByStop: currentStopAlerts
        )
        layerManager?.updateSourceData(routeSourceGenerator: updatedRouteSources,
                                       stopSourceGenerator: updatedStopSources)
    }

    func updateStopDetailsLayers(
        _ stopMapData: StopMapResponse,
        _ filter: StopDetailsFilter?,
        _ departures: StopDetailsDepartures?
    ) {
        if let filter {
            let filteredRouteWithShapes = filteredRouteShapesForStop(stopMapData: stopMapData,
                                                                     filter: filter,
                                                                     departures: departures)
            let filteredSource = RouteSourceGenerator(routeData: [filteredRouteWithShapes],
                                                      routesById: globalFetcher.routes,
                                                      stopsById: globalFetcher.stops,
                                                      alertsByStop: currentStopAlerts)
            layerManager?.updateSourceData(routeSourceGenerator: filteredSource)

        } else {
            let railRouteSource = RouteSourceGenerator.forRailAtStop(stopMapData.routeShapes,
                                                                     railRouteShapeFetcher.response?
                                                                         .routesWithSegmentedShapes ?? [],
                                                                     globalFetcher.routes,
                                                                     globalFetcher.stops,
                                                                     currentStopAlerts)
            layerManager?.updateSourceData(routeSourceGenerator: railRouteSource)
        }
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

    func handleStopAlertChange(alertsByStop: [String: AlertAssociatedStop]) {
        let updatedStopSources = StopSourceGenerator(
            stops: globalMapData?.mapStops ?? [:],
            selectedStop: lastNavEntry?.stop(),
            routeLines: layerManager?.routeSourceGenerator?.routeLines,
            alertsByStop: alertsByStop
        )
        layerManager?.updateSourceData(stopSourceGenerator: updatedStopSources)
        // If routes are already being displayed, keep using those. Otherwise, use the rail shapes
        let routeData = layerManager?.routeSourceGenerator?.routeData ??
            railRouteShapeFetcher.response?.routesWithSegmentedShapes ??
            []
        let updatedRouteSources = RouteSourceGenerator(routeData: routeData,
                                                       routesById: globalFetcher.routes,
                                                       stopsById: globalFetcher.stops,
                                                       alertsByStop: alertsByStop)
        layerManager?.updateSourceData(routeSourceGenerator: updatedRouteSources)
    }
}
