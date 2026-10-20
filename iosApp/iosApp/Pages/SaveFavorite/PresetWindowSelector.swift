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
        let presetGrid = VStack(spacing: 4) {
            ForEach(presetRows, id: \.hashValue) { presets in
                HStack(spacing: 4) {
                    ForEach(presets, id: \.self) { preset in
                        let isSelected = preset == selectedPreset
                        let description = NSLocalizedString(
                            "start to end of service",
                            comment: "Description for the all-day notification preset"
                        )

                        if preset == .allDay {
                            PresetButton(
                                isSelected: isSelected,
                                onSelect: {
                                    onSelect(preset)
                                },
                                label: NSLocalizedString("All day", comment: "Notification window preset label"),
                                description: {
                                    Text(description).font(Typography.callout).foregroundStyle(Color.text.opacity(0.6))
                                },
                                centerContent: false
                            )
                            .frame(maxWidth: .infinity)
                        } else {
                            let startTimeFormatted = DateComponents.fromLocalTime(preset.startTime).formatHour()
                            let endTimeFormatted = DateComponents.fromLocalTime(preset.endTime).formatHour()

                            let label = switch preset {
                            case .morning: NSLocalizedString("Morning", comment: "Notification window preset label")
                            case .midday: NSLocalizedString("Midday", comment: "Notification window preset label")
                            case .evening: NSLocalizedString("Evening", comment: "Notification window preset label")
                            default: ""
                            }
                            let description = if let startTimeFormatted, let endTimeFormatted {
                                NSLocalizedString(
                                    "\(startTimeFormatted)—\(endTimeFormatted)",
                                    comment: "time formatting, ex: 10 AM—2 PM"
                                )
                            } else {
                                ""
                            }

                            PresetButton(
                                isSelected: isSelected,
                                onSelect: {
                                    onSelect(preset)
                                },
                                label: label,
                                description: {
                                    Text(description).font(Typography.footnote).foregroundStyle(Color.text.opacity(0.6))
                                },
                                centerContent: true
                            )
                            .frame(maxWidth: .infinity)
                        }
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
                    ),
                    description: { Image(.tabIconMore).tint(Color.text.opacity(0.6)) },
                    centerContent: false
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

private struct PresetButton<Description: View>: View {
    @ObserveInjection var inject
    let isSelected: Bool
    let onSelect: () -> Void
    let label: String
    @ViewBuilder let description: Description
    let centerContent: Bool

    var body: some View {
        Button(action: onSelect) {
            if centerContent {
                VStack(alignment: .center, spacing: 0) {
                    Text(label)
                        .font(Typography.bodySemibold)
                    presetWindowTrailingContent
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding(.horizontal, 8)
                .padding(.vertical, 10)
            } else {
                HStack(spacing: 0) {
                    Text(label)
                        .font(Typography.bodySemibold)
                    Spacer()
                    presetWindowTrailingContent
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
            }
        }
        .foregroundStyle(isSelected ? Color.fill3 : Color.text.opacity(0.6))
        .background(isSelected ? Color.key : Color.fill3)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    @ViewBuilder
    private var presetWindowTrailingContent: some View {
        HStack(alignment: .center) {
            if isSelected {
                Image(.faBellFilled)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 24, height: 24)
            } else {
                description
            }
        }
        .frame(minHeight: 24)
    }
}
