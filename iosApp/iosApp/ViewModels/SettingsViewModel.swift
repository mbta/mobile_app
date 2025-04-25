//
//  SettingsViewModel.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/11/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import Foundation
import Shared
import SwiftUI

class SettingsViewModel: ObservableObject {
    @Published var sections = [MoreSection]()

    // swiftlint:disable:next function_body_length
    init() {
        let feedbackFormUrl = localizedFeedbackFormUrl(
            baseUrl: "https://mbta.com/appfeedback",
            translation: Bundle.main.preferredLocalizations.first ?? "en",
            separateHTForm: true
        )
        sections = [
            MoreSection(id: .feedback, items: [
                .link(
                    label: NSLocalizedString(
                        "Send App Feedback",
                        comment: "Label for a More page link to a form to provide feedback on the app itself"
                    ),
                    url: feedbackFormUrl
                ),
            ]),
            MoreSection(id: .resources, items: [
                .link(
                    label: NSLocalizedString(
                        "Trip Planner",
                        comment: "Label for a More page link to the MBTA.com trip planner"
                    ),
                    url: "https://www.mbta.com/trip-planner"
                ),
                .link(
                    label: NSLocalizedString(
                        "Fare Information",
                        comment: "Label for a More page link to fare information on MBTA.com"
                    ),
                    url: "https://www.mbta.com/fares"
                ),
                .link(
                    label: NSLocalizedString(
                        "Commuter Rail and Ferry Tickets",
                        comment: "Label for a More page link to the MBTA mTicket app"
                    ),
                    url: "https://apps.apple.com/us/app/mbta-mticket/id560487958",
                    note: NSLocalizedString(
                        "mTicket App",
                        comment: "Footnote underneath the \"Commuter Rail and Ferry Tickets\" label on the More page link to the MBTA mTicket app"
                    )
                ),
            ]),
            MoreSection(id: .settings, items: [
                .toggle(
                    label: NSLocalizedString(
                        "Map Display",
                        comment: "A setting on the More page to show / hide maps from the app"
                    ),
                    setting: .hideMaps
                ),
                .toggle(
                    label: NSLocalizedString(
                        "Station Accessibility Info",
                        comment: "A setting on the More page to toggle displaying station accessibility info"
                    ),
                    setting: .stationAccessibility
                ),
            ]),
            MoreSection(id: .featureFlags, items: [
                .toggle(
                    label: NSLocalizedString(
                        "Debug Mode",
                        comment: "A setting on the More page to display debug information (only visible for developers)"
                    ),
                    setting: .devDebugMode
                ),
                .toggle(
                    label: "Enhanced Favorites",
                    setting: .enhancedFavorites
                ),
                .toggle(
                    label: NSLocalizedString(
                        "Route Search",
                        comment: "A setting on the More page to display routes in search (only visible for developers)"
                    ),
                    setting: .searchRouteResults
                ),
            ]),
            MoreSection(id: .other, items: [
                .link(
                    label: NSLocalizedString(
                        "Terms of Use",
                        comment: "Label for a More page link to the MBTA.com terms of use"
                    ),
                    url: "https://www.mbta.com/policies/terms-use"
                ),
                .link(
                    label: NSLocalizedString(
                        "Privacy Policy",
                        comment: "Label for a More page link to the MBTA.com privacy policy"
                    ),
                    url: "https://www.mbta.com/policies/privacy-policy"
                ),
                .navLink(
                    label: NSLocalizedString(
                        "Software Licenses",
                        comment: "Label for a More page link to view dependency licenses"
                    ),
                    destination: .licenses
                ),
                .link(
                    label: NSLocalizedString(
                        "View Source on GitHub",
                        comment: "Label for a More page link to the MBTA Go source code"
                    ),
                    url: "https://github.com/mbta/mobile_app"
                ),
            ]),
            MoreSection(id: .support, items: [
                .phone(label: "617-222-3200", phoneNumber: "6172223200"),
            ]),
        ]
    }
}
