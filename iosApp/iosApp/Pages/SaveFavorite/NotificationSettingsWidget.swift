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
            let result = calendar.nextDate(after: beforeDayStart, matching: self, matchingPolicy: .strict)!
            return result
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
    @ObservedObject var settings: MutableFavoriteSettings.Notifications

    var notificationPermissionManager: INotificationPermissionManager
    var authorizationStatus: UNAuthorizationStatus? { notificationPermissionManager.authorizationStatus }

    var body: some View {
        VStack(spacing: 8) {
            VStack(spacing: 16) {
                Toggle(isOn: $settings.enabled) {
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
                .disabled(authorizationStatus == .denied)
                .tint(Color.key)
                if authorizationStatus == .denied {
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
            .onChange(of: settings.enabled) { enabled in
                Task {
                    if enabled {
                        let notificationPermission = await notificationPermissionManager.requestPermission()
                        guard notificationPermission else {
                            settings.enabled = false
                            return
                        }
                        if settings.windows.count == 0 {
                            settings.windows = [Self.defaultWindow()]
                        }
                    }
                }
            }

            if settings.enabled {
                ForEach(settings.windows) { window in
                    WindowWidget(
                        window: window, deleteWindow: settings.windows.count > 1 ? {
                            settings.windows.removeAll(where: { $0.id == window.id })
                        } : nil
                    )
                }
                Button(action: { settings.windows += [Self.defaultWindow(existingWindows: settings.windows)] }) {
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

    static func defaultWindow(existingWindows: [MutableFavoriteSettings.Notifications.Window] = [])
        -> MutableFavoriteSettings
        .Notifications.Window {
        if existingWindows.isEmpty {
            .init(
                startTime: .init(hour: 8, minute: 0, second: 0),
                endTime: .init(hour: 9, minute: 0, second: 0),
                daysOfWeek: [.monday, .tuesday, .wednesday, .thursday, .friday]
            )
        } else {
            .init(
                startTime: .init(hour: 12, minute: 0, second: 0),
                endTime: .init(hour: 13, minute: 0, second: 0),
                daysOfWeek: [.saturday, .sunday]
            )
        }
    }

    struct WindowWidget: View {
        @ObservedObject var window: MutableFavoriteSettings.Notifications.Window
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
                VStack(spacing: 0) {
                    LabeledTimeInput(
                        label: Text("From"),
                        time: $window.startTime,
                        minimumTime: nil
                    )
                    .onChange(of: window.startTime) { startTime in
                        if startTime.hour! > window.endTime.hour! ||
                            (startTime.hour! == window.endTime.hour! && startTime.minute! > window.endTime.minute!) {
                            if startTime.hour! < 23 {
                                window.endTime = .init(hour: startTime.hour! + 1, minute: startTime.minute!, second: 0)
                            } else {
                                window.endTime = .init(hour: 23, minute: 59, second: 0)
                            }
                        }
                    }
                    HaloSeparator()
                    LabeledTimeInput(
                        label: Text("To"),
                        time: $window.endTime,
                        minimumTime: window.startTime
                    )
                    DaysOfWeekInput(daysOfWeek: $window.daysOfWeek)
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
        @Binding var time: DateComponents
        let minimumTime: DateComponents?

        init(
            label: Text,
            time: Binding<DateComponents>,
            minimumTime: DateComponents? = nil
        ) {
            self.label = label
            _time = time
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
            DatePicker(selection: $time.nextDate, in: dateRange, displayedComponents: [.hourAndMinute]) {
                label
            }
            .datePickerStyle(.compact)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }
    }

    struct DaysOfWeekInput: View {
        @Binding var daysOfWeek: Set<Kotlinx_datetimeDayOfWeek>

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
                        daysOfWeek.formSymmetricDifference([day])
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
                                daysOfWeek.formSymmetricDifference([day])
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
        }
    }
}

struct NotificationSettingsWidget_Previews: PreviewProvider {
    struct Holder: View {
        @State var settings = MutableFavoriteSettings.Notifications(
            enabled: true,
            windows: [
                NotificationSettingsWidget.defaultWindow(),
                NotificationSettingsWidget.defaultWindow(existingWindows: [NotificationSettingsWidget.defaultWindow()]),
            ]
        )

        var body: some View {
            NotificationSettingsWidget(
                settings: settings,
                notificationPermissionManager: MockNotificationPermissionManager()
            )
        }
    }

    static var previews: some View {
        Holder()
            .padding(.horizontal, 16)
            .padding(.vertical, 24)
            .background(Color.fill2)
    }
}
