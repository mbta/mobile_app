//
//  ScreenTracker.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/9/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import AppcuesKit
import FirebaseAnalytics
import Foundation

enum AnalyticsScreen: String {
    case nearbyTransit = "NearbyTransitPage"
    case tripDetails = "TripDetailsPage"
    case stopDetails = "StopDetailsPage"
    case settings = "SettingsPage"
}

protocol ScreenTracker {
    func track(screen: AnalyticsScreen)
}

extension AnalyticsProvider: ScreenTracker {
    private var appcues: Appcues? {
        AppDelegate.instance.appcues
    }

    func track(screen: AnalyticsScreen) {
        Analytics.logEvent(
            AnalyticsEventScreenView,
            parameters: [
                AnalyticsParameterScreenName: screen.rawValue,
            ]
        )
        appcues?.screen(title: screen.rawValue)
    }
}
