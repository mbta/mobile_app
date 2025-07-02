//
//  MapViewModelTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 7/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import XCTest

final class MapViewModelTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testSetsHttpInterceptor() {
        var interceptorSet = false
        let mapVM = MapViewModel(setHttpInterceptor: { _ in interceptorSet = true })
        XCTAssertTrue(interceptorSet)
    }

    func testUpdatesSources() {
        let updateRouteSourceCalled = XCTestExpectation(description: "Update route source called")
        let layerManager: iosApp.IMapLayerManager = MockLayerManager(updateRouteDataCallback: { _ in
            updateRouteSourceCalled.fulfill()
        })

        let mapVM = MapViewModel(layerManager: layerManager)
        mapVM.updateRouteSource(globalData: nil, globalMapData: nil)

        wait(for: [updateRouteSourceCalled], timeout: 1)
    }
}
