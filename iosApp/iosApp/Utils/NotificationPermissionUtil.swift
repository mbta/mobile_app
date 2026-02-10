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

protocol INotificationPermissionManager {
    var authorizationStatus: UNAuthorizationStatus? { get }
    func requestPermission() async -> Bool
    func openNotificationSettings()
}

class NotificationPermissionManager: INotificationPermissionManager {
    private let center: UNUserNotificationCenter
    @Published var authorizationStatus: UNAuthorizationStatus?

    init() {
        center = UNUserNotificationCenter.current()
        center.getNotificationSettings { settings in
            self.authorizationStatus = settings.authorizationStatus
        }
    }

    // Note: The first time we make this authorization request, the system prompts the user to grant or deny the request
    // and records that response. Subsequent authorization requests do not prompt the user.
    /// Returns true if  granted, false if not
    @MainActor
    func requestPermission() async -> Bool {
        do {
            return try await center.requestAuthorization(options: [.alert, .sound, .badge]) == true
        } catch {
            Logger().error("Failed to request notification permissions: \(error)")
        }
        return false
    }

    /// Brings the user to the system notification settings for our app
    func openNotificationSettings() {
        guard let url = URL(string: UIApplication.openNotificationSettingsURLString),
              UIApplication.shared.canOpenURL(url) else { return }
        UIApplication.shared.open(url)
    }
}

class MockNotificationPermissionManager: INotificationPermissionManager {
    @Published var authorizationStatus: UNAuthorizationStatus?
    private var requestPermissionResponse: Bool
    private var onRequestPermission: () -> Void
    private var onOpenSettings: () -> Void

    init(
        initialAuthorizationStatus: UNAuthorizationStatus? = .authorized,
        requestPermissionResponse: Bool = true,
        onRequestPermission: @escaping () -> Void = {},
        onOpenSettings: @escaping () -> Void = {},
    ) {
        authorizationStatus = initialAuthorizationStatus
        self.requestPermissionResponse = requestPermissionResponse
        self.onRequestPermission = onRequestPermission
        self.onOpenSettings = onOpenSettings
    }

    func updateAuthorizationStatus(nextStatus: UNAuthorizationStatus) {
        authorizationStatus = nextStatus
    }

    @MainActor
    func requestPermission() async -> Bool {
        onRequestPermission()
        authorizationStatus = if requestPermissionResponse { .authorized } else { .denied }
        return requestPermissionResponse
    }

    func openNotificationSettings() {
        onOpenSettings()
    }
}
