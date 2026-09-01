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
    @MainActor func testAddTimePeriod() async throws {
        var settings: FavoriteSettings.Notifications = .companion.disabled
        let sut = NotificationSettingsWidget(
            settings: settings,
            setSettings: { newSettings in settings = newSettings },
            notificationPermissionManager: MockNotificationPermissionManager()
        ).withFixedSettings([:])

        try sut.inspect().find(text: "Get disruption notifications").find(ViewType.Toggle.self, relation: .parent).tap()
        XCTAssertEqual(settings, .init(enabled: true, windows: []))

        try sut.inspect().findAndCallOnChange(newValue: true)
        try await Task.sleep(for: .seconds(1))

        XCTAssertEqual(
            settings,
            .init(
                enabled: true,
                windows: [.init(
                    startTime: .init(hour: 8, minute: 0, second: 0, nanosecond: 0),
                    endTime: .init(hour: 9, minute: 0, second: 0, nanosecond: 0),
                    daysOfWeek: [.monday, .tuesday, .wednesday, .thursday, .friday]
                )]
            )
        )
    }

    func testAddSecondTimePeriod() throws {
        let firstWindow = FavoriteSettings.NotificationsWindow(
            startTime: .init(hour: 1, minute: 0, second: 0, nanosecond: 0),
            endTime: .init(hour: 2, minute: 0, second: 0, nanosecond: 0),
            daysOfWeek: [.thursday]
        )
        var settings: FavoriteSettings.Notifications = .init(enabled: true, windows: [firstWindow])
        let sut = NotificationSettingsWidget(
            settings: settings,
            setSettings: { newSettings in settings = newSettings },
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
        XCTAssertEqual(
            settings,
            .init(
                enabled: true,
                windows: [
                    firstWindow,
                    .init(
                        startTime: .init(hour: 12, minute: 0, second: 0, nanosecond: 0),
                        endTime: .init(hour: 13, minute: 0, second: 0, nanosecond: 0),
                        daysOfWeek: [.saturday, .sunday]
                    ),
                ]
            )
        )
    }

    func testDeleteButtonWhenTwoTimePeriods() throws {
        var settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [
                .companion.morningDefault(daysOfWeek: FavoriteSettings.NotificationsWindow.companion.weekdays),
                .companion.eveningDefault(daysOfWeek: [.monday])
            ]
        )
        let sut = NotificationSettingsWidget(
            settings: settings,
            setSettings: { newSettings in settings = newSettings },
            notificationPermissionManager: MockNotificationPermissionManager()
        ).withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "Delete"))
    }

    func testChangeTime() throws {
        let now = EasternTimeInstant(year: 2026, month: .august, day: 27, hour: 12, minute: 30, second: 0)

        var settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [.init(
                startTime: .init(hour: 8, minute: 0, second: 0, nanosecond: 0),
                endTime: .init(hour: 9, minute: 0, second: 0, nanosecond: 0),
                daysOfWeek: [.monday, .tuesday, .wednesday, .thursday, .friday]
            )]
        )
        let sut = NotificationSettingsWidget(
            settings: settings,
            setSettings: { newSettings in settings = newSettings },
            notificationPermissionManager: MockNotificationPermissionManager(),
            now: now
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
        XCTAssertEqual(settings.windows[0].startTime, .init(hour: 7, minute: 45, second: 0, nanosecond: 0))
        try sut.inspect().find(
            ViewType.DatePicker.self,
            where: { try $0.labelView().text().string() == "Select end time" }
        )
        .select(date: XCTUnwrap(Calendar(identifier: .iso8601).nextDate(
            after: .now,
            matching: .init(hour: 9, minute: 10),
            matchingPolicy: .strict
        )))
        XCTAssertEqual(settings.windows[0].endTime, .init(hour: 9, minute: 10, second: 0, nanosecond: 0))
    }

    func testChangeDays() throws {
        let now = EasternTimeInstant(year: 2026, month: .august, day: 27, hour: 12, minute: 30, second: 0)

        var settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [.init(
                startTime: .init(hour: 8, minute: 0, second: 0, nanosecond: 0),
                endTime: .init(hour: 9, minute: 0, second: 0, nanosecond: 0),
                daysOfWeek: [.monday, .tuesday, .wednesday, .thursday, .friday]
            )]
        )
        let sut = NotificationSettingsWidget(
            settings: settings,
            setSettings: { newSettings in settings = newSettings },
            notificationPermissionManager: MockNotificationPermissionManager(),
            now: now
        ).withFixedSettings([:])

        try sut.inspect().find(text: "Sun").find(ViewType.VStack.self, relation: .parent).callOnTapGesture()
        XCTAssertEqual(settings.windows[0].daysOfWeek, [.sunday, .monday, .tuesday, .wednesday, .thursday, .friday])
    }

    func testValidatesTime() throws {
        let now = EasternTimeInstant(year: 2026, month: .august, day: 27, hour: 12, minute: 30, second: 0)

        var settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [.init(
                startTime: .init(hour: 8, minute: 0, second: 0, nanosecond: 0),
                endTime: .init(hour: 9, minute: 0, second: 0, nanosecond: 0),
                daysOfWeek: [.monday, .tuesday, .wednesday, .thursday, .friday]
            )]
        )
        let sut = NotificationSettingsWidget(
            settings: settings,
            setSettings: { newSettings in settings = newSettings },
            notificationPermissionManager: MockNotificationPermissionManager(),
            now: now
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
        XCTAssertEqual(settings.windows[0].startTime, .init(hour: 10, minute: 45, second: 0, nanosecond: 0))
        XCTAssertEqual(settings.windows[0].endTime, .init(hour: 11, minute: 45, second: 0, nanosecond: 0))
        // ViewInspector appears not to expose or enforce valid ranges, so can’t test minimum end time
    }

    func testRequestsPermission() throws {
        let now = EasternTimeInstant(year: 2026, month: .august, day: 27, hour: 12, minute: 30, second: 0)

        let permissionExp = expectation(description: "permission was requested")

        var settings: FavoriteSettings.Notifications = .companion.disabled
        let permissionManager = MockNotificationPermissionManager(
            initialAuthorizationStatus: .notDetermined,
            requestPermissionResponse: true,
            onRequestPermission: { permissionExp.fulfill() }
        )
        let sut = NotificationSettingsWidget(settings: settings,
                                             setSettings: { newSettings in settings = newSettings },
                                             notificationPermissionManager: permissionManager,
                                             now: now)
        ViewHosting.host(view: sut.withFixedSettings([:]))

        try sut.inspect().find(text: "Get disruption notifications").find(ViewType.Toggle.self, relation: .parent).tap()
        try sut.inspect().findAndCallOnChange(newValue: true)
        wait(for: [permissionExp])

        XCTAssertEqual(.init(enabled: true, windows: [.init(
            startTime: .init(hour: 8, minute: 0, second: 0, nanosecond: 0),
            endTime: .init(hour: 9, minute: 0, second: 0, nanosecond: 0),
            daysOfWeek: [.monday, .tuesday, .wednesday, .thursday, .friday]
        )]), settings)
        XCTAssertEqual(.authorized, permissionManager.authorizationStatus)
    }

    func testPermissionDenied() throws {
        let permissionExp = expectation(description: "permission was requested")
        let settingsLinkExp = expectation(description: "settings link was tapped")

        var settings: FavoriteSettings.Notifications = .companion.disabled
        let permissionManager = MockNotificationPermissionManager(
            initialAuthorizationStatus: .notDetermined,
            requestPermissionResponse: false,
            onRequestPermission: { permissionExp.fulfill() },
            onOpenSettings: { settingsLinkExp.fulfill() }
        )
        let sut = NotificationSettingsWidget(settings: settings,
                                             setSettings: { newSettings in settings = newSettings },
                                             notificationPermissionManager: permissionManager)
        ViewHosting.host(view: sut.withFixedSettings([:]))

        try sut.inspect().find(text: "Get disruption notifications").find(ViewType.Toggle.self, relation: .parent).tap()
        try sut.inspect().findAndCallOnChange(newValue: true)
        wait(for: [permissionExp])

        XCTAssertEqual(.companion.disabled, settings)
        XCTAssertEqual(.denied, permissionManager.authorizationStatus)
        XCTAssert(try sut.inspect().find(text: "Get disruption notifications")
            .find(ViewType.Toggle.self, relation: .parent).isDisabled())
        try sut.inspect().find(button: "Allow Notifications in Settings").tap()
        wait(for: [settingsLinkExp])

        permissionManager.updateAuthorizationStatus(nextStatus: .authorized)
        XCTAssertFalse(try sut.inspect().find(text: "Get disruption notifications")
            .find(ViewType.Toggle.self, relation: .parent).isDisabled())
        XCTAssertThrowsError(try sut.inspect().find(button: "Allow Notifications in Settings"))
    }

    func testPresetButtonsAreNotVisibleWhenFeatureFlagDisabled() throws {
        let settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [FavoriteSettings.NotificationsWindow.companion.morningDefault(daysOfWeek: [.monday])]
        )
        let sut = NotificationSettingsWidget(
            settings: settings,
            setSettings: { _ in },
            notificationPermissionManager: MockNotificationPermissionManager()
        ).withFixedSettings([.notificationPresetWindows: false])

        XCTAssertThrowsError(try sut.inspect().find(button: "Morning"))
    }

    func testPresetButtonsAreVisibleWhenFeatureFlagEnabled() throws {
        let settings: FavoriteSettings.Notifications = .init(
            enabled: true,
            windows: [FavoriteSettings.NotificationsWindow.companion.morningDefault(daysOfWeek: [.monday])]
        )
        let sut = NotificationSettingsWidget(
            settings: settings,
            setSettings: { _ in },
            notificationPermissionManager: MockNotificationPermissionManager()
        ).withFixedSettings([.notificationPresetWindows: true])

        XCTAssertNotNil(try sut.inspect().find(button: "Morning"))
        XCTAssertNotNil(try sut.inspect().find(button: "Midday"))
        XCTAssertNotNil(try sut.inspect().find(button: "Evening"))
        XCTAssertNotNil(try sut.inspect().find(button: "All day"))
        XCTAssertNotNil(try sut.inspect().find(button: "Custom"))
    }

    @MainActor func testSelectsPresetMatchingCurrentTime() async throws {
        let now = EasternTimeInstant(year: 2026, month: .august, day: 27, hour: 12, minute: 30, second: 0)
        var settings: FavoriteSettings.Notifications = .companion.disabled
        let widget = NotificationSettingsWidget(
            settings: settings,
            setSettings: { newSettings in settings = newSettings },
            notificationPermissionManager: MockNotificationPermissionManager(),
            now: now
        )
        let sut = widget.withFixedSettings([.notificationPresetWindows: true])

        try sut.inspect().find(text: "Get disruption notifications").find(ViewType.Toggle.self, relation: .parent).tap()
        try sut.inspect().findAndCallOnChange(newValue: true)
        try await Task.sleep(for: .seconds(1))

        XCTAssertEqual(
            .init(
                enabled: true,
                windows: [FavoriteSettings.NotificationsWindow.companion
                    .middayDefault(daysOfWeek: FavoriteSettings.NotificationsWindow.companion.weekdays)]
            ),
            settings
        )
    }
}
