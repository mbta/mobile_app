//
//  HomeMapViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 2/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import ViewInspector
@_spi(Experimental) import MapboxMaps
import XCTest

final class HomeMapViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testNoLocationDefaultCenter() throws {
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        let sut = HomeMapView(locationDataManager: locationDataManager)
        XCTAssertEqual(sut.viewport.camera?.center, HomeMapView.defaultCenter)
    }

    func testFollowsPuckWhenUserLocationIsKnown() throws {
        let locationFetcher = MockLocationFetcher()
        locationFetcher.authorizationStatus = .authorizedAlways

        let locationDataManager: LocationDataManager = .init(locationFetcher: locationFetcher)
        let newLocation: CLLocation = .init(latitude: 42, longitude: -71)
        locationFetcher.updateLocations(locations: [newLocation])
        XCTAssertEqual(locationDataManager.currentLocation, newLocation)

        var sut = HomeMapView(locationDataManager: locationDataManager)

        let hasAppeared = sut.on(\.didAppear) { _ in }
        ViewHosting.host(view: sut)

        wait(for: [hasAppeared], timeout: 1)


        XCTAssertEqual(sut.viewport.followPuck.debugDescription, "ASDF")
    }
}
