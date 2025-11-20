//
//  SaveFavoriteHeaderTests.swift
//  iosApp
//
//  Created by esimon on 11/19/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class SaveFavoriteHeaderTests: XCTestCase {
    @MainActor func testSaveHeader() {
        let sut = SaveFavoriteHeader(
            isFavorite: false,
            onCancel: {},
            onSave: {}
        )

        ViewHosting.host(view: sut)

        XCTAssertNotNil(try? sut.inspect().find(text: "Add Favorite"))
    }

    @MainActor func testEditHeader() {
        let sut = SaveFavoriteHeader(
            isFavorite: true,
            onCancel: {},
            onSave: {}
        )

        ViewHosting.host(view: sut)

        XCTAssertNotNil(try? sut.inspect().find(text: "Edit Favorite"))
    }

    @MainActor func testCancelButton() {
        let cancelExp = expectation(description: "called back")

        let sut = SaveFavoriteHeader(
            isFavorite: true,
            onCancel: { cancelExp.fulfill() },
            onSave: { XCTFail("Should not save") }
        )

        ViewHosting.host(view: sut)

        try? sut.inspect().find(button: "Cancel").tap()
        wait(for: [cancelExp], timeout: 1)
    }

    @MainActor func testSaveButton() {
        let saveExp = expectation(description: "called save")

        let sut = SaveFavoriteHeader(
            isFavorite: true,
            onCancel: { XCTFail("Should not cancel") },
            onSave: { saveExp.fulfill() }
        )

        ViewHosting.host(view: sut)

        try? sut.inspect().find(button: "Save").tap()
        wait(for: [saveExp], timeout: 1)
    }
}
