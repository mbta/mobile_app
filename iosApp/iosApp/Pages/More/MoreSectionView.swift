//
//  MoreSectionView.swift
//  iosApp
//
//  Created by esimon on 10/28/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct MoreSectionView: View {
    var section: MoreSection

    @EnvironmentObject var settingsCache: SettingsCache

    var body: some View {
        if !(section.hiddenOnProd && appVariant == .prod) {
            VStack(alignment: .leading, spacing: 8) {
                header
                rows
                footer
            }
        }
    }

    @ViewBuilder
    private var header: some View {
        if let name = section.label?.value {
            VStack(alignment: .leading, spacing: 2) {
                Text(name)
                    .foregroundStyle(Color.text)
                    .font(Typography.subheadlineSemibold)
                    .listRowInsets(EdgeInsets())
                    .accessibilityAddTraits(.isHeader)
                    .accessibilityHeading(.h2)
                if let noteAbove = section.noteAbove {
                    Text(noteAbove.value)
                        .foregroundStyle(Color.text)
                        .font(Typography.footnote)
                }
            }
            .padding(2)
        }
    }

    @ViewBuilder
    private var rows: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(section.items.enumerated()), id: \.offset) { index, row in
                VStack(alignment: .leading, spacing: 0) {
                    switch onEnum(of: row) {
                    case let .toggle(toggle):
                        Toggle(isOn: Binding<Bool>(
                            // Setting is hide maps, but label is "Map Display" - invert the
                            // value of hide maps to match the label
                            get: {
                                toggle.settings == .hideMaps ? !settingsCache.get(toggle.settings) : settingsCache
                                    .get(toggle.settings)
                            },
                            set: { _ in settingsCache.set(toggle.settings, !settingsCache.get(toggle.settings)) }
                        )) { Text(toggle.label.value) }
                            .padding(.vertical, 6)
                            .padding(.horizontal, 16)
                            .frame(minHeight: 44)
                    case let .link(link):
                        MoreLink(
                            label: link.label.value,
                            url: link.url,
                            note: link.note?.value,
                            isKey: section.id == .feedback
                        )
                        .accessibilityAddTraits(section.id == .feedback ? [.isHeader] : [])
                        .accessibilityHeading(section.id == .feedback ? .h2 : .unspecified)
                    case let .phone(phone):
                        MorePhone(label: phone.label, phoneNumber: phone.phoneNumber)
                    case let .navLink(navLink):
                        MoreNavLink(label: navLink.label.value, callback: navLink.callback)
                    default:
                        EmptyView()
                    }
                    if index < section.items.count - 1 {
                        HaloSeparator().frame(maxWidth: .infinity)
                    }
                }
            }
        }
        .background(section.id == .feedback ? Color.key : Color.fill3)
        .withRoundedBorder()
    }

    @ViewBuilder
    private var footer: some View {
        if let noteBelow = section.noteBelow?.value {
            Text(noteBelow)
                .foregroundStyle(Color.text)
                .font(Typography.footnote)
                .padding(.top, 2)
        }
    }
}
