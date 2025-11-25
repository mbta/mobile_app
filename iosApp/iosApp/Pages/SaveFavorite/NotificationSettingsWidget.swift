//
//  NotificationSettingsWidget.swift
//  iosApp
//
//  Created by Melody Horn on 11/21/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct NotificationSettingsWidget: View {
    // unfortunately, we can’t use bindings directly
    let settings: FavoriteSettings.Notifications
    let setSettings: (FavoriteSettings.Notifications) -> Void

    var body: some View {
        VStack(spacing: 8) {
            Toggle(
                isOn: .init(
                    get: { settings.enabled },
                    set: { setSettings(.init(
                        enabled: $0,
                        windows: settings.windows.isEmpty ? [Self.defaultWindow()] : settings.windows
                    )) }
                )
            ) {
                HStack {
                    if settings.enabled {
                        Image(.faBellFilled)
                            .foregroundStyle(Color.key)
                    } else {
                        Image(.faBell)
                    }
                    Text("Get disruption notifications")
                }
            }
            .tint(Color.key)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(Color.fill3)
            .withRoundedBorder()
            if settings.enabled {
                ForEach(Array(settings.windows.enumerated()), id: \.offset) { index, window in
                    WindowWidget(
                        window: window, setWindow: { newWindow in
                            var newWindows = settings.windows
                            newWindows[index] = newWindow
                            setSettings(.init(enabled: settings.enabled, windows: newWindows))
                        }, deleteWindow: settings.windows.count > 1 ? {
                            var newWindows = settings.windows
                            newWindows.remove(at: index)
                            setSettings(.init(enabled: settings.enabled, windows: newWindows))
                        } : nil
                    )
                }
                Button(action: { setSettings(.init(
                    enabled: settings.enabled,
                    windows: settings.windows + [Self.defaultWindow(existingWindows: settings.windows)]
                )) }) {
                    HStack(spacing: 12) {
                        Image(.plus)
                            .resizable()
                            .padding(4)
                            .background(Color.text.opacity(0.6), in: .circle)
                            .foregroundStyle(Color.fill3)
                            .frame(width: 24, height: 24)
                        Text("Add another time period")
                        Spacer()
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Color.fill3)
                .withRoundedBorder()
                .foregroundStyle(Color.text.opacity(0.6))
            }
        }
    }

    static func defaultWindow(existingWindows: [FavoriteSettings.NotificationsWindow] = []) -> FavoriteSettings
        .NotificationsWindow {
        if existingWindows.isEmpty {
            .init(
                startTime: .init(hour: 8, minute: 0, second: 0, nanosecond: 0),
                endTime: .init(hour: 9, minute: 0, second: 0, nanosecond: 0),
                daysOfWeek: [.monday, .tuesday, .wednesday, .thursday, .friday]
            )
        } else {
            .init(
                startTime: .init(hour: 12, minute: 0, second: 0, nanosecond: 0),
                endTime: .init(hour: 13, minute: 0, second: 0, nanosecond: 0),
                daysOfWeek: [.saturday, .sunday]
            )
        }
    }

    struct WindowWidget: View {
        let window: FavoriteSettings.NotificationsWindow
        let setWindow: (FavoriteSettings.NotificationsWindow) -> Void
        let deleteWindow: (() -> Void)?

        var body: some View {
            HStack(spacing: 0) {
                if let deleteWindow {
                    Button(action: deleteWindow) {
                        Image(.trashCan).accessibilityLabel(Text("Delete"))
                    }
                    .foregroundStyle(Color.error)
                    .frame(minWidth: 44)
                }
                VStack(spacing: 0) {
                    LabeledTimeInput(
                        label: Text("From"),
                        time: window.startTime,
                        setTime: { setWindow(.init(
                            startTime: $0,
                            endTime: window.endTime,
                            daysOfWeek: window.daysOfWeek
                        )) }
                    )
                    HaloSeparator()
                    LabeledTimeInput(
                        label: Text("To"),
                        time: window.endTime,
                        setTime: { setWindow(.init(
                            startTime: window.startTime,
                            endTime: $0,
                            daysOfWeek: window.daysOfWeek
                        )) }
                    )
                    DaysOfWeekInput(
                        daysOfWeek: window.daysOfWeek,
                        setDaysOfWeek: { setWindow(.init(
                            startTime: window.startTime,
                            endTime: window.endTime,
                            daysOfWeek: $0
                        )) }
                    )
                }
                .background(Color.fill3)
                .clipShape(RoundedRectangle(cornerRadius: 7))
                .padding(1)
            }
            .background(Color.halo)
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
    }

    struct LabeledTimeInput: View {
        let label: Text
        let time: Kotlinx_datetimeLocalTime
        let setTime: (Kotlinx_datetimeLocalTime) -> Void

        var body: some View {
            DatePicker(selection: .init(get: {
                Calendar(identifier: .iso8601).nextDate(
                    after: .now,
                    matching: .init(hour: Int(time.hour), minute: Int(time.minute), second: Int(time.second)),
                    matchingPolicy: .strict
                ) ?? .now
            }, set: {
                let components = Calendar(identifier: .iso8601).dateComponents([.hour, .minute, .second], from: $0)
                if let hour = components.hour, let minute = components.minute, let second = components.second {
                    setTime(.init(hour: Int32(hour), minute: Int32(minute), second: Int32(second), nanosecond: 0))
                }
            }), displayedComponents: [.hourAndMinute]) {
                label
            }
            .datePickerStyle(.compact)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }
    }

    struct DaysOfWeekInput: View {
        let daysOfWeek: Set<Kotlinx_datetimeDayOfWeek>
        let setDaysOfWeek: (Set<Kotlinx_datetimeDayOfWeek>) -> Void

        static var days: [Kotlinx_datetimeDayOfWeek] {
            [.sunday, .monday, .tuesday, .wednesday, .thursday, .friday, .saturday]
        }

        static var calendar: Calendar {
            var result = Calendar(identifier: .iso8601)
            result.locale = .autoupdatingCurrent
            return result
        }

        var body: some View {
            let calendar = Self.calendar
            HStack(alignment: .top, spacing: 2) {
                ForEach(Self.days, id: \.ordinal) { day in
                    let isIncluded = daysOfWeek.contains(day)
                    VStack(spacing: 0) {
                        Text(calendar.shortStandaloneWeekdaySymbols[Int(day.ordinal + 1) % 7])
                            .lineLimit(1)
                            .font(Typography.footnoteSemibold)
                        if isIncluded {
                            Image(.faCheck)
                        } else {
                            Image(.faCheck).hidden()
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, 8)
                    .onTapGesture {
                        setDaysOfWeek(daysOfWeek.symmetricDifference([day]))
                    }
                    .background(isIncluded ? Color.key : Color.fill1)
                    .clipShape(RoundedRectangle(cornerRadius: 6))
                    .foregroundStyle(isIncluded ? Color.fill3 : Color.text.opacity(0.6))
                    .accessibilityElement(children: .ignore)
                    .accessibilityChildren {
                        // .accessibilityAddTraits(.isToggle) is iOS 17+ only, so we use a real toggle
                        // labelled with the full name of the day
                        Toggle(
                            isOn: .init(get: { isIncluded }, set: { _ in
                                setDaysOfWeek(daysOfWeek.symmetricDifference([day]))
                            }),
                            label: {
                                Text(calendar.standaloneWeekdaySymbols[Int(day.ordinal + 1) % 7])
                            }
                        )
                    }
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }
    }
}

struct NotificationSettingsWidget_Previews: PreviewProvider {
    struct Holder: View {
        @State var settings = FavoriteSettings.Notifications(
            enabled: true,
            windows: [
                NotificationSettingsWidget.defaultWindow(),
                NotificationSettingsWidget.defaultWindow(existingWindows: [NotificationSettingsWidget.defaultWindow()]),
            ]
        )

        var body: some View {
            NotificationSettingsWidget(settings: settings, setSettings: { settings = $0 })
        }
    }

    static var previews: some View {
        Holder()
            .padding(.horizontal, 16)
            .padding(.vertical, 24)
            .background(Color.fill2)
    }
}
