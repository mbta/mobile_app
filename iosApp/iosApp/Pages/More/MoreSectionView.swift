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
                if let name = section.name {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(name)
                            .foregroundStyle(Color.text)
                            .font(Typography.subheadlineSemibold)
                            .listRowInsets(EdgeInsets())
                            .accessibilityAddTraits(.isHeader)
                            .accessibilityHeading(.h2)
                        if let noteAbove = section.noteAbove {
                            Text(noteAbove)
                                .foregroundStyle(Color.text)
                                .font(Typography.footnote)
                        }
                    }
                    .padding(2)
                }
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(section.items.enumerated()), id: \.element.id) { index, row in
                        VStack(alignment: .leading, spacing: 0) {
                            switch row {
                            case let .toggle(label: label, setting: setting):
                                Toggle(isOn: Binding<Bool>(
                                    // Setting is hide maps, but label is "Map Display" - invert the
                                    // value of hide maps to match the label
                                    get: {
                                        setting == .hideMaps ? !settingsCache.get(setting) : settingsCache.get(setting)
                                    },
                                    set: { _ in settingsCache.set(setting, !settingsCache.get(setting)) }
                                )) { Text(label) }
                                    .padding(.vertical, 6)
                                    .padding(.horizontal, 16)
                                    .frame(minHeight: 44)
                            case let .link(label: label, url: url, note: note):
                                MoreLink(label: label, url: url, note: note, isKey: section.id == .feedback)
                                    .accessibilityAddTraits(section.id == .feedback ? [.isHeader] : [])
                                    .accessibilityHeading(section.id == .feedback ? .h2 : .unspecified)
                            case let .phone(label: label, phoneNumber: phoneNumber):
                                MorePhone(label: label, phoneNumber: phoneNumber)
                            case let .navLink(label: label, destination: destination):
                                MoreNavLink(label: label, destination: destination)
                            }
                            if index < section.items.count - 1 {
                                HaloSeparator().frame(maxWidth: .infinity)
                            }
                        }
                    }
                }
                .background(section.id == .feedback ? Color.key : Color.fill3)
                .withRoundedBorder()
                if let noteBelow = section.noteBelow {
                    Text(noteBelow)
                        .foregroundStyle(Color.text)
                        .font(Typography.footnote)
                        .padding(.top, 2)
                }
            }
        }
    }
}
