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
    var settingsRepository: ISettingsRepository
    @State var mapDebug = false

    let inspection = Inspection<Self>()

    init(settingsRepository: ISettingsRepository = RepositoryDI().settings) {
        self.settingsRepository = settingsRepository
        mapDebug = false
    }

    var body: some View {
        VStack {
            Text("Settings")
                .font(.title)

            List {
                Toggle(isOn: $mapDebug) { Label("Map Debug", systemImage: "location.magnifyingglass") }
            }
        }
        .onChange(of: mapDebug) { mapDebug in
            Task {
                do {
                    try await settingsRepository.setMapDebug(mapDebug: mapDebug)
                } catch {
                    debugPrint("failed to save mapDebug", error)
                }
            }
        }
        .task {
            do {
                mapDebug = try await settingsRepository.getMapDebug().boolValue
            } catch {
                debugPrint("failed to load mapDebug", error)
                mapDebug = false
            }
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }
}

#Preview {
    SettingsPage()
}
