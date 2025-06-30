//
//  SaveFavoritesFlowTest.swift
//  iosAppTests
//
//  Created by Kayla Brady on 6/30/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Testing

struct SaveFavoritesFlowTest {
    override func setUp() {
        executionTimeAllowance = 60
    }

    @Test func testWithoutTappingAnyButtonSavesProposedChanges() async throws {}

    @Test func testCancelDoesntUpdateFavorites() async throws {}

    @Test func testAddingOtherDirectionSavesBoth() async throws {}

    @Test func testRemovingOtherDirectoinSavesBoth() async throws {}

    @Test func testRemovingOtherDirectoinSavesBoth() async throws {}

    @Test func testRemovingProposedFavoriteDisablesAddButton() async throws {}

    @Test func testFavoritingOnlyDirectionPresentsDialogWhenNonBus() async throws {}

    @Test func testFavoritingOnlyDirectionSkipsDialogWhenBus() async throws {}

    @Test func testUnfavoritingOnlyDirectionUpdatesFavoritesWithoutDialog() async throws {}

    @Test func testFavoritingWhenOnlyDirectionIsOppositePresentsDialog() async throws {}
}
