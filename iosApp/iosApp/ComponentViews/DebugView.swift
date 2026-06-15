//
//  DebugView.swift
//  iosApp
//
//  Created by Kayla Brady on 11/7/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import Shared
import SwiftUI

struct DebugView<Content: View>: View {
    @ObserveInjection var inject

    let content: () -> Content
    var details: (() -> Content)?

    @EnvironmentObject var settingsCache: SettingsCache
    @State var detailsPresented = false
    @State var debugRepository = RepositoryDI().debug
    @State private var debugState: DebugState? = nil
    @State private var now = EasternTimeInstant.now()
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        if settingsCache.get(.devDebugMode) {
            ZStack {
                Rectangle()
                    .strokeBorder(Color(.text), style: .init(lineWidth: 2, dash: [10]))
                HStack(alignment: .center, spacing: 8) {
                    VStack(alignment: .leading) {
                        content()

                        if let channelUpdates = debugState?.channelUpdates {
                            Text(verbatim: "channel connections:")
                            ForEach(channelUpdates.keys.sorted(), id: \.self) { key in
                                let trimmedKey = if key.count > 25 { "\(key.prefix(25))..." } else { key }
                                let updateDuration = if let updateTime = channelUpdates[key] {
                                    Duration(
                                        secondsComponent: abs(now.minus(updateTime).inWholeSeconds),
                                        attosecondsComponent: 0
                                    )
                                    .formatted(.units(allowed: [.seconds], width: .narrow))
                                } else { "??" }

                                Text(verbatim: "\(trimmedKey) last updated \(updateDuration) ago")
                            }
                        }
                    }
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
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
                .padding(8)
            }
            .task {
                for await state in debugRepository.state {
                    debugState = state
                }
            }
            .onReceive(timer) { input in now = input.toEasternInstant() }
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
