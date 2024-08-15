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
    case stopDetailsFiltered = "StopDetailsFilteredPage"
    case stopDetailsUnfiltered = "StopDetailsUnfilteredPage"
    case settings = "SettingsPage"
}

protocol ScreenTracker {
    func track(screen: AnalyticsScreen)
}

extension AnalyticsProvider: ScreenTracker {
    func track(screen: AnalyticsScreen) {
        Analytics.logEvent(
            AnalyticsEventScreenView,
            parameters: [
                AnalyticsParameterScreenName: screen.rawValue,
            ]
        )
        if let appcues {
            appcues.screen(title: screen.rawValue)
        } else {
            debugPrint("Appcues not initialized")
        }
    }
}
