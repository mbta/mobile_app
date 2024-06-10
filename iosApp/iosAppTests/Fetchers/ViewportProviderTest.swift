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

    func testAnimateToPreservesZoomIfNotGiven() throws {
        let provider = ViewportProvider(viewport: .camera(center: .init(latitude: 0, longitude: 0), zoom: 14.0))

        provider.animateTo(coordinates: .init(latitude: 1, longitude: 1))
        XCTAssertEqual(provider.viewport.camera?.center, .init(latitude: 1, longitude: 1))
        XCTAssertEqual(provider.viewport.camera?.zoom, 14.0)
    }

    func testAnimateToSetsZoom() throws {
        let provider = ViewportProvider(viewport: .camera(center: .init(latitude: 0, longitude: 0), zoom: 14.0))

        provider.animateTo(coordinates: .init(latitude: 1, longitude: 1), zoom: 17.0)
        XCTAssertEqual(provider.viewport.camera?.center, .init(latitude: 1, longitude: 1))
        XCTAssertEqual(provider.viewport.camera?.zoom, 17.0)
    }

    func testSaveFollowing() throws {
        let provider = ViewportProvider(viewport: .followPuck(zoom: 16.0))

        provider.saveNearbyTransitViewport()
        provider.animateTo(coordinates: .init(latitude: 1, longitude: 1), zoom: 17.0)
        provider.restoreNearbyTransitViewport()

        XCTAssertEqual(provider.viewport.followPuck?.zoom, 16.0)
    }

    func testSavePanned() throws {
        let provider = ViewportProvider(viewport: .idle)

        let center = CLLocationCoordinate2D(latitude: 1, longitude: 2)
        let zoom = 16.0
        provider.updateCameraState(.init(center: center, padding: .zero, zoom: zoom, bearing: .zero, pitch: .zero))
        provider.saveNearbyTransitViewport()
        provider.animateTo(coordinates: .init(latitude: 3, longitude: 4), zoom: 17.0)
        provider.restoreNearbyTransitViewport()

        XCTAssertEqual(provider.viewport.camera?.center, center)
        XCTAssertEqual(provider.viewport.camera?.zoom, zoom)
    }

    func testRestoreWithoutSave() throws {
        // this shouldn't be possible, but if we implement e.g. deep linking in a way that makes this possible,
        // it's important that this not crash
        let provider = ViewportProvider(viewport: .camera(center: .init(latitude: 0, longitude: 0)))

        provider.restoreNearbyTransitViewport()

        XCTAssertEqual(provider.viewport.camera?.center, .init(latitude: 0, longitude: 0))
    }
}
