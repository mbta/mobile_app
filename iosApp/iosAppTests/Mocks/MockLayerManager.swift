//
//  MockLayerManager.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 7/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
@_spi(Experimental) import MapboxMaps
import Shared
import SwiftUI

class MockLayerManager: IMapLayerManager {
    var currentScheme: ColorScheme?
    private let addIconsCallback: () -> Void
    private let addLayersCallback: () -> Void
    private let updateChildStopDataCallback: (MapboxMaps.FeatureCollection) -> Void
    private let updateRouteDataCallback: ([RouteSourceData]) -> Void
    private let updateStopDataCallback: (MapboxMaps.FeatureCollection) -> Void

    init(addIconsCallback: @escaping () -> Void = {},
         addLayersCallback: @escaping () -> Void = {},
         updateChildStopDataCallback: @escaping (MapboxMaps.FeatureCollection) -> Void = { _ in },
         updateRouteDataCallback: @escaping ([RouteSourceData]) -> Void = { _ in },
         updateStopDataCallback: @escaping (MapboxMaps.FeatureCollection) -> Void = { _ in }) {
        self.addIconsCallback = addIconsCallback
        self.addLayersCallback = addLayersCallback
        self.updateChildStopDataCallback = updateChildStopDataCallback
        self.updateRouteDataCallback = updateRouteDataCallback
        self.updateStopDataCallback = updateStopDataCallback
    }

    func addIcons(recreate _: Bool = false) {
        addIconsCallback()
    }

    func addLayers(
        routes _: [MapFriendlyRouteResponse.RouteWithSegmentedShapes],
        state _: StopLayerGenerator.State,
        globalResponse _: GlobalResponse,
        colorScheme: ColorScheme
    ) {
        currentScheme = colorScheme
        addLayersCallback()
    }

    func resetPuckPosition() {}

    func updateSourceData(routeData: [RouteSourceData]) {
        updateRouteDataCallback(routeData)
    }

    func updateSourceData(stopData: MapboxMaps.FeatureCollection) {
        updateStopDataCallback(stopData)
    }

    func updateSourceData(childStopData: MapboxMaps.FeatureCollection) {
        updateChildStopDataCallback(childStopData)
    }
}
