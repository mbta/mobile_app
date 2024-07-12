//
//  AnalyticsProvider.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 6/12/24.
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

class AnalyticsProvider: ObservableObject {

    weak var appcues: Appcues?

    init(appcues: Appcues? = nil) {
        self.appcues = appcues
    }

    /**
     * The `file` param is automatically populated with the call sites file path which we parse the class name from.
     * e.g.: `NearbyTransitAnalytics`
     */
    func logEvent(_ name: String, parameters: [String: Any] = [:], file: String = #file) {
        var params = parameters
        params["class_name"] = URL(string: file)?.deletingPathExtension().lastPathComponent
        Analytics.logEvent(name, parameters: params)
    }

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
            debugPrint("appcues instance not available")
        }
    }

}
