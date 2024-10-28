//
//  MoreSection.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/11/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared

struct MoreSection: Identifiable, Equatable {
    enum Category: String {
        case feedback
        case resources
        case settings
        case featureFlags
        case other
        case support
    }

    var id: Category
    var items: [MoreItem]

    var name: String? {
        switch id {
        case .feedback: nil
        case .resources: "Resources"
        case .settings: "Settings"
        case .featureFlags: "Feature Flags"
        case .other: nil
        case .support: "General MBTA Information & Support"
        }
    }

    var note: String? {
        switch id {
        case .support: "Monday through Friday: 6:30 AM - 8 PM"
        default: nil
        }
    }

    var requiresStaging: Bool {
        switch id {
        case .featureFlags: true
        default: false
        }
    }
}
