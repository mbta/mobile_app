//
//  NearbyTransitLocationProvider.swift
//  iosApp
//
//  Created by Simon, Emma on 3/21/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation

class NearbyTransitLocationProvider: ObservableObject {
    var currentLocation: CLLocationCoordinate2D?
    var cameraLocation: CLLocationCoordinate2D
    var isFollowing: Bool

    @Published var location: CLLocationCoordinate2D

    init(currentLocation: CLLocationCoordinate2D? = nil, cameraLocation: CLLocationCoordinate2D, isFollowing: Bool) {
        self.currentLocation = currentLocation
        self.cameraLocation = cameraLocation
        self.isFollowing = isFollowing

        location = Self.resolveLocation(currentLocation, cameraLocation, isFollowing)
        handleFollowingCamera()
    }

    static func resolveLocation(
        _ currentLocation: CLLocationCoordinate2D?,
        _ cameraLocation: CLLocationCoordinate2D,
        _ isFollowing: Bool
    ) -> CLLocationCoordinate2D {
        if isFollowing, currentLocation != nil { currentLocation! } else { cameraLocation }
    }

    func updateCurrentLocation(_ newLocation: CLLocationCoordinate2D?) {
        currentLocation = newLocation
        handleFollowingCamera()
        updateLocation()
    }

    func updateCameraLocation(_ newLocation: CLLocationCoordinate2D) {
        if cameraLocation.isRoughlyEqualTo(newLocation) {
            return
        }
        cameraLocation = newLocation
        updateLocation()
    }

    func updateIsFollowing(_ newFollowing: Bool, withCameraLocation newLocation: CLLocationCoordinate2D? = nil) {
        isFollowing = newFollowing
        if newLocation != nil {
            cameraLocation = newLocation!
        }
        handleFollowingCamera()
        updateLocation()
    }

    private func updateLocation() {
        location = Self.resolveLocation(currentLocation, cameraLocation, isFollowing)
    }

    /*
      Reset camera location to current location when viewport is following.
      This prevents loading stale camera locations when the viewport stops following.
     */
    private func handleFollowingCamera() {
        if isFollowing, currentLocation != nil {
            cameraLocation = currentLocation!
        }
    }
}
