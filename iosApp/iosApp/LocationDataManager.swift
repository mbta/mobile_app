//
//  LocationDataManager.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-01-18.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import CoreLocation
import Combine

class LocationDataManager : NSObject, CLLocationManagerDelegate, ObservableObject {
    var locationManager = CLLocationManager()
    @Published var currentLocation: CLLocation? = nil
    @Published var authorizationStatus = CLAuthorizationStatus.notDetermined

    override init() {
        super.init()
        locationManager.delegate = self
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus
        // TODO only if requested
        if (manager.authorizationStatus == .authorizedWhenInUse || manager.authorizationStatus == .authorizedAlways) {
            manager.startUpdatingLocation()
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        currentLocation = locations.last
    }
}
