//
//  PresetWindowSelector.swift
//  iosApp
//

import Shared
import SwiftUI

struct PresetWindowSelector: View {
    @ObserveInjection var inject
    let presetRows: [[Preset]]
    let selectedPreset: Preset?
    let onSelect: (Preset?) -> Void

    var body: some View {
        let presetGrid = VStack {
            ForEach(presetRows, id: \.hashValue) { presets in
                HStack {
                    ForEach(presets, id: \.self) { preset in
                        let isSelected = preset == selectedPreset
                        let label = switch preset {
                        case .morning: NSLocalizedString("Morning", comment: "Notification window preset label")
                        case .midday: NSLocalizedString("Midday", comment: "Notification window preset label")
                        case .evening: NSLocalizedString("Evening", comment: "Notification window preset label")
                        case .allDay: NSLocalizedString("All day", comment: "Notification window preset label")
                        }
                        PresetButton(
                            isSelected: isSelected,
                            onSelect: {
                                print("SELECTED PRESET \(preset)")
                                onSelect(preset)
                            },
                            label: label
                        )
                        .frame(maxWidth: .infinity)
                    }
                }
            }

            HStack {
                PresetButton(
                    isSelected: selectedPreset == nil,
                    onSelect: {
                        onSelect(nil)
                    },
                    label: NSLocalizedString(
                        "Custom",
                        comment: "Button text for selecting custom time range for notifications"
                    )
                )
                .frame(maxWidth: .infinity)
            }
        }
        .accessibilityElement(children: .contain)

        if #available(iOS 17.0, *) {
            presetGrid
                .accessibilityAddTraits(.isTabBar)
        } else {
            presetGrid
        }
    }
}

private struct PresetButton: View {
    @ObserveInjection var inject
    let isSelected: Bool
    let onSelect: () -> Void
    let label: String

    var body: some View {
        Button(action: onSelect) {
            Text(label)
                .font(Typography.footnoteSemibold)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
        }
        .foregroundStyle(isSelected ? Color.fill3 : Color.text.opacity(0.6))
        .background(isSelected ? Color.key : Color.fill1)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}
