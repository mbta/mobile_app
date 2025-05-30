//
//  MoreToggle.swift
//  iosApp
//
//  Created by Melody Horn on 4/28/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct MoreToggle: View {
    let label: String
    let setting: Settings
    @ObservedObject var settingsCache: SettingsCache

    var body: some View {
        let value = settingsCache.get(setting)
        Toggle(isOn: Binding<Bool>(
            // Setting is hide maps, but label is "Map Display" - invert the
            // value of hide maps to match the label
            get: { setting == .hideMaps ? !value : value },
            set: { _ in settingsCache.set(setting, !value) }
        )) { Text(label) }
            .padding(.vertical, 6)
            .padding(.horizontal, 16)
            .frame(minHeight: 44)
    }
}

#Preview {
    MoreToggle(label: "Map Display", setting: .hideMaps, settingsCache: .init(settingsRepo: MockSettingsRepository()))
}
