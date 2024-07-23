//
//  MapViewModel.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-06-26.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
@_spi(Experimental) import MapboxMaps

class MapViewModel: ObservableObject {
    @Published var childStops: [String: Stop]?
    @Published var selectedVehicle: Vehicle?
    @Published var routeSourceData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes] = []
    @Published var stopSourceData: StopSourceData = .init()
    @Published var stopMapData: StopMapResponse?

    @Published var allRailSourceData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes] = []
    var snappedStopRouteLines: [RouteLineData] = []
    var layerManager: IMapLayerManager?

    init(allRailSourceData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes] = [],
         layerManager: IMapLayerManager? = nil) {
        self.allRailSourceData = allRailSourceData
        self.layerManager = layerManager
    }

    func updateRouteSource(routeLines: [RouteLineData]) {
        layerManager?
            .updateSourceData(routeData: RouteFeaturesBuilder.shared.buildCollection(routeLines: routeLines).toMapbox())
    }

    func updateStopSource(_ stopData: FeatureCollection) {
        layerManager?.updateSourceData(stopData: stopData)
    }

    func updateChildStopSource(_ childStopSource: GeoJSONSource) {
        layerManager?.updateSourceData(childStopSource: childStopSource)
    }

    static func filteredRouteShapesForStop(
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
}
