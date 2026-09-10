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
    @MainActor func testEnable() throws {
        let settings: FavoriteSettings.Notifications = .companion.disabled
        var enabled = false

        let sut = NotificationSettingsWidgetPresetnationView(
            state: .init(settings: settings, selectedPreset: nil),
            setEnabled: { enabled = $0 },
            notificationPermissionManager: MockNotificationPermissionManager()
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
        let sut = NotificationSettingsWidgetPresetnationView(
            state: .init(settings: .init(enabled: true, windows: [firstWindow]), selectedPreset: nil),
            addPlaceholderWindow: { addedWindow = true },
            notificationPermissionManager: MockNotificationPermissionManager()
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

        XCTAssertThrowsError(try sut.inspect().find(viewWithAccessibilityLabel: "Delete"))
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

        let sut = NotificationSettingsWidgetPresetnationView(
            state: .init(settings: settings, selectedPreset: nil),
            notificationPermissionManager: MockNotificationPermissionManager()
        )
        .withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "Delete"))
    }

    func testChangeStartTime() throws {
        var settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [.init(preset: .morning,
                            daysOfWeek: [.monday])]
        )
        var customWindows: [FavoriteSettings.NotificationsWindow] = []

        let sut = NotificationSettingsWidgetPresetnationView(state: .init(settings: settings, selectedPreset: nil),
                                                             setCustomWindows: { customWindows = $0 },
                                                             notificationPermissionManager: MockNotificationPermissionManager(
                                                             ))
                                                             .withFixedSettings([:])

        try sut.inspect().find(
            ViewType.DatePicker.self,
            where: { try $0.labelView().text().string() == "Select start time" }
        )
        .select(date: XCTUnwrap(Calendar(identifier: .iso8601).nextDate(
            after: .now,
            matching: .init(hour: 7, minute: 45),
            matchingPolicy: .strict
        )))

        XCTAssertEqual(
            customWindows,
            [.init(
                startTime: .init(hour: 7, minute: 45, second: 0, nanosecond: 0),
                endTime: Preset.morning.endTime,
                daysOfWeek: [.monday]
            )]
        )
    }

    func testChangeEndTime() throws {
        var settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [.init(preset: .morning,
                            daysOfWeek: [.monday])]
        )
        var customWindows: [FavoriteSettings.NotificationsWindow] = []

        let sut = NotificationSettingsWidgetPresetnationView(state: .init(settings: settings, selectedPreset: nil),
                                                             setCustomWindows: { customWindows = $0 },
                                                             notificationPermissionManager: MockNotificationPermissionManager(
                                                             ))
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

        XCTAssertEqual(
            customWindows,
            [.init(
                startTime: Preset.morning.startTime,
                endTime: .init(hour: 13, minute: 45, second: 0, nanosecond: 0),
                daysOfWeek: [.monday]
            )]
        )
    }

    func testChangeDays() throws {
        var settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [.init(preset: .morning,
                            daysOfWeek: [.monday])]
        )
        var customWindows: [FavoriteSettings.NotificationsWindow] = []

        let sut = NotificationSettingsWidgetPresetnationView(state: .init(settings: settings, selectedPreset: nil),
                                                             setCustomWindows: { customWindows = $0 },
                                                             notificationPermissionManager: MockNotificationPermissionManager(
                                                             ))
                                                             .withFixedSettings([:])

        try sut.inspect().find(text: "Sun").find(ViewType.VStack.self, relation: .parent).callOnTapGesture()
        XCTAssertEqual(customWindows[0].daysOfWeek, [.sunday, .monday])
    }

    func testValidatesTime() throws {
        var settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [.init(
                startTime: .init(hour: 8, minute: 0, second: 0, nanosecond: 0),
                endTime: .init(hour: 9, minute: 0, second: 0, nanosecond: 0),
                daysOfWeek: [.monday, .tuesday, .wednesday, .thursday, .friday]
            )]
        )
        var customWindows: [FavoriteSettings.NotificationsWindow] = []

        let sut = NotificationSettingsWidgetPresetnationView(
            state: .init(settings: settings, selectedPreset: nil),
            setCustomWindows: { customWindows = $0 },
            notificationPermissionManager: MockNotificationPermissionManager()
        ).withFixedSettings([:])

        let calendar = Calendar(identifier: .iso8601)
        let dayStart = calendar.startOfDay(for: .now)
        try sut.inspect().find(
            ViewType.DatePicker.self,
            where: { try $0.labelView().text().string() == "Select start time" }
        )
        .select(date: XCTUnwrap(calendar.nextDate(
            after: dayStart,
            matching: .init(hour: 10, minute: 45),
            matchingPolicy: .strict
        )))
        XCTAssertEqual(customWindows[0].startTime, .init(hour: 10, minute: 45, second: 0, nanosecond: 0))
        XCTAssertEqual(customWindows[0].endTime, .init(hour: 11, minute: 45, second: 0, nanosecond: 0))
        // ViewInspector appears not to expose or enforce valid ranges, so can’t test minimum end time
    }

    func testRequestsPermission() throws {
        let permissionExp = expectation(description: "permission was requested")

        var enabled = false

        var settings: FavoriteSettings.Notifications = .companion.disabled
        let permissionManager = MockNotificationPermissionManager(
            initialAuthorizationStatus: .notDetermined,
            requestPermissionResponse: true,
            onRequestPermission: { permissionExp.fulfill() }
        )

        let sut = NotificationSettingsWidgetPresetnationView(state: .init(settings: settings, selectedPreset: nil),
                                                             setEnabled: { enabled = $0 },
                                                             notificationPermissionManager: permissionManager)

        ViewHosting.host(view: sut.withFixedSettings([:]))

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

        wait(for: [exp, settingsLinkExp], timeout: 5)
    }

    func testPresetButtonsAreNotVisibleWhenFeatureFlagDisabled() throws {
        let settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [FavoriteSettings.NotificationsWindow(preset: .morning, daysOfWeek: [.monday])]
        )

        let sut = NotificationSettingsWidgetPresetnationView(state: .init(settings: settings, selectedPreset: nil),
                                                             notificationPermissionManager: MockNotificationPermissionManager(
                                                             ))
                                                             .withFixedSettings([.notificationPresetWindows: false])

        XCTAssertThrowsError(try sut.inspect().find(button: "Morning"))
    }

    func testPresetButtonsAreVisibleWhenFeatureFlagEnabled() throws {
        let settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [FavoriteSettings.NotificationsWindow(preset: .morning, daysOfWeek: [.monday])]
        )

        let sut = NotificationSettingsWidgetPresetnationView(
            state: .init(settings: settings, selectedPreset: nil),
            notificationPermissionManager: MockNotificationPermissionManager()
        ).withFixedSettings([.notificationPresetWindows: true])

        XCTAssertNotNil(try sut.inspect().find(button: "Morning"))
        XCTAssertNotNil(try sut.inspect().find(button: "Midday"))
        XCTAssertNotNil(try sut.inspect().find(button: "Evening"))
        XCTAssertNotNil(try sut.inspect().find(button: "All day"))
        XCTAssertNotNil(try sut.inspect().find(button: "Custom"))
    }
}
