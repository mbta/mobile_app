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
    private var distanceFilter: Double

    public init(locationFetcher: LocationFetcher = CLLocationManager(), distanceFilter: Double = kCLDistanceFilterNone) {
        self.locationFetcher = locationFetcher
        self.distanceFilter = distanceFilter
        super.init()
        self.locationFetcher.locationFetcherDelegate = self
    }

    public func locationFetcherDidChangeAuthorization(_ fetcher: LocationFetcher) {
        authorizationStatus = fetcher.authorizationStatus
        // TODO: only if requested
        if fetcher.authorizationStatus == .authorizedWhenInUse || fetcher.authorizationStatus == .authorizedAlways {
            fetcher.distanceFilter = self.distanceFilter
            fetcher.startUpdatingLocation()
        }
    }

    public func locationFetcher(_: LocationFetcher, didUpdateLocations locations: [CLLocation]) {
        currentLocation = locations.last
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
