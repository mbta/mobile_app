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
import shared
@_spi(Experimental) import MapboxMaps

final class HomeMapViewTests: XCTestCase {
    struct NotUnderTestError: Error {}
    
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testNoLocationDefaultCenter() throws {
        let stopFetcher: StopFetcher = .init(backend: IdleBackend())
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        let sut = HomeMapView(stopFetcher: stopFetcher, locationDataManager: locationDataManager)
        XCTAssertEqual(sut.viewport.camera?.center, HomeMapView.defaultCenter)
    }

    func testFollowsPuckWhenUserLocationIsKnown() throws {
        let stopFetcher: StopFetcher = .init(backend: IdleBackend())
        let locationFetcher = MockLocationFetcher()
        locationFetcher.authorizationStatus = .authorizedAlways

        let locationDataManager: LocationDataManager = .init(locationFetcher: locationFetcher)
        let newLocation: CLLocation = .init(latitude: 42, longitude: -71)

        var sut = HomeMapView(stopFetcher: stopFetcher, locationDataManager: locationDataManager)

        let hasAppeared = sut.on(\.didAppear) { _ in
            XCTAssertNotNil(sut.viewport.followPuck)
        }
        ViewHosting.host(view: sut)
        locationFetcher.updateLocations(locations: [newLocation])
        XCTAssertEqual(locationDataManager.currentLocation, newLocation)

        wait(for: [hasAppeared], timeout: 5)
    }

    func testAllStopsCall() throws {
        class FakeStopFetcher: StopFetcher {
            let getStopsExpectation: XCTestExpectation
            
            init(getStopsExpectation: XCTestExpectation) {
                self.getStopsExpectation = getStopsExpectation
                super.init(backend: IdleBackend())
            }
            
            override func getAllStops() async throws {
                getStopsExpectation.fulfill()
                throw NotUnderTestError()
            }
        }
        
        let getStopsExpectation = expectation(description: "getAllStops")
        
        var sut = HomeMapView(stopFetcher: FakeStopFetcher(getStopsExpectation: getStopsExpectation))
        let hasAppeared = sut.on(\.didAppear) { _ in }
        ViewHosting.host(view: sut)
        wait(for: [hasAppeared], timeout: 5)
        wait(for: [getStopsExpectation], timeout: 1)
    }
}
