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
            ForEach(Array(presetRows.enumerated()), id: \.element) { _, presets in
                HStack {
                    ForEach(Array(presets.enumerated()), id: \.element.name) { _, preset in
                        let isSelected = preset == selectedPreset
                        let label = switch onEnum(of: preset) {
                        case let .morning: NSLocalizedString("Morning")
                        case let .midday: NSLocalizedString("Midday")
                        case let .evening: NSLocalizedString("Evening")
                        case let .allDay: NSLocalizedString("All day")
                        }
                        PresetButton(
                            isSelected: isSelected,
                            onSelect: { onSelect(preset) },
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
