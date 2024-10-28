//
//  MoreSectionView.swift
//  iosApp
//
//  Created by esimon on 10/28/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct MoreSectionView: View {
    var section: MoreSection
    var toggleSetting: (Settings) -> Void

    var body: some View {
        if !(section.requiresStaging && !(appVariant == .staging)) {
            VStack(alignment: .leading, spacing: 8) {
                if let name = section.name {
                    VStack(alignment: .leading, spacing: 0) {
                        Text(name)
                            .foregroundStyle(Color.text)
                            .font(Typography.subheadlineSemibold)
                            .listRowInsets(EdgeInsets())
                        if let note = section.note {
                            Text(note)
                                .foregroundStyle(Color.text)
                                .font(Typography.footnote)
                                .padding(.top, 2)
                        }
                    }
                    .padding(.bottom, 2)
                }
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(section.items.enumerated()), id: \.element.id) { index, row in
                        VStack(alignment: .leading, spacing: 0) {
                            switch row {
                            case let .toggle(setting: toggle):
                                Toggle(isOn: Binding<Bool>(
                                    get: { toggle.isOn },
                                    set: { _ in toggleSetting(toggle.key) }
                                )) { Text(row.label) }
                                    .padding(.vertical, 6)
                                    .padding(.horizontal, 16)
                                    .frame(minHeight: 44)
                            case let .link(label: label, url: url, note: note):
                                MoreLink(label: label, url: url, note: note)
                            case let .phone(label: label, phoneNumber: phoneNumber):
                                MorePhone(label: label, phoneNumber: phoneNumber)
                            }
                            if index < section.items.count {
                                Rectangle()
                                    .fill(Color.halo)
                                    .frame(height: 1)
                                    .frame(maxWidth: .infinity)
                            }
                        }
                    }
                }
                .background(Color.fill3)
                .clipShape(.rect(cornerRadius: 8.0))
                .overlay(
                    RoundedRectangle(cornerRadius: 8.0)
                        .stroke(Color.halo, lineWidth: 1.0)
                )
            }
        }
    }
}
