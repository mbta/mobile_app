//
//  SettingsPage.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-04-30.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct SettingsPage: View {
    let inspection = Inspection<Self>()

    @StateObject var viewModel = SettingsViewModel()

    var body: some View {
        VStack {
            Text("Settings")
                .font(Typography.title1)

            List($viewModel.settings) { $section in
                if !(section.requiresStaging && !(appVariant == .staging)) {
                    Section(section.name) {
                        ForEach($section.settings) { $row in
                            Toggle(isOn: $row.isOn) { Label(row.name, systemImage: row.icon) }
                        }
                    }
                }
            }
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .task { await viewModel.getSettings() }
    }
}

#Preview {
    SettingsPage()
        .font(Typography.body)
}
