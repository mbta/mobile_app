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
    @Published var settings = [SettingsSection]()

    private let settingsRepository: ISettingsRepository
    private var cancellables = Set<AnyCancellable>()

    init(settingsRepository: ISettingsRepository = RepositoryDI().settings) {
        self.settingsRepository = settingsRepository
        setUpSubscriptions()
    }

    func setUpSubscriptions() {
        $settings
            .dropFirst(2)
            .map { sections in
                sections.reduce([Setting]()) { partialResult, section in
                    partialResult + section.settings
                }
            }
            .sink { [weak self] settingsToCommit in
                guard let self else { return }
                setSettings(settingsToCommit)
            }
            .store(in: &cancellables)
    }

    func getSettings() async {
        do {
            let storedSettings = try await settingsRepository.getSettings()
            settings = [
                SettingsSection(
                    id: .debug,
                    settings: storedSettings.filter { $0.category == .debug }
                ),
                SettingsSection(
                    id: .featureFlags,
                    settings: storedSettings.filter { $0.category == .featureFlags }
                ),
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
