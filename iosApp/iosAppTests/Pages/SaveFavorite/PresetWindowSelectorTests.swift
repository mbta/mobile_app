//
//  PresetWindowSelectorTests.swift
//  iosApp
//
//  Created by Kayla Brady on 8/31/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class PresetWindowSelectorTests: XCTestCase {
    func testPresetWindowsVisible() {
        var selectedWindow: FavoriteSettings.NotificationsWindow?

        let sut = PresetWindowSelector(
            presetRows: [[
                .init(
                    label: "Morning",
                    window: .companion
                        .morningDefault(daysOfWeek: FavoriteSettings.NotificationsWindow.companion.weekdays)
                ),
                .init(
                    label: "Midday",
                    window: .companion
                        .middayDefault(daysOfWeek: FavoriteSettings.NotificationsWindow.companion.weekdays)
                )
            ]],
            selectedPreset: .Preset(rowIndex: 1, columnIndex: 0),
            presetsEnabled: true,
            onSelect: { window in selectedWindow = window }
        )

        XCTAssertNotNil(try sut.inspect().find(button: "Morning"))
        XCTAssertNotNil(try sut.inspect().find(button: "Midday"))
        XCTAssertNotNil(try sut.inspect().find(button: "Custom"))

        try? sut.inspect().find(button: "Morning").tap()

        XCTAssertEqual(
            selectedWindow,
            .companion.morningDefault(daysOfWeek: FavoriteSettings.NotificationsWindow.companion.weekdays)
        )
    }

    func testCustomDefaultsToNow() {
        var selectedWindow: FavoriteSettings.NotificationsWindow?

        let sut = PresetWindowSelector(
            presetRows: [[
                .init(
                    label: "Morning",
                    window: .companion
                        .morningDefault(daysOfWeek: FavoriteSettings.NotificationsWindow.companion.weekdays)
                ),
                .init(
                    label: "Midday",
                    window: .companion
                        .middayDefault(daysOfWeek: FavoriteSettings.NotificationsWindow.companion.weekdays)
                )
            ]],
            selectedPreset: .Preset(rowIndex: 1, columnIndex: 0),
            presetsEnabled: true,
            now: .init(year: 2026, month: .august, day: 31, hour: 4, minute: 30, second: 0),
            onSelect: { window in selectedWindow = window }
        )

        try? sut.inspect().find(button: "Custom").tap()
        XCTAssertEqual(
            selectedWindow,
            .init(
                startTime: .init(hour: 4, minute: 0, second: 0, nanosecond: 0),
                endTime: .init(hour: 5, minute: 0, second: 0, nanosecond: 0),
                daysOfWeek: [.monday, .tuesday, .wednesday, .thursday, .friday]
            )
        )
    }
}
