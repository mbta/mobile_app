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
import SwiftUI

class MockLayerManager: IMapLayerManager {
    var currentScheme: ColorScheme?
    private let addIconsCallback: () -> Void
    private let addLayersCallback: () -> Void
    private let updateChildStopDataCallback: (FeatureCollection) -> Void
    private let updateRouteDataCallback: (FeatureCollection) -> Void
    private let updateStopDataCallback: (FeatureCollection) -> Void

    init(addIconsCallback: @escaping () -> Void = {},
         addLayersCallback: @escaping () -> Void = {},
         updateChildStopDataCallback: @escaping (FeatureCollection) -> Void = { _ in },
         updateRouteDataCallback: @escaping (FeatureCollection) -> Void = { _ in },
         updateStopDataCallback: @escaping (FeatureCollection) -> Void = { _ in }) {
        self.addIconsCallback = addIconsCallback
        self.addLayersCallback = addLayersCallback
        self.updateChildStopDataCallback = updateChildStopDataCallback
        self.updateRouteDataCallback = updateRouteDataCallback
        self.updateStopDataCallback = updateStopDataCallback
    }

    func addIcons(recreate _: Bool = false) {
        addIconsCallback()
    }

    func addLayers(colorScheme: ColorScheme, recreate _: Bool = false) {
        currentScheme = colorScheme
        addLayersCallback()
    }

    func updateSourceData(routeData: FeatureCollection) {
        updateRouteDataCallback(routeData)
    }

    func updateSourceData(stopData: FeatureCollection) {
        updateStopDataCallback(stopData)
    }

    func updateSourceData(childStopData: FeatureCollection) {
        updateChildStopDataCallback(childStopData)
    }
}
