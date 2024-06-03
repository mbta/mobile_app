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

        XCTAssertEqual(routeLayers.count, 4)
        let baseRouteLayer = routeLayers[0]
        XCTAssertEqual(baseRouteLayer.id, RouteLayerGenerator.routeLayerId)
        let alertingBgLayer = routeLayers[1]
        XCTAssertEqual(alertingBgLayer.id, RouteLayerGenerator.alertingBgRouteLayerId)
        let shuttledLayer = routeLayers[2]
        XCTAssertEqual(shuttledLayer.id, RouteLayerGenerator.shuttledRouteLayerId)
        let suspendedLayer = routeLayers[3]
        XCTAssertEqual(suspendedLayer.id, RouteLayerGenerator.suspendedRouteLayerId)

        XCTAssertNotNil(shuttledLayer.lineDasharray)
        XCTAssertNotNil(suspendedLayer.lineDasharray)
    }

    func testLayersHaveOffset() {
        let routeLayerGenerator = RouteLayerGenerator()
        let routeLayers = routeLayerGenerator.routeLayers

        XCTAssertEqual(routeLayers.count, 4)
        let baseRouteLayer = routeLayers[0]
        XCTAssertEqual(baseRouteLayer.id, RouteLayerGenerator.routeLayerId)
        let alertingBgLayer = routeLayers[1]
        XCTAssertEqual(alertingBgLayer.id, RouteLayerGenerator.alertingBgRouteLayerId)
        let shuttledLayer = routeLayers[2]
        XCTAssertEqual(shuttledLayer.id, RouteLayerGenerator.shuttledRouteLayerId)
        let suspendedLayer = routeLayers[3]
        XCTAssertEqual(suspendedLayer.id, RouteLayerGenerator.suspendedRouteLayerId)

        XCTAssertNotNil(shuttledLayer.lineOffset)
        XCTAssertNotNil(suspendedLayer.lineOffset)
    }

    func testBaseLayerColorFromData() {
        let routeLayerGenerator = RouteLayerGenerator()
        let routeLayers = routeLayerGenerator.routeLayers

        let baseRouteLayer = routeLayers[0]
        XCTAssertEqual(baseRouteLayer.lineColor, .expression(Exp(.get) {
            RouteSourceGenerator.propRouteColor
        }))
    }

    func testSortKeyFromData() {
        let routeLayerGenerator = RouteLayerGenerator()
        let routeLayers = routeLayerGenerator.routeLayers

        let baseRouteLayer = routeLayers[0]
        XCTAssertEqual(baseRouteLayer.lineSortKey, .expression(Exp(.get) {
            RouteSourceGenerator.propRouteSortKey
        }))
    }
}
