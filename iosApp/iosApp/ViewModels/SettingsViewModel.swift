//
//  SettingsViewModel.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/11/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
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
                    .link(label: "Trip Planner", url: "https://www.mbta.com/trip-planner"),
                    .link(label: "Fare Information", url: "https://www.mbta.com/fares"),
                    .link(
                        label: "Commuter Rail and Ferry tickets",
                        url: "https://apps.apple.com/us/app/mbta-mticket/id560487958",
                        note: "mTicket App"
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
                    .link(label: "Terms of Use", url: "https://www.mbta.com/policies/terms-use"),
                    .link(label: "Privacy Policy", url: "https://www.mbta.com/policies/privacy-policy"),
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
