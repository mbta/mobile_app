//
//  AnalyticsExtensions.swift
//  iosApp
//
//  Created by Horn, Melody on 2025-01-14.
//  Copyright © 2025 MBTA. All rights reserved.
//

import CoreLocation
import Shared
import SwiftUI

extension Analytics {
    func recordSession(colorScheme: ColorScheme) {
        let analyticsColorScheme: AnalyticsColorScheme = switch colorScheme {
        case .light: .light
        case .dark: .dark
        @unknown default: .unknown
        }
        recordSession(colorScheme: analyticsColorScheme)
    }

    func recordSession(locationAccess: CLAuthorizationStatus, locationAccuracy: CLAccuracyAuthorization) {
        let locationAllowed = switch locationAccess {
        case .authorizedAlways, .authorizedWhenInUse: true
        case .notDetermined, .denied, .restricted: false
        @unknown default: false
        }
        let locationAccess: AnalyticsLocationAccess = switch (locationAllowed, locationAccuracy) {
        case (true, .fullAccuracy): .precise
        case (true, _): .approximate
        case (false, _): .off
        }
        recordSession(locationAccess: locationAccess)
    }
}
