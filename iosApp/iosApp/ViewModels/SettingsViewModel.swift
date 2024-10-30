//
//  SettingsViewModel.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/11/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import Foundation
import shared

class SettingsViewModel: ObservableObject {
    @Published var sections = [MoreSection]()

    private let settingsRepository: ISettingsRepository
    private var settings: Set<Setting> = Set()
    private var cancellables = Set<AnyCancellable>()

    init(settingsRepository: ISettingsRepository = RepositoryDI().settings) {
        self.settingsRepository = settingsRepository
    }

    func toggleSetting(key: Settings) {
        setSettings(settings.map { setting in
            if key == setting.key {
                setting.isOn = !setting.isOn
            }
            return setting
        })
    }

    @MainActor func getSections() async {
        do {
            settings = try await settingsRepository.getSettings()
            sections = [
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
                            "Commuter Rail and Ferry tickets",
                            comment: "Label for a More page link to the MBTA mTicket app"
                        ),
                        url: "https://apps.apple.com/us/app/mbta-mticket/id560487958",
                        note: NSLocalizedString(
                            "mTicket App",
                            comment: "Footnote underneath the \"Commuter Rail and Ferry tickets\" label on the More page link to the MBTA mTicket app"
                        )
                    ),
                ]),
                MoreSection(
                    id: .settings,
                    items: settings.filter { $0.category == .settings }.map { MoreItem.toggle(setting: $0) }
                ),
                MoreSection(
                    id: .featureFlags,
                    items: settings.filter { $0.category == .featureFlags }.map { MoreItem.toggle(setting: $0) }
                ),
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
                ]),
                MoreSection(id: .support, items: [
                    .phone(label: "617-222-3200", phoneNumber: "6172223200"),
                ]),
            ]
        } catch {
            debugPrint("failed to load settings")
        }
    }

    private func setSettings(_ settings: [Setting]) {
        Task {
            try await settingsRepository.setSettings(settings: Set(settings))
        }
    }
}
