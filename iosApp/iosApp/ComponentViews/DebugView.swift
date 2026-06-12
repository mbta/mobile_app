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
    @ObserveInjection var inject

    let content: () -> Content
    var details: (() -> Content)?

    @EnvironmentObject var settingsCache: SettingsCache
    @State var detailsPresented = false

    var body: some View {
        if settingsCache.get(.devDebugMode) {
            ZStack {
                Rectangle()
                    .strokeBorder(Color(.text), style: .init(lineWidth: 2, dash: [10]))
                HStack(alignment: .center, spacing: 8) {
                    VStack(alignment: .leading) {
                        content()
                            .font(Typography.footnote)
                    }
                    if details != nil {
                        Spacer()
                        Button {
                            detailsPresented = true
                        }
                        label: {
                            Image(systemName: "info.circle")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(height: 16)
                        }
                    }
                }.padding(8)
            }
            .frame(maxWidth: .infinity)
            .background(Color.fill3)
            .padding(4)
            .fixedSize(horizontal: false, vertical: true)
            .sheet(
                isPresented: $detailsPresented,
                content: {
                    NavigationStack {
                        ScrollView {
                            VStack(alignment: .leading) {
                                if let details {
                                    details()
                                        .font(Typography.footnote)
                                        .frame(alignment: .leading)
                                }
                            }
                            .padding(16)
                        }
                        .navigationTitle("Debug Details")
                        .toolbar {
                            ToolbarItem(placement: .confirmationAction) {
                                Button("Done") {
                                    detailsPresented = false
                                }
                            }
                        }
                    }
                }
            )
            .enableInjection()
        }
    }
}
