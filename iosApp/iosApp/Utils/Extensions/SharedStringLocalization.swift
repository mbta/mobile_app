//
//  SharedStringLocalization.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 10/1/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
import Shared

extension SharedString {
    var value: String {
        switch self {
        case .commuterRailAndFerryTickets:
            NSLocalizedString(
                "Commuter Rail and Ferry Tickets",
                comment: "Label for a More page link to the MBTA mTicket app"
            )
        case .debugMode:
            NSLocalizedString(
                "Debug Mode",
                comment: "A setting on the More page to display debug information (only visible for developers)"
            )
        case .fareInformation:
            NSLocalizedString(
                "Fare Information",
                comment: "Label for a More page link to fare information on MBTA.com"
            )
        case .featureFlagsSection:
            NSLocalizedString(
                "Feature Flags",
                comment: "More page section header, only displayed in the developer app for enabling in-progress features"
            )
        case .mapDisplay:
            NSLocalizedString(
                "Map Display",
                comment: "A setting on the More page to show / hide maps from the app"
            )
        case .mticketApp:
            NSLocalizedString(
                "mTicket App",
                comment: "Footnote underneath the \"Commuter Rail and Ferry Tickets\" label on the More page link to the MBTA mTicket app"
            )
        case .notifications:
            "Notifications" // Temp feature flag
        case .privacyPolicy:
            NSLocalizedString(
                "Privacy Policy",
                comment: "Label for a More page link to the MBTA.com privacy policy"
            )
        case .resourcesSection:
            NSLocalizedString(
                "Resources",
                comment: "More page section header, includes links to MBTA.com and mTicket app"
            )
        case .routeSearch:
            NSLocalizedString(
                "Route Search",
                comment: "A setting on the More page to display routes in search (only visible for developers)"
            )
        case .sendAppFeedback:
            NSLocalizedString(
                "Send App Feedback",
                comment: "Label for a More page link to a form to provide feedback on the app itself"
            )
        case .settingsSection:
            NSLocalizedString(
                "Settings",
                comment: "More page section header, includes settings that the user can configure"
            )
        case .softwareLicenses:
            NSLocalizedString(
                "Software Licenses",
                comment: "Label for a More page link to view dependency licenses"
            )
        case .stationAccessibilityInfo:
            NSLocalizedString(
                "Station Accessibility Info",
                comment: "A setting on the More page to toggle displaying station accessibility info"
            )
        case .supportAccessibilityNote:
            NSLocalizedString(
                "711 for TTY callers; VRS for ASL callers",
                comment: "Footnote under the More page support phone number, these are the instructions for accessible support via teletypewriter or video relay service"
            )
        case .supportHours:
            NSLocalizedString(
                "Monday through Friday: 6:30 AM - 8 PM",
                comment: "Footnote under the More page support header, these are the hours for the support call center"
            )
        case .supportSection:
            NSLocalizedString(
                "General MBTA Information & Support",
                comment: "More page section header, includes user support resources"
            )
        case .termsOfUse:
            NSLocalizedString(
                "Terms of Use",
                comment: "Label for a More page link to the MBTA.com terms of use"
            )
        case .tripPlanner:
            NSLocalizedString(
                "Trip Planner",
                comment: "Label for a More page link to the MBTA.com trip planner"
            )
        case .viewSourceOnGithub:
            NSLocalizedString(
                "View Source on GitHub",
                comment: "Label for a More page link to the MBTA Go source code"
            )
        }
    }
}
