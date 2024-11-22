//
//  MockLocationFetcher.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 2/12/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
import iosApp
import XCTest

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

    func requestWhenInUseAuthorization() {}

    func updateLocations(locations: [CLLocation]) {
        locationFetcherDelegate?.locationFetcher(self, didUpdateLocations: locations)
    }
}

class MockOnboardingLocationFetcher: LocationFetcher {
    let requestExp: XCTestExpectation?

    init(requestExp: XCTestExpectation? = nil) {
        self.requestExp = requestExp
    }

    var locationFetcherDelegate: LocationFetcherDelegate? {
        didSet {
            // the real CLLocationManager will also do this, although maybe not at the same moment
            locationFetcherDelegate?.locationFetcherDidChangeAuthorization(self)
        }
    }

    var authorizationStatus: CLAuthorizationStatus = .notDetermined
    var distanceFilter: CLLocationDistance = 0
    func startUpdatingLocation() {}

    func requestWhenInUseAuthorization() {
        requestExp?.fulfill()
    }
}
