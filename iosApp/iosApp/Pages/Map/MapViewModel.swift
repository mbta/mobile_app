//
//  MapViewModel.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-06-26.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

class MapViewModel: ObservableObject {
    @Published var selectedVehicle: Vehicle?
    @Published var routeSourceData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes] = []
    @Published var stopSourceData: StopSourceData?

    @Published var allRailSourceData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes] = []
    @Published var allStopSourceData: StopSourceData?
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
}
