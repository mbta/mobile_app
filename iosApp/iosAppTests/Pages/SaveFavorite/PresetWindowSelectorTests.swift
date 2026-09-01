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
        var selectedWindows: [FavoriteSettings.NotificationsWindow]?

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
            customPreset: [FavoriteSettings.NotificationsWindow.companion
                .eveningDefault(daysOfWeek: FavoriteSettings.NotificationsWindow.companion.weekend)],
            onSelect: { windows in selectedWindows = windows }
        )

        XCTAssertNotNil(try sut.inspect().find(button: "Morning"))
        XCTAssertNotNil(try sut.inspect().find(button: "Midday"))
        XCTAssertNotNil(try sut.inspect().find(button: "Custom"))

        try? sut.inspect().find(button: "Morning").tap()

        XCTAssertEqual(
            selectedWindows,
            [.companion.morningDefault(daysOfWeek: FavoriteSettings.NotificationsWindow.companion.weekdays)]
        )
    }

    func testCustomUsesProvidedCustomPreset() {
        var selectedWindows: [FavoriteSettings.NotificationsWindow] = []
        let customWindow = FavoriteSettings.NotificationsWindow(
            startTime: .init(hour: 10, minute: 15, second: 0, nanosecond: 0),
            endTime: .init(hour: 11, minute: 45, second: 0, nanosecond: 0),
            daysOfWeek: [.sunday, .tuesday]
        )

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
            customPreset: [customWindow],
            onSelect: { windows in selectedWindows = windows }
        )

        try? sut.inspect().find(button: "Custom").tap()
        XCTAssertEqual(selectedWindows, [customWindow])
    }
}
