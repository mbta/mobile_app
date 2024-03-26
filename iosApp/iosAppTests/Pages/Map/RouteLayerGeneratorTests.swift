//
//  RouteLayerGeneratorTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 3/26/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import XCTest
@_spi(Experimental) import MapboxMaps

final class RouteLayerGeneratorTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testRouteLayersAreCreated() {
        let routeLayerGenerator = RouteLayerGenerator(routeData: MapTestDataHelper.routeResponse)
        let routeLayers = routeLayerGenerator.routeLayers

        XCTAssertEqual(routeLayers.count, 2)
        let redRouteLayer = routeLayers.first { $0.id == RouteLayerGenerator.getRouteLayerId(MapTestDataHelper.routeRed.id) }
        XCTAssertNotNil(redRouteLayer)
        guard let redRouteLayer else { return }
        XCTAssertEqual(redRouteLayer.lineColor, .constant(StyleColor(.init(hex: MapTestDataHelper.routeRed.color))))
    }
}
