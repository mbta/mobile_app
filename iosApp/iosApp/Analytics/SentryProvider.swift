//
//  SentryProvider.swift
//  iosApp
//
//  Created by Kayla Brady on 4/16/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Foundation
import Shared

class SentryProvider {
    static let shared = SentryRepository()

    override func logEvent(name: String, parameters: [String: String]) {
        FirebaseAnalytics.Analytics.logEvent(name, parameters: parameters)
    }

    override func setUserProperty(name: String, value: String) {
        FirebaseAnalytics.Analytics.setUserProperty(value, forName: name)
    }
}
