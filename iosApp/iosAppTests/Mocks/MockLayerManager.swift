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

class MockLayerManager: IMapLayerManager {
    var routeLayerGenerator: RouteLayerGenerator?
    var stopSourceGenerator: StopSourceGenerator?
    var stopLayerGenerator: StopLayerGenerator?
    var childStopSourceGenerator: ChildStopSourceGenerator?
    var childStopLayerGenerator: ChildStopLayerGenerator?
    private let addLayersCallback: () -> Void
    private let updateChildStopSourceCallback: (GeoJSONSource) -> Void
    private let updateRouteSourceCallback: (GeoJSONSource) -> Void
    private let updateStopSourceCallback: (GeoJSONSource) -> Void

    init(addLayersCallback: @escaping () -> Void = {},
         updateChildStopSourceCallback: @escaping (GeoJSONSource) -> Void = { _ in },
         updateRouteSourceCallback: @escaping (GeoJSONSource) -> Void = { _ in },
         updateStopSourceCallback: @escaping (GeoJSONSource) -> Void = { _ in }) {
        self.addLayersCallback = addLayersCallback
        self.updateChildStopSourceCallback = updateChildStopSourceCallback
        self.updateRouteSourceCallback = updateRouteSourceCallback
        self.updateStopSourceCallback = updateStopSourceCallback
    }

    func addLayers(
        routeLayerGenerator _: RouteLayerGenerator,
        stopLayerGenerator _: StopLayerGenerator,
        childStopLayerGenerator _: ChildStopLayerGenerator
    ) {
        addLayersCallback()
    }

    func updateSourceData(routeSource: GeoJSONSource) {
        updateRouteSourceCallback(routeSource)
    }

    func updateSourceData(stopSource: GeoJSONSource) {
        updateStopSourceCallback(stopSource)
    }

    func updateSourceData(childStopSource: GeoJSONSource) {
        updateChildStopSourceCallback(childStopSource)
    }
}
