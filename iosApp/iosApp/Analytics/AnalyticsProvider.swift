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



class AnalyticsProvider {

    static let shared = AnalyticsProvider()

    var appcues: Appcues? = nil

    /**
     * The `file` param is automatically populated with the call sites file path which we parse the class name from.
     * e.g.: `NearbyTransitAnalytics`
     */
    func logEvent(_ name: String, parameters: [String: Any] = [:], file: String = #file) {
        var params = parameters
        params["class_name"] = URL(string: file)?.deletingPathExtension().lastPathComponent
        Analytics.logEvent(name, parameters: params)
    }
}
