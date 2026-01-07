//
//  DebugView.swift
//  iosApp
//
//  Created by Kayla Brady on 11/7/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftUI

struct DebugView<Content: View>: View {
    let content: () -> Content

    @EnvironmentObject var settingsCache: SettingsCache

    var body: some View {
        if settingsCache.get(.devDebugMode) {
            ZStack {
                Rectangle()
                    .strokeBorder(Color(.text), style: .init(lineWidth: 2, dash: [10]))
                VStack(alignment: .leading) {
                    content()
                        .font(Typography.footnote)
                }.padding(8)
            }
            .frame(maxWidth: .infinity)
            .background(Color.fill3)
            .padding(4)
            .fixedSize(horizontal: false, vertical: true)
        }
    }
}
