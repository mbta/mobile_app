//
//  StopDetailsFilteredViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 6/20/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class StopDetailsViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testStarSavesEnhancedFavoritesWithDialogBehindFlag() throws {}

    @MainActor func testUnfavoriteWithoutDialogBehindFlag() throws {}

    @MainActor func testStarSavesOldPinWithoutEnhancedFlag() throws {}
}
