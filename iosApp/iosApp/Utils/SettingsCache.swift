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
    var settingsViewModel: SettingsViewModel
    @Published var cache: [Settings: Bool]

    init(settingsViewModel: SettingsViewModel = ViewModelDI().settings, cache: [Settings: Bool] = [:]) {
        self.settingsViewModel = settingsViewModel
        self.cache = cache
    }

    @MainActor
    func activate() async {
        for await state in settingsViewModel.models {
            cache = state.mapValues { $0.boolValue }
        }
    }

    /// Retrieves the state of a `setting` from the cache.
    ///
    /// Unlike the Android version, does not transparently load in the background.
    func get(_ setting: Settings) -> Bool {
        cache[setting] ?? false
    }

    /// Edits the value of a single `setting` in both the cache and the settings repository.
    func set(_ setting: Settings, _ value: Bool) {
        settingsViewModel.set(setting: setting, value: value)
    }
}

/// Provides the `SettingsCache` to the `content` and also loads the cache data.
struct SettingsCacheProvider<Content: View>: View {
    let content: () -> Content
    @State var cache = SettingsCache()

    var body: some View {
        content()
            .task { await cache.activate() }
            .environmentObject(cache)
    }
}

extension View {
    /// Sets the settings within this view to some fixed value.
    func withFixedSettings(_ settings: [Settings: Bool]) -> some View {
        environmentObject(SettingsCache(cache: settings))
    }
}
