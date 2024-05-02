//
//  SettingsPage.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-04-30.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct SettingsPage: View {
    @State var isCool = false

    var body: some View {
        VStack {
            Text("Settings")
                .font(.title)
                .foregroundColor(isCool ? Color(hex: "BA75C7") : Color.primary)

            List {
                Toggle(isOn: $isCool) { Label("Cool", systemImage: "sparkles") }
            }
        }
    }
}

#Preview {
    SettingsPage()
}
