//
//  RouteSourceGeneratorTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 3/26/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Polyline
import shared
import XCTest
@_spi(Experimental) import MapboxMaps

final class RouteSourceGeneratorTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testRouteSourcesAreCreated() {
        let routeSourceGenerator = RouteSourceGenerator(routeData: MapTestDataHelper.routeResponse)

        XCTAssertEqual(routeSourceGenerator.routeSources.count, 2)

        let redSource = routeSourceGenerator.routeSources.first { $0.id == RouteSourceGenerator.getRouteSourceId(MapTestDataHelper.routeRed.id) }
        XCTAssertNotNil(redSource)
        if case let .featureCollection(collection) = redSource!.data.unsafelyUnwrapped {
            XCTAssertEqual(collection.features.count, 2)
            XCTAssertEqual(
                collection.features[0].geometry,
                .lineString(LineString(Polyline(encodedPolyline: MapTestDataHelper.shapeRedC2.polyline!).coordinates!))
            )
        } else {
            XCTFail("Red route source had no features")
        }

        let orangeSource = routeSourceGenerator.routeSources.first { $0.id == RouteSourceGenerator.getRouteSourceId(MapTestDataHelper.routeOrange.id) }
        XCTAssertNotNil(orangeSource)
        if case let .featureCollection(collection) = orangeSource!.data.unsafelyUnwrapped {
            XCTAssertEqual(collection.features.count, 1)
            XCTAssertEqual(
                collection.features[0].geometry,
                .lineString(LineString(Polyline(encodedPolyline: MapTestDataHelper.shapeOrangeC1.polyline!).coordinates!))
            )
        } else {
            XCTFail("Orange route source had no features")
        }
    }
}
