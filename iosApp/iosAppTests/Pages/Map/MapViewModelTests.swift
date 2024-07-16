//
//  MapViewModelTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 7/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import XCTest

final class MapViewModelTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testUpdatesSources() {
        let updateRouteSourceCalled = XCTestExpectation(description: "Update route source called")
        let layerManager: IMapLayerManager = MockLayerManager(updateRouteDataCallback: { _ in
            updateRouteSourceCalled.fulfill()
        })

        let mapVM = MapViewModel(layerManager: layerManager)
        mapVM.updateRouteSource(routeLines: [])

        wait(for: [updateRouteSourceCalled], timeout: 1)
    }

    func testShapeFiltering() {
        let basicMapResponse = StopMapResponse(
            routeShapes: MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
            childStops: [:]
        )
        let filteredShapes = MapViewModel.filteredRouteShapesForStop(
            stopMapData: basicMapResponse,
            filter: .init(
                routeId: MapTestDataHelper.routeRed.id,
                directionId: MapTestDataHelper.patternRed10.directionId
            ),
            departures: nil
        )
        XCTAssertEqual(filteredShapes.count, 1)

        let glFilteredShapes = MapViewModel.filteredRouteShapesForStop(
            stopMapData: GreenLineHelper.stopMapResponse,
            filter: .init(routeId: "line-Green", directionId: 0),
            departures: nil
        )
        XCTAssertEqual(glFilteredShapes.count, 3)
        XCTAssertEqual(glFilteredShapes[0].segmentedShapes.count, 1)
    }
}
