//
//  SettingsExtension.swift
//  iosApp
//
//  Created by esimon on 1/22/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

extension [Settings: Bool] {
    func getSafe(_ setting: Settings) -> Bool { self[setting] ?? false }
}

extension [Settings: KotlinBoolean] {
    func getSafe(_ setting: Settings) -> Bool { self[setting]?.boolValue ?? false }
}

extension ISettingsRepository {
    func load(_ settings: [Settings]) async -> [Settings: Bool] {
        let loaded = try? await getSettings()
        return settings.reduce(into: [:]) { results, setting in
            results[setting] = loaded?.getSafe(setting) ?? false
        }
    }
}
