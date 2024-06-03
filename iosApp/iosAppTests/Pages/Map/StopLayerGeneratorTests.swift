//
//  StopLayerGeneratorTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 3/26/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import XCTest
@_spi(Experimental) import MapboxMaps

final class StopLayerGeneratorTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testStopLayersAreCreated() {
        let stopLayerGenerator = StopLayerGenerator()
        let stopLayers = stopLayerGenerator.stopLayers

        XCTAssertEqual(stopLayers.count, 5)
        XCTAssertEqual(stopLayers[0].id, StopLayerGenerator.stopTouchTargetLayerId)
        XCTAssertEqual(stopLayers[1].id, StopLayerGenerator.stopLayerId)
        XCTAssertEqual(stopLayers[2].id, StopLayerGenerator.getTransferLayerId(0))
        XCTAssertEqual(stopLayers[3].id, StopLayerGenerator.getTransferLayerId(1))
        XCTAssertEqual(stopLayers[4].id, StopLayerGenerator.getTransferLayerId(2))
    }
}
