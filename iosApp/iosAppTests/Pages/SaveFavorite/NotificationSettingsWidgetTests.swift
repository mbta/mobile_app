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
    func testAddTimePeriod() throws {
        let settings: MutableFavoriteSettings.Notifications = .init(.companion.disabled)
        let sut = NotificationSettingsWidget(settings: settings)

        try sut.inspect().find(text: "Get disruption notifications").find(ViewType.Toggle.self, relation: .parent).tap()
        XCTAssertEqual(settings, .init(enabled: true, windows: []))
        try sut.inspect().findAndCallOnChange(newValue: true)
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
        let sut = NotificationSettingsWidget(settings: settings)

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
        let sut = NotificationSettingsWidget(settings: settings)

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
        let sut = NotificationSettingsWidget(settings: settings)

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
        let sut = NotificationSettingsWidget(settings: settings)

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
}
