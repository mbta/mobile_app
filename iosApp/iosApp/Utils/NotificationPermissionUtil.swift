//
//  NotificationPermissionUtil.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 11/17/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
import os
import UIKit
import UserNotifications

enum NotificationPermissionUtil {
    // Note: The first time we make this authorization request, the system prompts the user to grant or deny the request
    // and records that response. Subsequent authorization requests do not prompt the user.
    /// Returns true if  granted, false if not
    @MainActor
    static func requestPermission() async -> Bool {
        let center = UNUserNotificationCenter.current()
        do {
            return try await center.requestAuthorization(options: [.alert, .sound, .badge]) == true
        } catch {
            Logger().error("Failed to request notification permissions: \(error)")
        }
        return false
    }

    /// Brings the user to the system notification settings for our app
    private static func openNotificationSettings() {
        guard let url = URL(string: UIApplication.openNotificationSettingsURLString),
              UIApplication.shared.canOpenURL(url) else { return }
        UIApplication.shared.open(url)
    }
}
