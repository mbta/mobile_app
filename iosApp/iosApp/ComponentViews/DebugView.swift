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
            .enableInjection()
        }
    }
}

struct AccordionDebugView<Content: View>: View {
    @ObserveInjection var inject

    let header: () -> Content
    let content: () -> Content
    @State var expanded = false

    @EnvironmentObject var settingsCache: SettingsCache

    var body: some View {
        if settingsCache.get(.devDebugMode) {
            ZStack {
                Rectangle()
                    .strokeBorder(Color(.text), style: .init(lineWidth: 2, dash: [10]))
                DisclosureGroup(
                    isExpanded: $expanded,
                    content: {
                        VStack(alignment: .leading) {
                            content()
                                .font(Typography.footnote)
                        }
                        .padding(8)
                        .frame(maxWidth: .infinity)
                    },
                    label: {
                        HStack(alignment: .center, spacing: 8) {
                            VStack(alignment: .leading) {
                                header()
                                    .font(Typography.footnote)
                            }
                            Spacer()
                            Image(.faChevronRight)
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(height: 16)
                                .rotationEffect(.degrees(expanded ? 90 : 0))
                        }.padding(8)
                    }
                )
                .disclosureGroupStyle(PlainDisclosureGroupStyle())
            }
            .frame(maxWidth: .infinity)
            .background(Color.fill3)
            .padding(4)
            .fixedSize(horizontal: false, vertical: true)
            .enableInjection()
        }
    }
}
