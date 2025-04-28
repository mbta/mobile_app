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
            try await settingsRepo.setSettings(settings: [setting: KotlinBoolean(bool: value)])
        }
    }
}

/// Struggling to inject environment values because your unit tests are a house of cards of pointer manipulation?
/// Simply put them in the default fallback! Nothing could possibly go wrong!
///
/// Always call `reset` if you called `set` or `setRepo`. Use `defer`, probably.
enum DefaultSettings {
    static let cache = SettingsCache(settingsRepo: MockSettingsRepository(), cache: nil)

    static func setRepo(_ repo: ISettingsRepository) {
        cache.settingsRepo = repo
    }

    static func set(_ settings: [Settings: Bool]) {
        cache.cache = settings
    }

    static func reset() {
        cache.settingsRepo = MockSettingsRepository()
        cache.cache = nil
    }
}

extension EnvironmentValues {
    @Entry var settingsCache: SettingsCache = DefaultSettings.cache
}

/// Provides the `EnvironmentValues.settingsCache` to the `content` and also loads the cache data.
struct SettingsCacheProvider<Content: View>: View {
    let content: () -> Content
    @State var cache = SettingsCache()

    var body: some View {
        content()
            .task { try? await cache.load() }
            .environment(\.settingsCache, cache)
    }
}

/// Retrieves the value of an individual setting from a `SettingsCache` retrieved from the environment.
///
/// May or may not actually update the component when the settings cache finishes loading, but in practice seems to.
@propertyWrapper struct GetSetting: DynamicProperty {
    let setting: Settings
    @Environment(\.settingsCache) var settingsCache: SettingsCache

    @inlinable init(_ setting: Settings) {
        self.setting = setting
    }

    @inlinable var wrappedValue: Bool {
        settingsCache.get(setting)
    }
}

extension View {
    /// Sets the settings within this view to some fixed value. Only works from previews, not ViewInspector tests.
    func withFixedSettings(_ settings: [Settings: Bool]) -> some View {
        environment(\.settingsCache, SettingsCache(cache: settings))
    }
}
