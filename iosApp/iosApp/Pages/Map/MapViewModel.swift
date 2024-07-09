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
    var layerManager: IMapLayerManager? = nil

    init(allRailSourceData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes] = [],
         layerManager: IMapLayerManager? = nil) {
        self.allRailSourceData = allRailSourceData
        self.layerManager = layerManager
    }

    func updateRouteSource(routeLines: [RouteLineData]) {
        layerManager?.updateSourceData(routeSource: RouteSourceGenerator.generateSource(routeLines: routeLines))
    }

    func updateStopSource(_ stopSource: GeoJSONSource) {
        layerManager?.updateSourceData(stopSource: stopSource)
    }

    func updateChildStopSource(_ childStopSource: GeoJSONSource) {
        layerManager?.updateSourceData(childStopSource: childStopSource)
    }
}
