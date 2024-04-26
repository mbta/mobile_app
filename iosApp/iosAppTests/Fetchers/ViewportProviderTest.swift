//
//  ViewportProviderTest.swift
//  iosAppTests
//
//  Created by Simon, Emma on 3/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
@_spi(Experimental) import MapboxMaps
import XCTest

final class ViewportProviderTest: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testDefaultViewport() throws {
        let provider = ViewportProvider()
        XCTAssertNotNil(provider.viewport.camera)
        XCTAssertEqual(provider.viewport.camera?.center, ViewportProvider.Defaults.center)
        XCTAssertEqual(provider.viewport.camera?.zoom, ViewportProvider.Defaults.zoom)
    }

    func testFollowViewport() async throws {
        let provider = ViewportProvider()
        XCTAssertNotNil(provider.viewport.camera)
        provider.follow()
        XCTAssertNil(provider.viewport.camera)
        XCTAssertNotNil(provider.viewport.followPuck)
    }

    func testUpdateViewport() throws {
        let provider = ViewportProvider()
        XCTAssertNotNil(provider.viewport.camera)
        provider.viewport = .camera(center: .init(latitude: 0, longitude: 0))
        XCTAssertEqual(provider.viewport.camera?.center, .init(latitude: 0, longitude: 0))
    }
}
