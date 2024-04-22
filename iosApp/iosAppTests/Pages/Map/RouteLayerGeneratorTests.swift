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
        let routeLayerGenerator = RouteLayerGenerator(mapFriendlyRoutesResponse: MapTestDataHelper.routeResponse,
                                                      routesById: [MapTestDataHelper.routeRed.id: MapTestDataHelper.routeRed,
                                                                   MapTestDataHelper.routeOrange.id: MapTestDataHelper.routeOrange])
        let routeLayers = routeLayerGenerator.routeLayers

        // 3 layers per route - non-alerting, alert, and alert background
        XCTAssertEqual(routeLayers.count, 6)
        let redRouteLayer = routeLayers.first { $0.id == RouteLayerGenerator.getRouteLayerId(MapTestDataHelper.routeRed.id) }
        XCTAssertNotNil(redRouteLayer)
        guard let redRouteLayer else { return }
        XCTAssertEqual(redRouteLayer.lineColor, .constant(StyleColor(.init(hex: MapTestDataHelper.routeRed.color))))

        let alertingRedLayer = routeLayers.first { $0.id == RouteLayerGenerator
            .getRouteLayerId("\(MapTestDataHelper.routeRed.id)-alerting")
        }

        XCTAssertNotNil(alertingRedLayer)
        XCTAssertNotNil(alertingRedLayer!.lineDasharray)
    }

    func testAlertingLayersLast() {
        let routeLayerGenerator = RouteLayerGenerator(mapFriendlyRoutesResponse: MapTestDataHelper.routeResponse,
                                                      routesById: [MapTestDataHelper.routeRed.id: MapTestDataHelper.routeRed,
                                                                   MapTestDataHelper.routeOrange.id: MapTestDataHelper.routeOrange])
        let routeLayers = routeLayerGenerator.routeLayers

        XCTAssertEqual(RouteLayerGenerator
            .getRouteLayerId(MapTestDataHelper.routeOrange.id), routeLayers[0].id)
        XCTAssertEqual(RouteLayerGenerator
            .getRouteLayerId(MapTestDataHelper.routeRed.id), routeLayers[1].id)
        XCTAssertEqual(RouteLayerGenerator
            .getRouteLayerId("\(MapTestDataHelper.routeOrange.id)-alerting-bg"), routeLayers[2].id)
        XCTAssertEqual(RouteLayerGenerator
            .getRouteLayerId("\(MapTestDataHelper.routeOrange.id)-alerting"), routeLayers[3].id)
        XCTAssertEqual(RouteLayerGenerator
            .getRouteLayerId("\(MapTestDataHelper.routeRed.id)-alerting-bg"), routeLayers[4].id)
        XCTAssertEqual(RouteLayerGenerator
            .getRouteLayerId("\(MapTestDataHelper.routeRed.id)-alerting"), routeLayers[5].id)
    }
}
