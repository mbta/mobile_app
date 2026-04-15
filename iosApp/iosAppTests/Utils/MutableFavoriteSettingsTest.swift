//
//  MutableFavoriteSettingsTest.swift
//  iosApp
//
//  Created by Kayla Brady on 4/8/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import XCTest

final class MutableFavoriteSettingsTest: XCTestCase {
    func testSafeEndWindowEndUnchangedWhenStillAfterStart() {
        let initialStart: DateComponents = .init(hour: 0, minute: 1, second: 3)
        let initialEnd: DateComponents = .init(hour: 2, minute: 3, second: 4)
        let window = MutableFavoriteSettings.Notifications.Window(
            startTime: initialStart,
            endTime: initialEnd,
            daysOfWeek: [.monday]
        )

        window.setSafeEndTime(startTime: .init(hour: 0, minute: 5, second: 6))

        XCTAssertEqual(window.endTime, initialEnd)
    }

    func testSafeEndWindowPushedBackWhenBeforeNewStart() {
        let initialStart: DateComponents = .init(hour: 0, minute: 1, second: 3)
        let initialEnd: DateComponents = .init(hour: 2, minute: 3, second: 4)
        let window = MutableFavoriteSettings.Notifications.Window(
            startTime: initialStart,
            endTime: initialEnd,
            daysOfWeek: [.monday]
        )

        window.setSafeEndTime(startTime: .init(hour: 4, minute: 5, second: 6))

        XCTAssertEqual(window.endTime, .init(hour: 5, minute: 5, second: 0))
    }

    func testSafeEndWindowPushedHourBackWhenEqualsNewStart() {
        let initialStart: DateComponents = .init(hour: 0, minute: 1, second: 3)
        let initialEnd: DateComponents = .init(hour: 2, minute: 3, second: 4)
        let window = MutableFavoriteSettings.Notifications.Window(
            startTime: initialStart,
            endTime: initialEnd,
            daysOfWeek: [.monday]
        )

        window.setSafeEndTime(startTime: initialEnd)

        XCTAssertEqual(window.endTime, .init(hour: 3, minute: 3, second: 0))
    }

    func testSafeEndWindowPushedBackToEoDWhenEqualsNewStart() {
        let initialStart: DateComponents = .init(hour: 0, minute: 1, second: 3)
        let initialEnd: DateComponents = .init(hour: 2, minute: 3, second: 4)
        let window = MutableFavoriteSettings.Notifications.Window(
            startTime: initialStart,
            endTime: initialEnd,
            daysOfWeek: [.monday]
        )

        window.setSafeEndTime(startTime: .init(hour: 23, minute: 0, second: 1))

        XCTAssertEqual(window.endTime, .init(hour: 23, minute: 59, second: 0))
    }

    func testMinEndMidHour() {
        let initialStart: DateComponents = .init(hour: 0, minute: 0, second: 0)
        let initialEnd: DateComponents = .init(hour: 1, minute: 0, second: 0)
        let window = MutableFavoriteSettings.Notifications.Window(
            startTime: initialStart,
            endTime: initialEnd,
            daysOfWeek: [.monday]
        )

        XCTAssertEqual(window.minimumEndTime(), .init(hour: 0, minute: 1, second: 0))
    }

    func testMinEndAt59() {
        let initialStart: DateComponents = .init(hour: 0, minute: 59, second: 0)
        let initialEnd: DateComponents = .init(hour: 2, minute: 0, second: 0)
        let window = MutableFavoriteSettings.Notifications.Window(
            startTime: initialStart,
            endTime: initialEnd,
            daysOfWeek: [.monday]
        )

        XCTAssertEqual(window.minimumEndTime(), .init(hour: 1, minute: 0, second: 0))
    }

    func testMinEndAtEOD() {
        let initialStart: DateComponents = .init(hour: 23, minute: 59, second: 0)
        let initialEnd: DateComponents = .init(hour: 23, minute: 59, second: 0)
        let window = MutableFavoriteSettings.Notifications.Window(
            startTime: initialStart,
            endTime: initialEnd,
            daysOfWeek: [.monday]
        )

        XCTAssertEqual(window.minimumEndTime(), .init(hour: 23, minute: 59, second: 0))
    }
}
