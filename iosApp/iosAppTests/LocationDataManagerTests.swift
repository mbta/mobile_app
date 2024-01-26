//
//  LocationDataManagerTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-01-22.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
import iosApp
import XCTest

final class LocationDataManagerTests: XCTestCase {

    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

    class MockLocationFetcher: LocationFetcher {
        var distanceFilter: CLLocationDistance = 0
        weak var locationFetcherDelegate: LocationFetcherDelegate?

        var authorizationStatus: CLAuthorizationStatus = .notDetermined {
            didSet {
                locationFetcherDelegate?.locationFetcherDidChangeAuthorization(self)
            }
        }

        var handleStartUpdatingLocation: (() -> Void)?
        func startUpdatingLocation() {
            handleStartUpdatingLocation?()
        }

        func requestWhenInUseAuthorization() {
            XCTFail("should not have requested when-in-use authorization")
        }

        func updateLocations(locations: [CLLocation]) {
            locationFetcherDelegate?.locationFetcher(self, didUpdateLocations: locations)
        }
    }

    func testInit() async throws {
        let locationFetcher = MockLocationFetcher()

        XCTAssertEqual(locationFetcher.distanceFilter, 0)

        let manager = LocationDataManager(locationFetcher: locationFetcher)

        XCTAssertEqual(manager.authorizationStatus, .notDetermined)
        XCTAssertNil(manager.currentLocation)
        XCTAssertIdentical(manager, locationFetcher.locationFetcherDelegate)

        let updateLocationExpectation = expectation(description: "start updating location")
        locationFetcher.handleStartUpdatingLocation = {
            updateLocationExpectation.fulfill()
        }

        locationFetcher.authorizationStatus = .authorizedWhenInUse

        await fulfillment(of: [updateLocationExpectation], timeout: 0.1)

        XCTAssertEqual(manager.authorizationStatus, .authorizedWhenInUse)
        XCTAssertNil(manager.currentLocation)
        XCTAssertEqual(locationFetcher.distanceFilter, 100)

        let location = CLLocation(latitude: 1.2, longitude: 3.4)

        locationFetcher.updateLocations(locations: [location])

        XCTAssertEqual(manager.currentLocation, location)
    }
}
