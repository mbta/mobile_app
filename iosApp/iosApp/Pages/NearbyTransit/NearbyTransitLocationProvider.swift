//
//  NearbyTransitLocationProvider.swift
//  iosApp
//
//  Created by Simon, Emma on 3/21/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation

struct NearbyTransitLocationProvider {
    let currentLocation: CLLocationCoordinate2D?
    let cameraLocation: CLLocationCoordinate2D
    let isFollowing: Bool

    var location: CLLocationCoordinate2D {
        if isFollowing, currentLocation != nil { currentLocation! } else { cameraLocation }
    }

    init(currentLocation: CLLocationCoordinate2D? = nil, cameraLocation: CLLocationCoordinate2D, isFollowing: Bool) {
        self.currentLocation = currentLocation
        self.cameraLocation = cameraLocation
        self.isFollowing = isFollowing
    }
}
