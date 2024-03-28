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
        let stopLayerGenerator = StopLayerGenerator(stopLayerTypes: [.stop, .station])
        let stopLayers = stopLayerGenerator.stopLayers

        XCTAssertEqual(stopLayers.count, 2)
        let stationLayer = stopLayers.first { $0.id == StopLayerGenerator.getStopLayerId(.station) }
        XCTAssertNotNil(stationLayer)
        guard let stationLayer else { return }
        XCTAssertEqual(stationLayer.iconImage, StopLayerGenerator.getStopLayerIcon(.station))
    }
}
