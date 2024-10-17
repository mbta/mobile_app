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
