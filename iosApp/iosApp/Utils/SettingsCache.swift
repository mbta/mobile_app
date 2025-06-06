//
//  SettingsCache.swift
//  iosApp
//
//  Created by Melody Horn on 4/25/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

/// Stores the state of the `Settings` so that they can be read instantly from anywhere once they have been loaded.
class SettingsCache: ObservableObject {
    var settingsRepo: ISettingsRepository
    @Published var cache: [Settings: Bool]?

    init(settingsRepo: ISettingsRepository = RepositoryDI().settings, cache: [Settings: Bool]? = nil) {
        self.settingsRepo = settingsRepo
        self.cache = cache
    }

    /// Retrieves the state of a `setting` from the cache.
    ///
    /// Unlike the Android version, does not transparently load in the background.
    func get(_ setting: Settings) -> Bool {
        cache?[setting] ?? false
    }

    /// Loads the cache from the settings repository. Should be called by `SettingsCacheProvider` in the full app.
    func load() async throws {
        cache = try await settingsRepo.getSettings().mapValues { $0.boolValue }
    }

    /// Edits the value of a single `setting` in both the cache and the settings repository.
    func set(_ setting: Settings, _ value: Bool) {
        let newSettings = [setting: value]
        cache = newSettings.merging(cache ?? [:]) { newValue, _ in newValue }
        Task {
            do {
                try await settingsRepo.setSettings(settings: [setting: KotlinBoolean(bool: value)])
            } catch {
                debugPrint(error)
                Sentry.shared.captureError(error: error)
            }
        }
    }
}

/// Provides the `SettingsCache` to the `content` and also loads the cache data.
struct SettingsCacheProvider<Content: View>: View {
    let content: () -> Content
    @State var cache = SettingsCache()

    var body: some View {
        content()
            .task { try? await cache.load() }
            .environmentObject(cache)
    }
}

extension View {
    /// Sets the settings within this view to some fixed value.
    func withFixedSettings(_ settings: [Settings: Bool]) -> some View {
        environmentObject(SettingsCache(cache: settings))
    }
}
