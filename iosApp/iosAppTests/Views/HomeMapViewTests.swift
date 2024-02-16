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

final class HomeMapViewTests: XCTestCase {
    struct NotUnderTestError: Error {}
    
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testNoLocationDefaultCenter() throws {
        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend())
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        let sut = HomeMapView(globalFetcher: globalFetcher, locationDataManager: locationDataManager)
        XCTAssertEqual(sut.viewport.camera?.center, HomeMapView.defaultCenter)
    }

    func testFollowsPuckWhenUserLocationIsKnown() throws {
        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend())
        let locationFetcher = MockLocationFetcher()
        locationFetcher.authorizationStatus = .authorizedAlways

        let locationDataManager: LocationDataManager = .init(locationFetcher: locationFetcher)
        let newLocation: CLLocation = .init(latitude: 42, longitude: -71)

        var sut = HomeMapView(globalFetcher: globalFetcher, locationDataManager: locationDataManager)

        let hasAppeared = sut.on(\.didAppear) { _ in
            XCTAssertNotNil(sut.viewport.followPuck)
        }
        ViewHosting.host(view: sut)
        locationFetcher.updateLocations(locations: [newLocation])
        XCTAssertEqual(locationDataManager.currentLocation, newLocation)

        wait(for: [hasAppeared], timeout: 5)
    }

    func testGlobalCall() throws {
        class FakeGlobalFetcher: GlobalFetcher {
            let getGlobalExpectation: XCTestExpectation
            
            init(getGlobalExpectation: XCTestExpectation) {
                self.getGlobalExpectation = getGlobalExpectation
                super.init(backend: IdleBackend())
            }
            
            override func getGlobalData() async throws {
                getGlobalExpectation.fulfill()
                throw NotUnderTestError()
            }
        }
        
        let getGlobalExpectation = expectation(description: "getGlobalData")
        
        var sut = HomeMapView(globalFetcher: FakeGlobalFetcher(getGlobalExpectation: getGlobalExpectation))
        let hasAppeared = sut.on(\.didAppear) { _ in }
        ViewHosting.host(view: sut)
        wait(for: [hasAppeared], timeout: 5)
        wait(for: [getGlobalExpectation], timeout: 1)
    }
}
