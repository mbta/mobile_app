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
        var selectedPreset: Preset?

        let sut = PresetWindowSelector(
            presetRows: [[.morning, .midday]],
            selectedPreset: .midday,
            onSelect: { preset in selectedPreset = preset }
        )

        XCTAssertNotNil(try sut.inspect().find(button: "Morning"))
        XCTAssertNotNil(try sut.inspect().find(button: "Midday"))
        XCTAssertNotNil(try sut.inspect().find(button: "Custom"))

        try? sut.inspect().find(button: "Morning").tap()

        XCTAssertEqual(selectedPreset, .morning)
    }

    func testCustomSelectsNil() {
        var selectedPreset: Preset? = .morning

        let sut = PresetWindowSelector(
            presetRows: [[.morning, .midday]],
            selectedPreset: selectedPreset,
            onSelect: { preset in selectedPreset = preset }
        )

        try? sut.inspect().find(button: "Custom").tap()
        XCTAssertEqual(nil, selectedPreset)
    }
}
