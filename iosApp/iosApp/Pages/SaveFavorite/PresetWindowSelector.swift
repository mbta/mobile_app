//
//  PresetWindowSelector.swift
//  iosApp
//

import Shared
import SwiftUI

struct PresetWindowSelector: View {
    @ObserveInjection var inject
    let presetRows: [[PresetWindow]]
    let selectedPreset: PresetSelection
    let now: EasternTimeInstant
    let customPreset: [FavoriteSettings.NotificationsWindow]
    let onSelect: ([FavoriteSettings.NotificationsWindow]) -> Void

    init(
        presetRows: [[PresetWindow]],
        selectedPreset: PresetSelection,
        now: EasternTimeInstant = .now(),
        customPreset: [FavoriteSettings.NotificationsWindow],
        onSelect: @escaping ([FavoriteSettings.NotificationsWindow]) -> Void
    ) {
        self.presetRows = presetRows
        self.selectedPreset = selectedPreset
        self.now = now
        self.customPreset = customPreset
        self.onSelect = onSelect
    }

    var body: some View {
        let presetGrid = VStack {
            ForEach(Array(presetRows.enumerated()), id: \.offset) { rowIndex, windows in
                HStack {
                    ForEach(Array(windows.enumerated()), id: \.element.label) { presetIndex, preset in
                        let isSelected = {
                            if case let .preset(preset) = onEnum(of: selectedPreset) {
                                return preset.rowIndex == rowIndex && preset.columnIndex == presetIndex
                            }
                            return false
                        }()
                        PresetButton(
                            isSelected: isSelected,
                            onSelect: { onSelect([preset.window]) },
                            label: preset.label
                        )
                        .frame(maxWidth: .infinity)
                    }
                }
            }

            HStack {
                PresetButton(
                    isSelected: selectedPreset == PresetSelection.Custom(),
                    onSelect: {
                        onSelect(customPreset)
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
