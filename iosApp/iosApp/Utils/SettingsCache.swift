//
//  SettingsCache.swift
//  iosApp
//
//  Created by Melody Horn on 4/25/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

class SettingsCache: ObservableObject {
    var settingsRepo: ISettingsRepository
    @Published var cache: [Settings: Bool]?

    init(settingsRepo: ISettingsRepository = RepositoryDI().settings, cache: [Settings: Bool]? = nil) {
        self.settingsRepo = settingsRepo
        self.cache = cache
    }

    func get(_ setting: Settings) -> Bool {
        cache?[setting] ?? false
    }

    func load() async throws {
        cache = try await settingsRepo.getSettings().mapValues { $0.boolValue }
    }

    func set(_ setting: Settings, _ value: Bool) {
        let newSettings = [setting: value]
        cache = newSettings.merging(cache ?? [:]) { newValue, _ in newValue }
        Task {
            try await settingsRepo.setSettings(settings: [setting: KotlinBoolean(bool: value)])
        }
    }
}

/// Struggling to inject environment values because your unit tests are a complete disaster of pointer manipulation?
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

struct SettingsCacheProvider<Content: View>: View {
    let content: () -> Content
    @State var cache = SettingsCache()

    var body: some View {
        content()
            .task { try? await cache.load() }
            .environment(\.settingsCache, cache)
    }
}

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
