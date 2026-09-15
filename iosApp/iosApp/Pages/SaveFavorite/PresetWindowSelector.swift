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
                        let startTimeFormatted = DateComponents.fromLocalTime(preset.startTime).formatHour()
                        let endTimeFormatted = DateComponents.fromLocalTime(preset.endTime).formatHour()

                        let label = switch preset {
                        case .morning: NSLocalizedString("Morning", comment: "Notification window preset label")
                        case .midday: NSLocalizedString("Midday", comment: "Notification window preset label")
                        case .evening: NSLocalizedString("Evening", comment: "Notification window preset label")
                        case .allDay: NSLocalizedString("All day", comment: "Notification window preset label")
                        }
                        let description = switch preset {
                        case .allDay:
                            NSLocalizedString(
                                "start of service",
                                comment: "Description for the all-day notification preset"
                            )
                        default:
                            if let startTimeFormatted, let endTimeFormatted {
                                NSLocalizedString(
                                    "\(startTimeFormatted) - \(endTimeFormatted)",
                                    comment: "time formatting, ex: 10 AM - 2 PM"
                                )
                            } else {
                                ""
                            }
                        }
                        PresetButton(
                            isSelected: isSelected,
                            onSelect: {
                                onSelect(preset)
                            },
                            label: label,
                            description: description,
                            centerContent: preset != .allDay
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
                    ),
                    description: "…",
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

private struct PresetButton: View {
    @ObserveInjection var inject
    let isSelected: Bool
    let onSelect: () -> Void
    let label: String
    let description: String
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
                .padding(.horizontal, 16)
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
                Text(description)
                    .font(Typography.footnote)
            }
        }
        .frame(height: 24)
    }
}

private extension DateComponents {
    func formatHour() -> String? {
        guard let date = Calendar.current.date(from: self) else { return nil }

        return date.formatted(.dateTime.hour(.defaultDigits(amPM: .abbreviated)))
    }
}
