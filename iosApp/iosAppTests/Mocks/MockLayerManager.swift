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
    private let updateRouteSourceCallback: (GeoJSONSource) -> Void

    init(addLayersCallback: @escaping () -> Void = {},
         updateRouteSourceCallback: @escaping (GeoJSONSource) -> Void = { _ in }) {
        self.addLayersCallback = addLayersCallback
        self.updateRouteSourceCallback = updateRouteSourceCallback
    }

    func addSources(
        stopSourceGenerator _: StopSourceGenerator,
        childStopSourceGenerator _: ChildStopSourceGenerator
    ) {}
    func addLayers(
        routeLayerGenerator _: RouteLayerGenerator,
        stopLayerGenerator _: StopLayerGenerator,
        childStopLayerGenerator _: ChildStopLayerGenerator
    ) {
        addLayersCallback()
    }

    func updateSourceData(
        stopSourceGenerator _: StopSourceGenerator,
        childStopSourceGenerator _: ChildStopSourceGenerator
    ) {}

    func updateSourceData(routeSource: GeoJSONSource) {
        updateRouteSourceCallback(routeSource)
    }

    func updateSourceData(stopSourceGenerator _: StopSourceGenerator) {}
    func updateSourceData(childStopSourceGenerator _: ChildStopSourceGenerator) {}
}
