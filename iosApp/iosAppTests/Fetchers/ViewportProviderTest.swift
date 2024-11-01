//
//  ViewportProviderTest.swift
//  iosAppTests
//
//  Created by Simon, Emma on 3/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
@_spi(Experimental) import MapboxMaps
import shared
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

        let newCenter: CLLocationCoordinate2D = .init(latitude: 1, longitude: 1)
        let newZoom: CGFloat = 17.0
        provider.saveNearbyTransitViewport()
        provider.animateTo(coordinates: newCenter, zoom: newZoom)
        provider.updateCameraState(
            .init(center: newCenter, padding: .zero, zoom: newZoom, bearing: 0, pitch: 0)
        )
        provider.restoreNearbyTransitViewport()

        XCTAssertEqual(provider.viewport.followPuck?.zoom, newZoom)
    }

    func testSaveCameraLocation() throws {
        let startCenter: CLLocationCoordinate2D = .init(latitude: 0, longitude: 0)
        let provider = ViewportProvider(viewport: .camera(center: startCenter, zoom: 16.0))

        let newCenter: CLLocationCoordinate2D = .init(latitude: 1, longitude: 1)
        let newZoom: CGFloat = 14.0
        provider.saveNearbyTransitViewport()
        provider.animateTo(coordinates: newCenter, zoom: newZoom)
        provider.updateCameraState(
            .init(center: newCenter, padding: .zero, zoom: newZoom, bearing: 0, pitch: 0)
        )
        provider.restoreNearbyTransitViewport()

        XCTAssertEqual(provider.viewport.camera?.center, startCenter)
        XCTAssertEqual(provider.viewport.camera?.zoom, newZoom)
    }

    func testUpdateCameraLocationFromCoordinate() throws {
        let updatedExp = expectation(description: "updated camera")
        let startCenter: CLLocationCoordinate2D = .init(latitude: 0, longitude: 0)
        let provider = ViewportProvider(viewport: .camera(center: startCenter, zoom: 16.0))

        let newCenter: CLLocationCoordinate2D = .init(latitude: 1, longitude: 1)
        provider.updateCameraState(CLLocation(latitude: 1, longitude: 1))
        let cancelSink = provider.cameraStatePublisher.sink { cameraUpdate in
            XCTAssertEqual(cameraUpdate.center, newCenter)
            updatedExp.fulfill()
        }

        wait(for: [updatedExp], timeout: 1)
        cancelSink.cancel()
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

    func testSetIsManuallyCentering() throws {
        let provider = ViewportProvider()

        XCTAssertFalse(provider.isManuallyCentering)
        provider.setIsManuallyCentering(true)

        XCTAssertTrue(provider.isManuallyCentering)
    }

    func testFollowVehicle() throws {
        let objects = ObjectCollectionBuilder()
        let provider = ViewportProvider()

        let stop = objects.stop { stop in
            stop.latitude = 3.0
            stop.longitude = 3.0
        }

        let vehicle1 = objects.vehicle { vehicle in
            vehicle.currentStatus = .incomingAt
            vehicle.latitude = 1.0
            vehicle.longitude = 1.0
        }

        let vehicle2 = objects.vehicle { vehicle in
            vehicle.currentStatus = .incomingAt
            vehicle.latitude = 2.0
            vehicle.longitude = 2.0
        }

        XCTAssertFalse(provider.isFollowingVehicle)

        provider.followVehicle(vehicle: vehicle1, target: nil)

        XCTAssertTrue(provider.isFollowingVehicle)

        XCTAssertEqual(provider.viewport.camera?.center?.latitude, 1.0)
        XCTAssertEqual(provider.viewport.camera?.center?.longitude, 1.0)

        provider.followVehicle(vehicle: vehicle1, target: stop)

        if case let .multiPoint(points) = provider.viewport.overview!.geometry.geometry {
            XCTAssertEqual(
                points,
                MultiPoint([
                    vehicle1.coordinate,
                    stop.coordinate,
                    LocationCoordinate2D(latitude: -1.0, longitude: -1.0),
                ])
            )
        } else {
            XCTFail("Viewport wasn't set to an overview containing a multipoint geometry")
        }

        provider.updateFollowedVehicle(vehicle: vehicle2)

        XCTAssertEqual(provider.viewport.camera?.center?.latitude, 2.0)
        XCTAssertEqual(provider.viewport.camera?.center?.longitude, 2.0)

        XCTAssertTrue(provider.isFollowingVehicle)

        provider.updateFollowedVehicle(vehicle: nil)

        XCTAssertFalse(provider.isFollowingVehicle)
    }

    func testViewportAround() throws {
        let center = CLLocationCoordinate2D(latitude: 5.0, longitude: 5.0)
        let point1 = CLLocationCoordinate2D(latitude: 2.0, longitude: -2.0)
        let point2 = CLLocationCoordinate2D(latitude: -10.0, longitude: 15.0)

        let viewport = ViewportProvider.viewportAround(center: center, inView: [point1, point2])

        if case let .multiPoint(points) = viewport.overview!.geometry.geometry {
            XCTAssertEqual(
                points,
                MultiPoint([
                    center,
                    point1,
                    point2,
                    LocationCoordinate2D(latitude: 8.0, longitude: 12.0),
                    LocationCoordinate2D(latitude: 20.0, longitude: -5.0),
                ])
            )
        } else {
            XCTFail("Viewport wasn't set to an overview containing a multipoint geometry")
        }
    }

    func testViewportIsDefault() throws {
        let provider = ViewportProvider()
        XCTAssert(provider.isDefault())
        provider.animateTo(coordinates: .init(latitude: 0.0, longitude: 0.0))
        XCTAssertFalse(provider.isDefault())

        let initializedProvider = ViewportProvider(viewport: .followPuck(zoom: 12.0))
        XCTAssertFalse(initializedProvider.isDefault())
    }

    func testViewportSaving() throws {
        let provider = ViewportProvider()
        provider.viewport = .idle
        provider.updateCameraState(.init(
            center: .init(latitude: 1.1, longitude: 1.1),
            padding: .zero, zoom: 14.0, bearing: 0, pitch: 0
        ))

        provider.saveCurrentViewport()
        XCTAssertEqual(provider.viewport, .camera(center: .init(latitude: 1.1, longitude: 1.1), zoom: 14.0))

        provider.viewport = .followPuck(zoom: 12.0)
        provider.saveCurrentViewport()
        XCTAssertEqual(provider.viewport, .followPuck(zoom: 14.0))
    }
}
