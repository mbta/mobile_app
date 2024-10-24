//
//  SettingsSection.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/11/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared

struct SettingsSection: Identifiable, Equatable {
    enum Category: String {
        case settings
        case debug
        case featureFlags
    }

    var id: Category
    var name: String {
        switch id {
        case .settings:
            "Settings"
        case .debug:
            "Debug"
        case .featureFlags:
            "Feature Flags"
        }
    }

    var settings: [Setting]
    var requiresStaging: Bool {
        switch id {
        case .settings:
            false
        case .debug:
            false
        case .featureFlags:
            true
        }
    }
}
