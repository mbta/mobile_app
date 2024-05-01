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
        let routeLayerGenerator = RouteLayerGenerator()
        let routeLayers = routeLayerGenerator.routeLayers

        XCTAssertEqual(routeLayers.count, 3)
        let baseRouteLayer = routeLayers[0]
        XCTAssertEqual(baseRouteLayer.id, RouteLayerGenerator.routeLayerId)
        let alertingBgLayer = routeLayers[1]
        XCTAssertEqual(alertingBgLayer.id, RouteLayerGenerator.alertingBgRouteLayerId)
        let alertingLayer = routeLayers[2]
        XCTAssertEqual(alertingLayer.id, RouteLayerGenerator.alertingRouteLayerId)

        XCTAssertNotNil(alertingLayer.lineDasharray)
    }

    func testLayersHaveOffset() {
        let routeLayerGenerator = RouteLayerGenerator()
        let routeLayers = routeLayerGenerator.routeLayers

        XCTAssertEqual(routeLayers.count, 3)
        let baseRouteLayer = routeLayers[0]
        XCTAssertNotNil(baseRouteLayer.lineOffset)
        let alertingBgLayer = routeLayers[1]
        XCTAssertNotNil(alertingBgLayer.lineOffset)
        let alertingLayer = routeLayers[1]
        XCTAssertNotNil(alertingLayer.lineOffset)
    }

    func testBaseLayerColorFromData() {
        let routeLayerGenerator = RouteLayerGenerator()
        let routeLayers = routeLayerGenerator.routeLayers

        let baseRouteLayer = routeLayers[0]
        XCTAssertEqual(baseRouteLayer.lineColor, .expression(Exp(.get) {
            RouteSourceGenerator.propRouteColor
        }))
    }
}
