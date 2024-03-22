//
//  NearbyTransitLocationProvider.swift
//  iosApp
//
//  Created by Simon, Emma on 3/21/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation

class NearbyTransitLocationProvider: ObservableObject {
    let currentLocation: CLLocationCoordinate2D?
    let cameraLocation: CLLocationCoordinate2D
    let isFollowing: Bool

    @Published var location: CLLocationCoordinate2D

    init(currentLocation: CLLocationCoordinate2D? = nil, cameraLocation: CLLocationCoordinate2D, isFollowing: Bool) {
        self.currentLocation = currentLocation
        self.cameraLocation = cameraLocation
        self.isFollowing = isFollowing

        location = if isFollowing, currentLocation != nil { currentLocation! } else { cameraLocation }
    }
}
