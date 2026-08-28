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
    let presetsEnabled: Bool
    let now: EasternTimeInstant
    let onSelect: (FavoriteSettings.NotificationsWindow) -> Void

    init(
        presetRows: [[PresetWindow]],
        selectedPreset: PresetSelection,
        presetsEnabled: Bool,
        now: EasternTimeInstant = .now(),
        onSelect: @escaping (FavoriteSettings.NotificationsWindow) -> Void
    ) {
        self.presetRows = presetRows
        self.selectedPreset = selectedPreset
        self.presetsEnabled = presetsEnabled
        self.now = now
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
                            enabled: presetsEnabled,
                            isSelected: isSelected,
                            onSelect: { onSelect(preset.window) },
                            label: preset.label
                        )
                        .frame(maxWidth: .infinity)
                    }
                }
            }

            HStack {
                PresetButton(
                    enabled: true,
                    isSelected: selectedPreset == PresetSelection.Custom(),
                    onSelect: {
                        onSelect(FavoriteSettings.NotificationsWindow.companion.customFromCurrentTime(now: now))
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
    let enabled: Bool
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
        .disabled(!enabled)
        .foregroundStyle(isSelected ? Color.fill3 : Color.text.opacity(0.6))
        .background(isSelected ? Color.key : Color.fill1)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}
