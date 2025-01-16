//
//  SessionAnalytics.swift
//  iosApp
//
//  Created by Horn, Melody on 2025-01-14.
//  Copyright © 2025 MBTA. All rights reserved.
//

import CoreLocation
import FirebaseAnalytics
import SwiftUI

protocol SessionAnalytics {
    func recordSession(colorScheme: ColorScheme)
    func recordSession(voiceOver: Bool)
    func recordSession(hideMaps: Bool)
    func recordSession(locationAccess: CLAuthorizationStatus, locationAccuracy: CLAccuracyAuthorization)
}

extension AnalyticsProvider: SessionAnalytics {
    func recordSession(colorScheme: ColorScheme) {
        let colorScheme = switch colorScheme {
        case .light: "light"
        case .dark: "dark"
        @unknown default: "unknown"
        }
        Analytics.setUserProperty(colorScheme, forName: "color_scheme")
    }

    func recordSession(voiceOver: Bool) {
        let voiceOver = switch voiceOver {
        case true: "true"
        case false: "false"
        }
        Analytics.setUserProperty(voiceOver, forName: "screen_reader_on")
    }

    func recordSession(hideMaps: Bool) {
        let hideMaps = switch hideMaps {
        case true: "true"
        case false: "false"
        }
        Analytics.setUserProperty(hideMaps, forName: "hide_maps_on")
    }

    func recordSession(locationAccess: CLAuthorizationStatus, locationAccuracy: CLAccuracyAuthorization) {
        let locationAllowed = switch locationAccess {
        case .authorizedAlways, .authorizedWhenInUse: true
        case .notDetermined, .denied, .restricted: false
        @unknown default: false
        }
        let locationAccess = switch (locationAllowed, locationAccuracy) {
        case (true, .fullAccuracy): "precise"
        case (true, _): "approximate"
        case (false, _): "off"
        }
        Analytics.setUserProperty(locationAccess, forName: "location_access")
    }
}
