//
//  AnalyticsProvider.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 6/12/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import FirebaseAnalytics
import Foundation
import Shared

class AnalyticsProvider: Shared.Analytics {
    static let shared = AnalyticsProvider()

    override func logEvent(name: String, parameters: [String: String]) {
        FirebaseAnalytics.Analytics.logEvent(name, parameters: parameters)
    }

    override func setUserProperty(name: String, value: String) {
        FirebaseAnalytics.Analytics.setUserProperty(value, forName: name)
    }
}
