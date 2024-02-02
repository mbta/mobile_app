//
//  LocationDataManager.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-01-18.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import CoreLocation
import Foundation

public class LocationDataManager: NSObject, LocationFetcherDelegate, ObservableObject {
    var locationFetcher: LocationFetcher
    @Published public var currentLocation: CLLocation?
    @Published public var authorizationStatus = CLAuthorizationStatus.notDetermined

    public init(locationFetcher: LocationFetcher = CLLocationManager()) {
        self.locationFetcher = locationFetcher
        super.init()
        self.locationFetcher.locationFetcherDelegate = self
    }

    public func locationFetcherDidChangeAuthorization(_ fetcher: LocationFetcher) {
        authorizationStatus = fetcher.authorizationStatus
        // TODO: only if requested
        if fetcher.authorizationStatus == .authorizedWhenInUse || fetcher.authorizationStatus == .authorizedAlways {
            // ignore updates less than 0.1km
            fetcher.distanceFilter = 100
            fetcher.startUpdatingLocation()
        }
    }

    public func locationFetcher(_: LocationFetcher, didUpdateLocations locations: [CLLocation]) {
        currentLocation = locations.last

        // workaround for denver not having any nearby stops
        guard let coordinate = locations.last?.coordinate else { return }
        if coordinate.longitude < -80 {
            currentLocation = CLLocation(latitude: coordinate.latitude + 2.8, longitude: coordinate.longitude + 33.9)
        }
    }
}

extension LocationDataManager: CLLocationManagerDelegate {
    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        locationFetcherDidChangeAuthorization(manager)
    }

    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        locationFetcher(manager, didUpdateLocations: locations)
    }
}

// https://developer.apple.com/videos/play/wwdc2018/417/
public protocol LocationFetcher: AnyObject {
    var locationFetcherDelegate: LocationFetcherDelegate? { get set }
    var authorizationStatus: CLAuthorizationStatus { get }
    var distanceFilter: CLLocationDistance { get set }
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
        get { delegate as! LocationFetcherDelegate? }
        // swiftlint:disable:next force_cast
        set { delegate = newValue as! CLLocationManagerDelegate? }
    }
}
