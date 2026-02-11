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
        let settings: MutableFavoriteSettings.Notifications = .init(.companion.disabled)
        let sut = NotificationSettingsWidget(
            settings: settings,
            notificationPermissionManager: MockNotificationPermissionManager()
        )

        try sut.inspect().find(text: "Get disruption notifications").find(ViewType.Toggle.self, relation: .parent).tap()
        XCTAssertEqual(settings, .init(enabled: true, windows: []))

        try sut.inspect().findAndCallOnChange(newValue: true)
        try await Task.sleep(for: .seconds(1))

        XCTAssertEqual(
            settings,
            .init(
                enabled: true,
                windows: [.init(
                    startTime: .init(hour: 8, minute: 0, second: 0),
                    endTime: .init(hour: 9, minute: 0, second: 0),
                    daysOfWeek: [.monday, .tuesday, .wednesday, .thursday, .friday]
                )]
            )
        )
    }

    func testAddSecondTimePeriod() throws {
        let firstWindow = MutableFavoriteSettings.Notifications.Window(
            startTime: .init(hour: 1, minute: 0, second: 0),
            endTime: .init(hour: 2, minute: 0, second: 0),
            daysOfWeek: [.thursday]
        )
        let settings = MutableFavoriteSettings.Notifications(enabled: true, windows: [firstWindow])
        let sut = NotificationSettingsWidget(
            settings: settings,
            notificationPermissionManager: MockNotificationPermissionManager()
        )

        // unfortunately, ViewInspector does not appear to surface the selected value of a DatePicker
        XCTAssertNotNil(try sut.inspect().find(
            ViewType.DatePicker.self,
            where: { try $0.labelView().text().string() == "From" }
        ))
        XCTAssertNotNil(try sut.inspect().find(
            ViewType.DatePicker.self,
            where: { try $0.labelView().text().string() == "To" }
        ))
        // ViewInspector as of 0.10.3 does not support accessibilityChildren so we can’t check the days of the week
        try sut.inspect().find(button: "Add another time period").tap()
        XCTAssertEqual(
            settings,
            .init(
                enabled: true,
                windows: [
                    firstWindow,
                    .init(
                        startTime: .init(hour: 12, minute: 0, second: 0),
                        endTime: .init(hour: 13, minute: 0, second: 0),
                        daysOfWeek: [.saturday, .sunday]
                    ),
                ]
            )
        )
    }

    func testChangeTime() throws {
        let settings = MutableFavoriteSettings.Notifications(
            enabled: true,
            windows: [.init(
                startTime: .init(hour: 8, minute: 0, second: 0),
                endTime: .init(hour: 9, minute: 0, second: 0),
                daysOfWeek: [.monday, .tuesday, .wednesday, .thursday, .friday]
            )]
        )
        let sut = NotificationSettingsWidget(
            settings: settings,
            notificationPermissionManager: MockNotificationPermissionManager()
        )

        try sut.inspect().find(ViewType.DatePicker.self, where: { try $0.labelView().text().string() == "From" })
            .select(date: Calendar(identifier: .iso8601).nextDate(
                after: .now,
                matching: .init(hour: 7, minute: 45),
                matchingPolicy: .strict
            )!)
        XCTAssertEqual(settings.windows[0].startTime, .init(hour: 7, minute: 45, second: 0))
        try sut.inspect().find(ViewType.DatePicker.self, where: { try $0.labelView().text().string() == "To" })
            .select(date: Calendar(identifier: .iso8601).nextDate(
                after: .now,
                matching: .init(hour: 9, minute: 10),
                matchingPolicy: .strict
            )!)
        XCTAssertEqual(settings.windows[0].endTime, .init(hour: 9, minute: 10, second: 0))
    }

    func testChangeDays() throws {
        let settings = MutableFavoriteSettings.Notifications(
            enabled: true,
            windows: [.init(
                startTime: .init(hour: 8, minute: 0, second: 0),
                endTime: .init(hour: 9, minute: 0, second: 0),
                daysOfWeek: [.monday, .tuesday, .wednesday, .thursday, .friday]
            )]
        )
        let sut = NotificationSettingsWidget(
            settings: settings,
            notificationPermissionManager: MockNotificationPermissionManager()
        )

        try sut.inspect().find(text: "Sun").find(ViewType.VStack.self, relation: .parent).callOnTapGesture()
        XCTAssertEqual(settings.windows[0].daysOfWeek, [.sunday, .monday, .tuesday, .wednesday, .thursday, .friday])
        try sut.inspect().find(text: "Wed").find(ViewType.VStack.self, relation: .parent).callOnTapGesture()
        XCTAssertEqual(settings.windows[0].daysOfWeek, [.sunday, .monday, .tuesday, .thursday, .friday])
    }

    func testValidatesTime() throws {
        let settings = MutableFavoriteSettings.Notifications(
            enabled: true,
            windows: [.init(
                startTime: .init(hour: 8, minute: 0, second: 0),
                endTime: .init(hour: 9, minute: 0, second: 0),
                daysOfWeek: [.monday, .tuesday, .wednesday, .thursday, .friday]
            )]
        )
        let sut = NotificationSettingsWidget(
            settings: settings,
            notificationPermissionManager: MockNotificationPermissionManager()
        )

        let calendar = Calendar(identifier: .iso8601)
        let dayStart = calendar.startOfDay(for: .now)
        try sut.inspect().find(ViewType.DatePicker.self, where: { try $0.labelView().text().string() == "From" })
            .select(date: calendar.nextDate(
                after: dayStart,
                matching: .init(hour: 10, minute: 45),
                matchingPolicy: .strict
            )!)
        XCTAssertEqual(settings.windows[0].startTime, .init(hour: 10, minute: 45, second: 0))
        try sut.inspect().findAndCallOnChange(newValue: settings.windows[0].startTime)
        XCTAssertEqual(settings.windows[0].endTime, .init(hour: 11, minute: 45, second: 0))
        // ViewInspector appears not to expose or enforce valid ranges, so can’t test minimum end time
    }

    func testRequestsPermission() throws {
        let permissionExp = expectation(description: "permission was requested")

        let settings: MutableFavoriteSettings.Notifications = .init(.companion.disabled)
        let permissionManager = MockNotificationPermissionManager(
            initialAuthorizationStatus: .notDetermined,
            requestPermissionResponse: true,
            onRequestPermission: { permissionExp.fulfill() }
        )
        let sut = NotificationSettingsWidget(settings: settings, notificationPermissionManager: permissionManager)
        ViewHosting.host(view: sut.withFixedSettings([:]))

        try sut.inspect().find(text: "Get disruption notifications").find(ViewType.Toggle.self, relation: .parent).tap()
        wait(for: [permissionExp])

        XCTAssertEqual(.init(enabled: true, windows: [.init(
            startTime: .init(hour: 8, minute: 0, second: 0),
            endTime: .init(hour: 9, minute: 0, second: 0),
            daysOfWeek: [.monday, .tuesday, .wednesday, .thursday, .friday]
        )]), settings)
        XCTAssertEqual(.authorized, permissionManager.authorizationStatus)
    }

    func testPermissionDenied() throws {
        let permissionExp = expectation(description: "permission was requested")
        let settingsLinkExp = expectation(description: "settings link was tapped")

        let settings: MutableFavoriteSettings.Notifications = .init(.companion.disabled)
        let permissionManager = MockNotificationPermissionManager(
            initialAuthorizationStatus: .notDetermined,
            requestPermissionResponse: false,
            onRequestPermission: { permissionExp.fulfill() },
            onOpenSettings: { settingsLinkExp.fulfill() }
        )
        let sut = NotificationSettingsWidget(settings: settings, notificationPermissionManager: permissionManager)
        ViewHosting.host(view: sut.withFixedSettings([:]))

        try sut.inspect().find(text: "Get disruption notifications").find(ViewType.Toggle.self, relation: .parent).tap()
        wait(for: [permissionExp])

        XCTAssertEqual(.init(.companion.disabled), settings)
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
}
