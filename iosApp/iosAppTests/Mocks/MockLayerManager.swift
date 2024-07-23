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
    var stopLayerGenerator: StopLayerGenerator?
    var childStopSourceGenerator: ChildStopSourceGenerator?
    var childStopLayerGenerator: ChildStopLayerGenerator?
    private let addLayersCallback: () -> Void
    private let updateChildStopSourceCallback: (GeoJSONSource) -> Void
    private let updateRouteDataCallback: (FeatureCollection) -> Void
    private let updateStopDataCallback: (FeatureCollection) -> Void

    init(addLayersCallback: @escaping () -> Void = {},
         updateChildStopSourceCallback: @escaping (GeoJSONSource) -> Void = { _ in },
         updateRouteDataCallback: @escaping (FeatureCollection) -> Void = { _ in },
         updateStopDataCallback: @escaping (FeatureCollection) -> Void = { _ in }) {
        self.addLayersCallback = addLayersCallback
        self.updateChildStopSourceCallback = updateChildStopSourceCallback
        self.updateRouteDataCallback = updateRouteDataCallback
        self.updateStopDataCallback = updateStopDataCallback
    }

    func addLayers(
        stopLayerGenerator _: StopLayerGenerator,
        childStopLayerGenerator _: ChildStopLayerGenerator,
        colorScheme _: ColorScheme
    ) {
        addLayersCallback()
    }

    func updateSourceData(routeData: FeatureCollection) {
        updateRouteDataCallback(routeData)
    }

    func updateSourceData(stopData: FeatureCollection) {
        updateStopDataCallback(stopData)
    }

    func updateSourceData(childStopSource: GeoJSONSource) {
        updateChildStopSourceCallback(childStopSource)
    }
}
