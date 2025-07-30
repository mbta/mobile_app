//
//  MoreSection.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/11/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import Shared

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
        case .resources: NSLocalizedString(
                "Resources",
                comment: "More page section header, includes links to MBTA.com and mTicket app"
            )
        case .settings: NSLocalizedString(
                "Settings",
                comment: "More page section header, includes settings that the user can configure"
            )
        case .featureFlags: NSLocalizedString(
                "Feature Flags",
                comment: "More page section header, only displayed in the developer app for enabling in-progress features"
            )
        case .other: nil
        case .support: NSLocalizedString(
                "General MBTA Information & Support",
                comment: "More page section header, includes user support resources"
            )
        }
    }

    var noteAbove: String? {
        switch id {
        case .support: NSLocalizedString(
                "Monday through Friday: 6:30 AM - 8 PM",
                comment: "Footnote under the More page support header, these are the hours for the support call center"
            )
        default: nil
        }
    }

    var noteBelow: String? {
        switch id {
        case .support: NSLocalizedString(
                "711 for TTY callers; VRS for ASL callers",
                comment: "Footnote under the More page support phone number, these are the instructions for accessible support via teletypewriter or video relay service"
            )
        default: nil
        }
    }

    var hiddenOnProd: Bool {
        switch id {
        case .featureFlags: true
        default: false
        }
    }
}
