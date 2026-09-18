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
    @ObserveInjection var inject
    let vm: INotificationSettingsViewModel
    let onUpdate: (FavoriteSettings.Notifications) -> Void

    var notificationPermissionManager: INotificationPermissionManager
    var authorizationStatus: UNAuthorizationStatus? { notificationPermissionManager.authorizationStatus }
    var now: EasternTimeInstant = .now()

    @State var vmState: NotificationSettingsViewModel.State?

    let inspection = Inspection<Self>()

    var body: some View {
        VStack(spacing: 0) {
            if let vmState {
                NotificationSettingsWidgetPresetnationView(state: vmState,
                                                           setEnabled: { enabled in vm.setEnabled(enabled: enabled) },
                                                           setPreset: { preset in vm.setPreset(preset: preset) },
                                                           setCustomWindows: { custom in
                                                               vm.setCustomWindows(windows: custom)
                                                           },
                                                           addPlaceholderWindow: { vm.addPlaceholderWindow() },
                                                           notificationPermissionManager: notificationPermissionManager)
            }
        }.manageVM(vm, $vmState, now)
            .onChange(of: vmState) { newState in
                if let newState, let settings = newState.settings {
                    onUpdate(settings)
                }
            }
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
            .enableInjection()
    }
}

struct NotificationSettingsWidgetPresetnationView: View {
    let state: NotificationSettingsViewModel.State
    var setEnabled: (Bool) -> Void = { _ in }
    var setPreset: (Preset?) -> Void = { _ in }
    var setCustomWindows: ([FavoriteSettings.NotificationsWindow]) -> Void = { _ in }
    var addPlaceholderWindow: () -> Void = {}

    var notificationPermissionManager: INotificationPermissionManager
    var authorizationStatus: UNAuthorizationStatus? { notificationPermissionManager.authorizationStatus }
    var now: EasternTimeInstant = .now()

    @EnvironmentObject var settingsCache: SettingsCache
    var presetWindowsEnabled: Bool { settingsCache.get(.notificationPresetWindows) }

    var daysOfWeek: Set<Kotlinx_datetimeDayOfWeek> {
        FavoriteSettings.NotificationsWindow.companion.defaultDaysOfWeek(now: now)
    }

    let presetOptions: [[Preset]] = [
        [.morning, .midday, .evening],
        [.allDay]
    ]

    let inspection = Inspection<Self>()

    var body: some View {
        VStack(spacing: 0) {
            if let settings = state.settings {
                VStack(spacing: 8) {
                    NotificationSwitch(
                        settings: settings,
                        onValueChanged: { setEnabled($0) },
                        notificationPermissionManager: notificationPermissionManager
                    )

                    if settings.enabled {
                        Text("When do you want notifications?")
                            .foregroundColor(Color.deemphasized)
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.vertical, 8)

                        VStack(spacing: 16) {
                            if presetWindowsEnabled {
                                PresetWindowSelector(
                                    presetRows: presetOptions,
                                    selectedPreset: state.selectedPreset,
                                    onSelect: { preset in
                                        setPreset(preset)
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
                                        setCustomWindows(newWindows)
                                    },
                                    deleteWindow: settings.windows.count > 1 ? {
                                        let nextWindows = settings.windows.filter { $0.id != window.id }
                                        setCustomWindows(nextWindows)
                                    } : nil
                                )
                            }

                            Button(action: {
                                addPlaceholderWindow()
                            }) {
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
                            .padding(.bottom, 16)
                            .withRoundedBorder(color: .clear)
                            .foregroundStyle(Color.text.opacity(0.6))
                        }
                        .padding(4)
                        .background(Color.fill1)
                        .withRoundedBorder(color: .clear)
                    }
                }
            }
        }
        .onAppear { UIDatePicker.appearance().minuteInterval = 15 }
        .onDisappear { UIDatePicker.appearance().minuteInterval = 1 }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .enableInjection()
    }
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
            VStack(spacing: 16) {
                VStack(spacing: 0) {
                    HStack(spacing: 8) {
                        TimeInput(
                            label: Text("Select start time"),
                            time: DateComponents.fromLocalTime(window.startTime),
                            type: window.startType,
                            clampTime: nil,
                            setTime: { time in
                                let startTime = time.toLocalTime()
                                setWindow(window.doCopy(
                                    startTime: startTime,
                                    endTime: FavoriteSettings.NotificationsWindow.companion
                                        .safeEndTime(startTime: startTime, endTime: window.endTime),
                                ))
                            },
                        )
                        Text("to")
                        TimeInput(
                            label: Text("Select end time"),
                            time: DateComponents.fromLocalTime(window.endTime),
                            type: window.endType,
                            clampTime: { time, roundUp in
                                Date.fromLocalTime(FavoriteSettings.NotificationsWindow.companion
                                    .safeEndTime(
                                        startTime: window.startTime,
                                        endTime: time.toLocalTime(),
                                        roundUp: roundUp
                                    ))
                            },
                            setTime: { time in setWindow(window.doCopy(endTime: time.toLocalTime())) },
                        )
                    }
                    HStack(spacing: 0) {
                        TimeNote(type: window.startType)
                        Spacer()
                        TimeNote(type: window.endType)
                    }.frame(maxWidth: .infinity)
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
            .padding(12)
            .background(Color.fill3)
            .clipShape(RoundedRectangle(cornerRadius: 7))
            .padding(1)
        }
        .background(Color.halo)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .enableInjection()
    }
}

enum TimeIconPosition {
    case before
    case after
}

private func timeNoteText(_ type: FavoriteSettings.NotificationsWindowType) -> Text? {
    switch onEnum(of: type) {
    case .nextDay: Text("next day")
    case .serviceEnd: Text("end of service")
    case .serviceStart: Text("start of service")
    default: nil
    }
}

struct TimeIcon: View {
    let type: FavoriteSettings.NotificationsWindowType
    let position: TimeIconPosition

    var icon: ImageResource? {
        switch onEnum(of: type) {
        case .serviceEnd, .nextDay: position == .after ? .serviceEndMoon : nil
        case .serviceStart: position == .before ? .serviceStartSun : nil
        default: nil
        }
    }

    var body: some View {
        if let icon {
            Image(icon)
                .resizable()
                .scaledToFit()
                .frame(width: 24, height: 24)
                .foregroundStyle(Color.deemphasized)
                .accessibilityHidden(true)
        }
    }
}

struct TimeNote: View {
    let type: FavoriteSettings.NotificationsWindowType

    var body: some View {
        if let text = timeNoteText(type) {
            text
                .accessibilityHidden(true)
                .font(Typography.footnote)
                .foregroundStyle(Color.deemphasized)
                .padding(.horizontal, 8)
        }
    }
}

struct TimeInput: View {
    @ObserveInjection var inject
    let label: Text
    let time: DateComponents
    let type: FavoriteSettings.NotificationsWindowType
    let clampTime: ((Date, Bool) -> Date)?
    let setTime: (DateComponents) -> Void

    @State var roundUp: Bool = true

    private var dateBinding: Binding<Date> {
        Binding(
            get: { time.nextDate },
            set: { newDate in
                var setDate = newDate
                var components = time
                if let clampTime {
                    setDate = clampTime(newDate, roundUp)
                    roundUp = if setDate == newDate { true } else { !roundUp }
                }
                components.nextDate = setDate
                setTime(components)
            }
        )
    }

    var alignment: Alignment {
        switch onEnum(of: type) {
        case .basic: .center
        case .nextDay, .serviceEnd: .trailing
        case .serviceStart: .leading
        }
    }

    var timeString: String { dateBinding.wrappedValue.formatted(date: .omitted, time: .shortened) }
    var timeNote: Text? { timeNoteText(type) }

    var body: some View {
        HStack(spacing: 10) {
            TimeIcon(type: type, position: .before)
            Text(timeString)
                .font(Typography.bodySemibold)
                .foregroundStyle(Color.text)
                .accessibilityHidden(true)
            TimeIcon(type: type, position: .after)
        }
        .padding(.horizontal, 8)
        .frame(maxWidth: .infinity, minHeight: 40, alignment: alignment)
        .overlay {
            // This is a hack to get around the default date picker styling,
            // the time is displayed with an invisible date picker on top of it.
            DatePicker(selection: dateBinding, displayedComponents: [.hourAndMinute]) { label }
                .labelsHidden()
                .datePickerStyle(.compact)
                .compositingGroup()
                .scaleEffect(x: 2, y: 1.6)
                .colorMultiply(.clear)
        }
        .background(RoundedRectangle(cornerRadius: 6).fill(Color.fill1))
        .contentShape(Rectangle())
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
        .enableInjection()
    }
}

struct NotificationSwitch: View {
    let settings: FavoriteSettings.Notifications
    let onValueChanged: (Bool) -> Void
    let notificationPermissionManager: INotificationPermissionManager

    let inspection = Inspection<Self>()

    @State var authorizationStatus: UNAuthorizationStatus?

    var body: some View {
        let enabledBinding = Binding<Bool>(
            get: {
                settings.enabled
            },
            set: { newValue in
                onValueChanged(newValue)
            }
        )

        let permissionDenied = authorizationStatus == .denied
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
                        enabledBinding.wrappedValue = false
                        return
                    }
                }
            }
        }
        .onAppear {
            authorizationStatus = notificationPermissionManager.authorizationStatus
        }
        .onChange(of: notificationPermissionManager.authorizationStatus) { newStatus in
            authorizationStatus = newStatus
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }
}

struct NotificationSettingsWidget_Previews: PreviewProvider {
    struct Holder: View {
        @ObserveInjection var inject
        let windows = [FavoriteSettings.NotificationsWindow.companion.default(
            existingWindows: [],
            presetsEnabled: false,
            now: EasternTimeInstant.now()
        )]

        var vm = NotificationSettingsViewModel(sentryRepository: MockSentryRepository())

        var body: some View {
            NotificationSettingsWidget(
                vm: vm,
                onUpdate: { _ in },
                notificationPermissionManager: MockNotificationPermissionManager()
            ).onAppear {
                vm.setEnabled(enabled: true)
                vm.setCustomWindows(windows: windows)
            }
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

extension Kotlinx_datetimeLocalTime: @retroactive Comparable {
    public static func < (lhs: Kotlinx_datetimeLocalTime, rhs: Kotlinx_datetimeLocalTime) -> Bool {
        // Call the bridged Kotlin compareTo method
        lhs.compareTo(other: rhs) < 0
    }

    public static func == (lhs: Kotlinx_datetimeLocalTime, rhs: Kotlinx_datetimeLocalTime) -> Bool {
        lhs.compareTo(other: rhs) == 0
    }
}
