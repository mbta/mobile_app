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

public class LocationDataManager : NSObject, LocationFetcherDelegate, ObservableObject {
    var locationFetcher: LocationFetcher
    @Published public var currentLocation: CLLocation? = nil
    @Published public var authorizationStatus = CLAuthorizationStatus.notDetermined

    public init(locationFetcher: LocationFetcher = CLLocationManager()) {
        self.locationFetcher = locationFetcher
        super.init()
        self.locationFetcher.locationFetcherDelegate = self
    }

    public func locationFetcherDidChangeAuthorization(_ fetcher: LocationFetcher) {
        authorizationStatus = fetcher.authorizationStatus
        // TODO only if requested
        if (fetcher.authorizationStatus == .authorizedWhenInUse || fetcher.authorizationStatus == .authorizedAlways) {
            fetcher.startUpdatingLocation()
        }
    }

    public func locationFetcher(_ fetcher: LocationFetcher, didUpdateLocations locations: [CLLocation]) {
        currentLocation = locations.last
    }
}

extension LocationDataManager : CLLocationManagerDelegate {
    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        self.locationFetcherDidChangeAuthorization(manager)
    }

    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        self.locationFetcher(manager, didUpdateLocations: locations)
    }
}

// https://developer.apple.com/videos/play/wwdc2018/417/
public protocol LocationFetcher {
    var locationFetcherDelegate: LocationFetcherDelegate? { get set }
    var authorizationStatus: CLAuthorizationStatus { get }
    func startUpdatingLocation()
    func requestWhenInUseAuthorization()
}

public protocol LocationFetcherDelegate: AnyObject {
    func locationFetcherDidChangeAuthorization(_ fetcher: LocationFetcher)
    func locationFetcher(_ fetcher: LocationFetcher, didUpdateLocations locations: [CLLocation])
}

extension CLLocationManager: LocationFetcher {
    public var locationFetcherDelegate: LocationFetcherDelegate? {
        // swiftlint:disable:next force_cast
        get { return delegate as! LocationFetcherDelegate? }
        // swiftlint:disable:next force_cast
        set { delegate = newValue as! CLLocationManagerDelegate? }
    }
}
