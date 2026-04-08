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
    func testWindowEndUnchangedWhenStillAfterStart() {
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

    func testWindowPushedBackWhenBeforeNewStart() {
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

    func testWindowPushedHourBackWhenEqualsNewStart() {
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

    func testWindowPushedBackToEoDWhenEqualsNewStart() {
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
}
