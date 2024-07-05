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
        let layerManager: IMapLayerManager = MockLayerManager(updateRouteSourceCallback: { _ in
            updateRouteSourceCalled.fulfill()
        })

        let mapVM = MapViewModel(layerManager: layerManager)
        mapVM.updateRouteSource(routeLines: [])

        wait(for: [updateRouteSourceCalled], timeout: 1)
    }
}
