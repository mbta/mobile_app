//
//  NotificationSettingsWidgetTests.swift
//  iosAppTests
//
//  Created by Melody Horn on 11/24/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class NotificationSettingsWidgetTests: XCTestCase {
    private func assertEqualWindows(
        expected: [FavoriteSettings.NotificationsWindow],
        actual: [FavoriteSettings.NotificationsWindow],
    ) {
        XCTAssertEqual(expected.count, actual.count)
        for (expectedWindow, actualWindow) in zip(expected, actual) {
            XCTAssertEqual(expectedWindow.startTime, actualWindow.startTime)
            XCTAssertEqual(expectedWindow.endTime, actualWindow.endTime)
            XCTAssertEqual(expectedWindow.daysOfWeek, actualWindow.daysOfWeek)
        }
    }

    @MainActor func testEnable() throws {
        let settings: FavoriteSettings.Notifications = .companion.disabled
        var enabled = false

        let sut = NotificationSettingsWidgetPresentationView(
            state: .init(settings: settings, selectedPreset: nil),
            showPresets: true,
            setEnabled: { enabled = $0 },
            notificationPermissionManager: MockNotificationPermissionManager(),
        ).withFixedSettings([:])

        try sut.inspect().find(text: "Get disruption notifications").find(ViewType.Toggle.self, relation: .parent).tap()
        XCTAssertTrue(enabled)
    }

    func testAddSecondTimePeriod() throws {
        let firstWindow = FavoriteSettings.NotificationsWindow(
            startTime: .init(hour: 1, minute: 0, second: 0, nanosecond: 0),
            endTime: .init(hour: 2, minute: 0, second: 0, nanosecond: 0),
            daysOfWeek: [.thursday]
        )

        var addedWindow = false
        let sut = NotificationSettingsWidgetPresentationView(
            state: .init(settings: .init(enabled: true, windows: [firstWindow]), selectedPreset: nil),
            showPresets: true,
            addPlaceholderWindow: { addedWindow = true },
            notificationPermissionManager: MockNotificationPermissionManager(),
        ).withFixedSettings([:])

        // unfortunately, ViewInspector does not appear to surface the selected value of a DatePicker
        XCTAssertNotNil(try sut.inspect().find(
            ViewType.DatePicker.self,
            where: { try $0.labelView().text().string() == "Select start time" }
        ))
        XCTAssertNotNil(try sut.inspect().find(
            ViewType.DatePicker.self,
            where: { try $0.labelView().text().string() == "Select end time" }
        ))

        XCTAssertThrowsError(try sut.inspect().find(viewWithAccessibilityLabel: "Delete time period"))
        // ViewInspector as of 0.10.3 does not support accessibilityChildren so we can’t check the days of the week
        try sut.inspect().find(button: "Add another time period").tap()
        XCTAssertTrue(addedWindow)
    }

    func testDeleteButtonWhenTwoTimePeriods() throws {
        let settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [.init(preset: .morning, daysOfWeek: [.monday]),
                      .init(preset: .evening, daysOfWeek: [.monday])]
        )

        let sut = NotificationSettingsWidgetPresentationView(
            state: .init(settings: settings, selectedPreset: nil),
            showPresets: true,
            notificationPermissionManager: MockNotificationPermissionManager(),
        )
        .withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "Delete time period"))
    }

    func testChangeStartTime() throws {
        let settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [.init(preset: .morning,
                            daysOfWeek: [.monday])]
        )
        var customWindows: [FavoriteSettings.NotificationsWindow] = []

        let sut = NotificationSettingsWidgetPresentationView(
            state: .init(settings: settings, selectedPreset: nil),
            showPresets: true,
            setWindows: { customWindows = $0 },
            notificationPermissionManager: MockNotificationPermissionManager(),
        ).withFixedSettings([:])

        try sut.inspect().find(
            ViewType.DatePicker.self,
            where: { try $0.labelView().text().string() == "Select start time" }
        )
        .select(date: XCTUnwrap(Calendar(identifier: .iso8601).nextDate(
            after: .now,
            matching: .init(hour: 7, minute: 45),
            matchingPolicy: .strict
        )))

        assertEqualWindows(
            expected: [.init(
                startTime: .init(hour: 7, minute: 45, second: 0, nanosecond: 0),
                endTime: Preset.morning.endTime,
                daysOfWeek: [.monday]
            )],
            actual: customWindows,
        )
    }

    func testChangeEndTime() throws {
        let settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [.init(preset: .morning,
                            daysOfWeek: [.monday])]
        )
        var customWindows: [FavoriteSettings.NotificationsWindow] = []

        let sut = NotificationSettingsWidgetPresentationView(
            state: .init(settings: settings, selectedPreset: nil),
            showPresets: true,
            setWindows: { customWindows = $0 },
            notificationPermissionManager: MockNotificationPermissionManager(),
        )
        .withFixedSettings([:])

        try sut.inspect().find(
            ViewType.DatePicker.self,
            where: { try $0.labelView().text().string() == "Select end time" }
        )
        .select(date: XCTUnwrap(Calendar(identifier: .iso8601).nextDate(
            after: .now,
            matching: .init(hour: 13, minute: 45),
            matchingPolicy: .strict
        )))

        assertEqualWindows(expected: [.init(
            startTime: Preset.morning.startTime,
            endTime: .init(hour: 13, minute: 45, second: 0, nanosecond: 0),
            daysOfWeek: [.monday]
        )], actual: customWindows)
    }

    func testChangeDays() throws {
        let settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [.init(preset: .morning,
                            daysOfWeek: [.monday])]
        )
        var customWindows: [FavoriteSettings.NotificationsWindow] = []

        let sut = NotificationSettingsWidgetPresentationView(
            state: .init(settings: settings, selectedPreset: nil),
            showPresets: true,
            setWindows: { customWindows = $0 },
            notificationPermissionManager: MockNotificationPermissionManager(),
        ).withFixedSettings([:])

        try sut.inspect().find(text: "Sun").find(ViewType.VStack.self, relation: .parent).callOnTapGesture()
        XCTAssertEqual(customWindows[0].daysOfWeek, [.sunday, .monday])
    }

    private struct TimeChangeCase {
        let changingStart: Bool
        let start: DateComponents
        let end: DateComponents
        let expectedStart: DateComponents
        let expectedEnd: DateComponents

        static func endCase(
            start: DateComponents, end: DateComponents, expected: (DateComponents, DateComponents)
        ) -> Self {
            .init(changingStart: false, start: start, end: end, expectedStart: expected.0, expectedEnd: expected.1)
        }

        static func startCase(
            start: DateComponents, end: DateComponents, expected: (DateComponents, DateComponents)
        ) -> Self {
            .init(changingStart: true, start: start, end: end, expectedStart: expected.0, expectedEnd: expected.1)
        }
    }

    private func time(_ hour: Int, _ minute: Int = 0) -> DateComponents { .init(hour: hour, minute: minute) }

    private var timeChangeCases: [TimeChangeCase] {
        [
            // Setting end time
            .endCase(start: time(3), end: time(3), expected: (time(3), time(3))),
            .endCase(start: time(1), end: time(3), expected: (time(1), time(3))),
            .endCase(start: time(2, 45), end: time(3, 15), expected: (time(2, 45), time(3))),
            .endCase(start: time(1), end: time(2), expected: (time(1), time(2))),
            .endCase(start: time(2), end: time(2), expected: (time(2), time(2, 15))),
            .endCase(start: time(2, 45), end: time(0), expected: (time(2, 45), time(3))),
            .endCase(start: time(8), end: time(17), expected: (time(8), time(17))),
            .endCase(start: time(23, 45), end: time(23, 45), expected: (time(23, 45), time(0))),
            .endCase(start: time(17), end: time(0, 30), expected: (time(17), time(0, 30))),
            // Setting start time
            .startCase(start: time(8), end: time(3), expected: (time(8), time(3))),
            .startCase(start: time(3), end: time(1), expected: (time(3), time(1))),
            .startCase(start: time(8), end: time(1), expected: (time(8), time(1))),
            .startCase(start: time(1), end: time(2), expected: (time(1), time(2))),
            .startCase(start: time(2), end: time(2), expected: (time(1, 45), time(2))),
            .startCase(start: time(1), end: time(0), expected: (time(23, 45), time(0))),
            .startCase(start: time(8), end: time(17), expected: (time(8), time(17))),
            .startCase(start: time(8), end: time(3, 15), expected: (time(3), time(3, 15))),
            .startCase(start: time(2, 45), end: time(3, 15), expected: (time(3), time(3, 15)))
        ]
    }

    func testTimeChangesKeepWindowValid() throws {
        let boundary = FavoriteSettings.Notifications.companion.serviceBoundary

        for testCase in timeChangeCases {
            let changingStart = testCase.changingStart
            let startTime = testCase.start.toLocalTime()
            let endTime = testCase.end.toLocalTime()
            let expectedStart = testCase.expectedStart.toLocalTime()
            let expectedEnd = testCase.expectedEnd.toLocalTime()
            let window = FavoriteSettings.NotificationsWindow(
                startTime: changingStart ? boundary : startTime,
                endTime: changingStart ? endTime : boundary,
                daysOfWeek: [.monday, .friday]
            )
            var updatedWindow: FavoriteSettings.NotificationsWindow?
            var announcements: [String] = []
            let sut = WindowWidget(
                window: window,
                setWindow: { updatedWindow = $0 },
                deleteWindow: nil,
                announceTimeAdjustment: { announcements.append($0) }
            ).withFixedSettings([:])
            let label = changingStart ? "Select start time" : "Select end time"

            try sut.inspect().find(
                ViewType.DatePicker.self,
                where: { try $0.labelView().text().string() == label }
            ).select(date: (changingStart ? testCase.start : testCase.end).nextDate)

            let actual = try XCTUnwrap(updatedWindow)
            let context = "\(label): \(testCase.start) to \(testCase.end)"
            XCTAssertEqual(actual.startTime, expectedStart, context)
            XCTAssertEqual(actual.endTime, expectedEnd, context)
            XCTAssertEqual(actual.daysOfWeek, window.daysOfWeek, context)
            XCTAssertEqual(actual.id, window.id, context)

            let requested = changingStart ? startTime : endTime
            let adjusted = changingStart ? expectedStart : expectedEnd
            let startString = describe(expectedStart, note: expectedStart == boundary ? "start of service" : nil)
            let isNextDay = (expectedEnd.hour, expectedEnd.minute) <= (expectedStart.hour, expectedStart.minute)
            let endString = describe(
                expectedEnd,
                note: expectedEnd == boundary ? "end of service" : isNextDay ? "next day" : nil
            )
            // The changing endpoint starts at the boundary, so it is kept when adjusted back to the boundary
            let verb = adjusted == boundary ? "kept at" : "changed to"
            let expectedAnnouncement: String? = if adjusted == requested {
                nil
            } else if changingStart {
                "Start time \(verb) \(startString) because end time is \(endString)."
            } else {
                "End time \(verb) \(endString) because start time is \(startString)."
            }
            XCTAssertEqual(announcements, expectedAnnouncement.map { [$0] } ?? [], context)
        }
    }

    private func describe(_ time: Kotlinx_datetimeLocalTime, note: String?) -> String {
        let timeString = Date.fromLocalTime(time).formatted(date: .omitted, time: .shortened)
        return if let note { "\(timeString), \(note)" } else { timeString }
    }

    @MainActor
    func testTimeInputShowsAdjustedTimeImmediately() {
        let adjusted = DateComponents(hour: 3, minute: 0)
        let adjustedString = adjusted.nextDate.formatted(date: .omitted, time: .shortened)
        var requested: DateComponents?
        // `time` never changes, as when the view model doesn't publish an update
        let sut = TimeInput(
            label: Text("Select end time"),
            time: DateComponents(hour: 20, minute: 0),
            type: FavoriteSettings.NotificationsWindowTypeBasic.shared,
            setTime: { time in
                requested = time
                return adjusted
            }
        )

        let selectExp = sut.inspection.inspect { view in
            try view.find(ViewType.DatePicker.self).select(date: DateComponents(hour: 8, minute: 0).nextDate)
        }
        let displayExp = sut.inspection.inspect(after: 0.1) { view in
            XCTAssertEqual(requested?.hour, 8)
            XCTAssertNoThrow(try view.find(text: adjustedString))
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        defer { ViewHosting.expel() }

        wait(for: [selectExp, displayExp], timeout: 2)
    }

    func testRequestsPermission() throws {
        let permissionExp = expectation(description: "permission was requested")

        var enabled = false

        let settings: FavoriteSettings.Notifications = .companion.disabled
        let permissionManager = MockNotificationPermissionManager(
            initialAuthorizationStatus: .notDetermined,
            requestPermissionResponse: true,
            onRequestPermission: { permissionExp.fulfill() }
        )

        let sut = NotificationSettingsWidgetPresentationView(
            state: .init(settings: settings, selectedPreset: nil),
            showPresets: true,
            setEnabled: { enabled = $0 },
            notificationPermissionManager: permissionManager
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        defer { ViewHosting.expel() }

        try sut.inspect().find(text: "Get disruption notifications").find(ViewType.Toggle.self, relation: .parent).tap()
        try sut.inspect().findAndCallOnChange(newValue: true)
        wait(for: [permissionExp])

        XCTAssertTrue(enabled)
        XCTAssertEqual(.authorized, permissionManager.authorizationStatus)
    }

    @MainActor
    func testPermissionDenied() {
        let settingsLinkExp = expectation(description: "settings link was tapped")

        let settings: FavoriteSettings.Notifications = .companion.disabled
        let permissionManager = MockNotificationPermissionManager(
            initialAuthorizationStatus: .denied,
            requestPermissionResponse: false,
            onRequestPermission: {},
            onOpenSettings: { settingsLinkExp.fulfill() }
        )
        let sut = NotificationSwitch(
            settings: settings,
            onValueChanged: { _ in },
            notificationPermissionManager: permissionManager
        )

        let exp = sut.inspection.inspect(after: 2.0) { view in
            XCTAssert(try view.find(text: "Get disruption notifications").find(ViewType.Toggle.self, relation: .parent)
                .isDisabled())

            try view.find(button: "Allow Notifications in Settings").tap()
        }

        ViewHosting.host(view: sut)
        defer { ViewHosting.expel() }

        wait(for: [exp, settingsLinkExp], timeout: 5)
    }

    func testPresetButtonsAreVisibleWhenFeatureFlagEnabled() throws {
        let settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [FavoriteSettings.NotificationsWindow(preset: .morning, daysOfWeek: [.monday])]
        )

        let sut = NotificationSettingsWidgetPresentationView(
            state: .init(settings: settings, selectedPreset: nil),
            showPresets: true,
            notificationPermissionManager: MockNotificationPermissionManager(),
        ).withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(button: "Morning"))
        XCTAssertNotNil(try sut.inspect().find(button: "Midday"))
        XCTAssertNotNil(try sut.inspect().find(button: "Evening"))
        XCTAssertNotNil(try sut.inspect().find(button: "All day"))
        XCTAssertNotNil(try sut.inspect().find(button: "Custom"))
    }

    func testPresetButtonsAreHiddenWhenEditingEnabledFavorite() throws {
        let settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [FavoriteSettings.NotificationsWindow(preset: .morning, daysOfWeek: [.monday])]
        )

        let sut = NotificationSettingsWidgetPresentationView(
            state: .init(settings: settings, selectedPreset: nil),
            showPresets: false,
            notificationPermissionManager: MockNotificationPermissionManager(),
        ).withFixedSettings([:])

        XCTAssertThrowsError(try sut.inspect().find(button: "Morning"))
        XCTAssertThrowsError(try sut.inspect().find(button: "Midday"))
        XCTAssertThrowsError(try sut.inspect().find(button: "Evening"))
        XCTAssertThrowsError(try sut.inspect().find(button: "All day"))
    }

    func testStartAndEndOfService() throws {
        NSTimeZone.default = NSTimeZone.system
        let settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [FavoriteSettings.NotificationsWindow(preset: .allDay, daysOfWeek: [.monday])]
        )

        let sut = NotificationSettingsWidgetPresentationView(
            state: .init(settings: settings, selectedPreset: nil),
            showPresets: true,
            notificationPermissionManager: MockNotificationPermissionManager(),
        ).withFixedSettings([:])

        XCTAssertEqual(
            2,
            try sut.inspect()
                .findAll(ViewType.Text.self)
                .filter { try $0.string() == "3:00\u{202F}AM" }
                .count
        )
        XCTAssertNotNil(try sut.inspect().find(text: "start of service"))
        XCTAssertNotNil(try sut.inspect().find(text: "end of service"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "service-start-sun"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "service-end-moon"))
        XCTAssertNotNil(try sut.inspect().find(where: { view in
            try view.accessibilityValue().string() == "3:00\u{202F}AM, start of service"
        }))
        XCTAssertNotNil(try sut.inspect().find(where: { view in
            try view.accessibilityValue().string() == "3:00\u{202F}AM, end of service"
        }))
    }

    func testNextDay() throws {
        let settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [
                FavoriteSettings.NotificationsWindow(
                    startTime: .init(hour: 22, minute: 0, second: 0, nanosecond: 0),
                    endTime: .init(hour: 1, minute: 0, second: 0, nanosecond: 0),
                    daysOfWeek: [.monday]
                )
            ]
        )

        let sut = NotificationSettingsWidgetPresentationView(
            state: .init(settings: settings, selectedPreset: nil),
            showPresets: true,
            notificationPermissionManager: MockNotificationPermissionManager(),
        ).withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(text: "next day"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "service-end-moon"))
    }
}
