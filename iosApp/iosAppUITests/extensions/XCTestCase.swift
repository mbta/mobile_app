//
//  XCTestCase.swift
//  iosApp
//
//  Created by Kayla Brady on 11/12/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import XCTest

extension XCTestCase {
    func addNotificationsPermissionPromptHandler() {
        addUIInterruptionMonitor(withDescription: "Push Notifications Alert") { alert -> Bool in
            if alert.buttons["Allow"].exists {
                alert.buttons["Allow"].tap()
                return true
            }
            return false
        }
    }

    func addLocationPermissionPromptHandler() {
        addUIInterruptionMonitor(withDescription: "Location Permission Alert") { alert -> Bool in
            let allowLocationButton = alert.buttons["Allow Once"]
            if allowLocationButton.exists {
                allowLocationButton.tap()
                return true
            }
            return false
        }
    }
}
