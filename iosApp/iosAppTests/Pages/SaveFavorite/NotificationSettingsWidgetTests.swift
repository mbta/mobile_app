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
        var settings: FavoriteSettings.Notifications?
        let sut = NotificationSettingsWidget(settings: .companion.disabled, setSettings: { settings = $0 })

        try sut.inspect().find(text: "Get disruption notifications").find(ViewType.Toggle.self, relation: .parent).tap()
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
        var settings: FavoriteSettings.Notifications?
        let firstWindow = FavoriteSettings.NotificationsWindow(
            startTime: .init(hour: 1, minute: 0, second: 0, nanosecond: 0),
            endTime: .init(hour: 2, minute: 0, second: 0, nanosecond: 0),
            daysOfWeek: [.thursday]
        )
        let sut = NotificationSettingsWidget(
            settings: .init(enabled: true, windows: [firstWindow]),
            setSettings: { settings = $0 }
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
                        startTime: .init(hour: 12, minute: 0, second: 0, nanosecond: 0),
                        endTime: .init(hour: 13, minute: 0, second: 0, nanosecond: 0),
                        daysOfWeek: [.saturday, .sunday]
                    ),
                ]
            )
        )
    }

    func testChangeTime() throws {
        var settings = FavoriteSettings.Notifications(
            enabled: true,
            windows: [.init(
                startTime: .init(hour: 8, minute: 0, second: 0, nanosecond: 0),
                endTime: .init(hour: 9, minute: 0, second: 0, nanosecond: 0),
                daysOfWeek: [.monday, .tuesday, .wednesday, .thursday, .friday]
            )]
        )
        let sut = NotificationSettingsWidget(settings: settings, setSettings: { settings = $0 })

        try sut.inspect().find(ViewType.DatePicker.self, where: { try $0.labelView().text().string() == "From" })
            .select(date: Calendar(identifier: .iso8601).nextDate(
                after: .now,
                matching: .init(hour: 7, minute: 45),
                matchingPolicy: .strict
            )!)
        XCTAssertEqual(settings.windows[0].startTime, .init(hour: 7, minute: 45, second: 0, nanosecond: 0))
        try sut.inspect().find(ViewType.DatePicker.self, where: { try $0.labelView().text().string() == "To" })
            .select(date: Calendar(identifier: .iso8601).nextDate(
                after: .now,
                matching: .init(hour: 9, minute: 10),
                matchingPolicy: .strict
            )!)
        XCTAssertEqual(settings.windows[0].endTime, .init(hour: 9, minute: 10, second: 0, nanosecond: 0))
    }

    func testChangeDays() throws {
        var settings = FavoriteSettings.Notifications(
            enabled: true,
            windows: [.init(
                startTime: .init(hour: 8, minute: 0, second: 0, nanosecond: 0),
                endTime: .init(hour: 9, minute: 0, second: 0, nanosecond: 0),
                daysOfWeek: [.monday, .tuesday, .wednesday, .thursday, .friday]
            )]
        )
        let sut = NotificationSettingsWidget(settings: settings, setSettings: { settings = $0 })

        try sut.inspect().find(text: "Sun").find(ViewType.VStack.self, relation: .parent).callOnTapGesture()
        XCTAssertEqual(settings.windows[0].daysOfWeek, [.sunday, .monday, .tuesday, .wednesday, .thursday, .friday])
        try sut.inspect().find(text: "Wed").find(ViewType.VStack.self, relation: .parent).callOnTapGesture()
        // sunday is not still included since the view doesn’t update after we tapped it
        XCTAssertEqual(settings.windows[0].daysOfWeek, [.monday, .tuesday, .thursday, .friday])
    }
}
