//
//  NotificationSettingsWidget.swift
//  iosApp
//
//  Created by Melody Horn on 11/21/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

private extension DateComponents {
    var nextDate: Date {
        get {
            let calendar = Calendar(identifier: .iso8601)
            let beforeDayStart = calendar.startOfDay(for: .now).addingTimeInterval(-0.01)
            return calendar.nextDate(after: beforeDayStart, matching: self, matchingPolicy: .strict)!
        }
        set {
            // in this file, we only use hour/minute/second
            let components: Set<Calendar.Component> = [.hour, .minute, .second]
            let calendar = Calendar(identifier: .iso8601)
            self = calendar.dateComponents(components, from: newValue)
        }
    }
}

struct NotificationSettingsWidget: View {
    @ObserveInjection var inject
    let settings: FavoriteSettings.Notifications
    let setSettings: (FavoriteSettings.Notifications) -> Void

    var notificationPermissionManager: INotificationPermissionManager
    var authorizationStatus: UNAuthorizationStatus? { notificationPermissionManager.authorizationStatus }
    var now: EasternTimeInstant = .now()

    @EnvironmentObject var settingsCache: SettingsCache
    var presetWindowsEnabled: Bool { settingsCache.get(.notificationPresetWindows) }

    let presetOptions: [[PresetWindow]] = [
        [
            .init(
                label: NSLocalizedString("Morning", comment: "Notification window preset label"),
                window: FavoriteSettings.NotificationsWindow.companion.morningDefault
            ),
            .init(
                label: NSLocalizedString("Midday", comment: "Notification window preset label"),
                window: FavoriteSettings.NotificationsWindow.companion.middayDefault
            ),
            .init(
                label: NSLocalizedString("Evening", comment: "Notification window preset label"),
                window: FavoriteSettings.NotificationsWindow.companion.eveningDefault
            )
        ],
        [
            .init(
                label: NSLocalizedString("All day", comment: "Notification window preset label"),
                window: FavoriteSettings.NotificationsWindow.companion.allDayDefault
            )
        ]
    ]

    var presetSelection: PresetSelection {
        PresetSelection.companion.selectedPresetFromSettings(
            settings: settings,
            presetOptions: presetOptions
        )
    }

    var body: some View {
        let enabledBinding = Binding<Bool>(
            get: {
                settings.enabled
            },
            set: { newValue in
                setSettings(settings.doCopy(enabled: newValue, windows: settings.windows))
            }
        )

        let permissionDenied = authorizationStatus == .denied
        VStack(spacing: 8) {
            VStack(spacing: 16) {
                Toggle(isOn: enabledBinding) {
                    HStack {
                        if settings.enabled {
                            Image(.faBellFilled)
                                .resizable()
                                .scaledToFit()
                                .frame(width: 24, height: 24)
                                .foregroundStyle(Color.key)
                        } else {
                            Image(.faBell)
                                .resizable()
                                .scaledToFit()
                                .frame(width: 24, height: 24)
                        }
                        Text("Get disruption notifications")
                    }
                }
                .disabled(permissionDenied)
                .opacity(permissionDenied ? 0.6 : 1.0)
                .tint(Color.key)
                if permissionDenied {
                    Button {
                        notificationPermissionManager.openNotificationSettings()
                    } label: {
                        HStack {
                            Text(
                                "Allow Notifications in Settings",
                                comment: "Label for a link to the app's notification permission settings"
                            ).font(.body)
                            Spacer()
                            Image(systemName: "arrow.up.right")
                                .resizable()
                                .frame(width: 10.5, height: 10.5, alignment: .center)
                                .fontWeight(.bold)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 2)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(Color.fill3)
            .withRoundedBorder()
            .onTapGesture {
                if permissionDenied { notificationPermissionManager.openNotificationSettings() }
            }
            .onChange(of: settings.enabled) { enabled in
                Task {
                    if enabled {
                        let notificationPermission = await notificationPermissionManager.requestPermission()
                        guard notificationPermission else {
                            setSettings(FavoriteSettings.Notifications.companion.disabled)
                            return
                        }
                        if settings.windows.count == 0 {
                            setSettings(settings.doCopy(
                                enabled: enabled,
                                windows: [FavoriteSettings.NotificationsWindow.companion.default(
                                    existingWindows: [],
                                    presetsEnabled: presetWindowsEnabled,
                                    now: now
                                )]
                            ))
                        }
                    }
                }
            }

            if settings.enabled {
                if presetWindowsEnabled {
                    PresetWindowSelector(
                        presetRows: presetOptions,
                        selectedPreset: presetSelection,
                        presetsEnabled: settings.windows.count <= 1,
                        now: now,
                        onSelect: { selectedWindow in
                            let otherWindows = settings.windows.dropFirst()
                            setSettings(settings.doCopy(
                                enabled: settings.enabled,
                                windows: [selectedWindow] + otherWindows
                            ))
                        }
                    )
                }

                ForEach(settings.windows, id: \.id) { window in
                    WindowWidget(
                        window: window,
                        setWindow: { newWindow in
                            let windowIndex = settings.windows.firstIndex(of: window)
                            var newWindows = settings.windows
                            if let windowIndex {
                                newWindows[windowIndex] = newWindow
                            }
                            setSettings(settings.doCopy(enabled: settings.enabled, windows: newWindows))
                        },
                        deleteWindow: { if settings.windows.count > 1 {
                            setSettings(
                                settings.doCopy(
                                    enabled: settings.enabled,
                                    windows: settings.windows.filter { $0.id != window.id }
                                )
                            )
                        }
                        }
                    )
                }

                let customWindow =
                    if presetWindowsEnabled {
                        FavoriteSettings.NotificationsWindow.companion.customFromCurrentTime(now: now)
                    } else {
                        FavoriteSettings.NotificationsWindow.companion.default(
                            existingWindows: settings.windows,
                            presetsEnabled: presetWindowsEnabled,
                            now: now
                        )
                    }

                Button(action: { setSettings(settings.doCopy(
                    enabled: settings.enabled,
                    windows: settings.windows + [customWindow]
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
        .enableInjection()
    }

    struct WindowWidget: View {
        @ObserveInjection var inject
        let window: FavoriteSettings.NotificationsWindow
        let setWindow: (FavoriteSettings.NotificationsWindow) -> Void
        let deleteWindow: (() -> Void)?

        var body: some View {
            HStack(spacing: 0) {
                if let deleteWindow {
                    Button(action: deleteWindow) {
                        Image(.faDelete).accessibilityLabel(Text("Delete"))
                    }
                    .foregroundStyle(Color.error)
                    .frame(minWidth: 44)
                }
                VStack {
                    HStack(spacing: 0) {
                        TimeInput(
                            label: Text("Select start time"),
                            time: DateComponents.fromLocalTime(window.startTime),
                            setTime: { time in
                                let startTime = time.toLocalTime()
                                setWindow(window.doCopy(
                                    startTime: startTime,
                                    endTime: FavoriteSettings.NotificationsWindow.companion
                                        .safeEndTime(startTime: startTime, endTime: window.endTime),
                                    daysOfWeek: window.daysOfWeek
                                ))
                            },
                            minimumTime: nil
                        ).frame(maxWidth: .infinity)
                        Text("to")
                        TimeInput(
                            label: Text("Select end time"),
                            time: DateComponents.fromLocalTime(window.endTime),
                            setTime: { time in setWindow(window.doCopy(
                                startTime: window.startTime,
                                endTime: time.toLocalTime(),
                                daysOfWeek: window.daysOfWeek
                            )) },
                            minimumTime: DateComponents
                                .fromLocalTime(FavoriteSettings.NotificationsWindow.companion
                                    .minimumEndTime(startTime: window.startTime))
                        ).frame(maxWidth: .infinity)
                    }
                    DaysOfWeekInput(
                        daysOfWeek: window.daysOfWeek,
                        setDaysOfWeek: { newDays in setWindow(window.doCopy(
                            startTime: window.startTime,
                            endTime: window.endTime,
                            daysOfWeek: newDays
                        )) }
                    )
                }
                .background(Color.fill3)
                .clipShape(RoundedRectangle(cornerRadius: 7))
                .padding(1)
            }
            .background(Color.halo)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .enableInjection()
        }
    }

    struct TimeInput: View {
        @ObserveInjection var inject
        let label: Text
        let time: DateComponents
        let setTime: (DateComponents) -> Void
        let minimumTime: DateComponents?

        init(
            label: Text,
            time: DateComponents,
            setTime: @escaping (DateComponents) -> Void,
            minimumTime: DateComponents? = nil
        ) {
            self.label = label
            self.time = time
            self.setTime = setTime
            self.minimumTime = minimumTime
        }

        var dateRange: ClosedRange<Date> {
            let calendar = Calendar(identifier: .iso8601)
            let beforeDayStart = calendar.startOfDay(for: .now).addingTimeInterval(-0.01)
            let minimum: DateComponents = minimumTime ?? .init(hour: 0, minute: 0, second: 0)
            let start = calendar.nextDate(
                after: beforeDayStart,
                matching: minimum,
                matchingPolicy: .strict
            )!
            let end = calendar.nextDate(
                after: start,
                matching: .init(hour: 23, minute: 59, second: 59),
                matchingPolicy: .strict
            )!
            return start ... end
        }

        var body: some View {
            let timeBinding = Binding<DateComponents>(
                get: {
                    time
                },
                set: { newValue in
                    setTime(newValue)
                }
            )

            DatePicker(selection: timeBinding.nextDate, in: dateRange, displayedComponents: [.hourAndMinute]) { label }
                .labelsHidden()
                .datePickerStyle(.compact)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .enableInjection()
        }
    }

    struct DaysOfWeekInput: View {
        @ObserveInjection var inject
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
                        Text(calendar.shortStandaloneWeekdaySymbols[day.indexSundayFirst])
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
                                Text(calendar.standaloneWeekdaySymbols[day.indexSundayFirst])
                            }
                        )
                    }
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .enableInjection()
        }
    }
}

struct NotificationSettingsWidget_Previews: PreviewProvider {
    struct Holder: View {
        @ObserveInjection var inject
        @State var settings: FavoriteSettings = .init(notifications: .init(
            enabled: true,
            windows: [FavoriteSettings.NotificationsWindow.companion.default(
                existingWindows: [],
                presetsEnabled: false,
                now: EasternTimeInstant.now()
            )]
        ))

        var body: some View {
            NotificationSettingsWidget(
                settings: settings.notifications,
                setSettings: { updatedSettings in
                    settings = settings.doCopy(notifications: updatedSettings)
                },
                notificationPermissionManager: MockNotificationPermissionManager()
            )
            .enableInjection()
        }
    }

    static var previews: some View {
        Holder()
            .padding(.horizontal, 16)
            .padding(.vertical, 24)
            .background(Color.fill2)
    }
}

extension DateComponents {
    static func fromLocalTime(_ localTime: Kotlinx_datetimeLocalTime) -> Self {
        .init(
            hour: Int(localTime.hour),
            minute: Int(localTime.minute),
            second: Int(localTime.second)
        )
    }

    func toLocalTime() -> Kotlinx_datetimeLocalTime {
        .init(
            hour: Int32(hour ?? 0),
            minute: Int32(minute ?? 0),
            second: Int32(second ?? 0),
            nanosecond: Int32(nanosecond ?? 0)
        )
    }
}

extension Kotlinx_datetimeLocalTime: @retroactive Comparable {
    public static func < (lhs: Kotlinx_datetimeLocalTime, rhs: Kotlinx_datetimeLocalTime) -> Bool {
        // Call the bridged Kotlin compareTo method
        lhs.compareTo(other: rhs) < 0
    }

    public static func == (lhs: Kotlinx_datetimeLocalTime, rhs: Kotlinx_datetimeLocalTime) -> Bool {
        lhs.compareTo(other: rhs) == 0
    }
}
