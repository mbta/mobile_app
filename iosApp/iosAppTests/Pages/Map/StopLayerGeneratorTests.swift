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

        XCTAssertEqual(stopLayers.count, 11)
        XCTAssertEqual(stopLayers[0].id, StopLayerGenerator.stopLayerSelectedPinId)
        XCTAssertEqual(stopLayers[1].id, StopLayerGenerator.stopTouchTargetLayerId)
        XCTAssertEqual(stopLayers[2].id, StopLayerGenerator.busLayerId)
        XCTAssertEqual(stopLayers[3].id, StopLayerGenerator.busAlertLayerId)
        XCTAssertEqual(stopLayers[4].id, StopLayerGenerator.stopLayerId)
        XCTAssertEqual(stopLayers[5].id, StopLayerGenerator.getTransferLayerId(0))
        XCTAssertEqual(stopLayers[6].id, StopLayerGenerator.getTransferLayerId(1))
        XCTAssertEqual(stopLayers[7].id, StopLayerGenerator.getTransferLayerId(2))
        XCTAssertEqual(stopLayers[8].id, StopLayerGenerator.getAlertLayerId(0))
        XCTAssertEqual(stopLayers[9].id, StopLayerGenerator.getAlertLayerId(1))
        XCTAssertEqual(stopLayers[10].id, StopLayerGenerator.getAlertLayerId(2))
    }
}
