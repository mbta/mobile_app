//
//  EditFavoritesPageTests.swift
//  iosApp
//
//  Created by Kayla Brady on 7/11/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class EditFavoritesPageTests: XCTestCase {
    @MainActor func testHeader() throws {
        let objects = ObjectCollectionBuilder()
        let globalData = GlobalResponse(objects: objects)
        let favoritesVM = MockFavoritesViewModel(initialState: .init(
            awaitingPredictionsAfterBackground: false,
            favorites: [],
            routeCardData: [],
            staticRouteCardData: []
        ))

        var onCloseCalled = false
        let sut = EditFavoritesPage(viewModel: favoritesVM, onClose: { onCloseCalled = true }, errorBannerVM: .init())

        XCTAssertNotNil(try sut.inspect().find(text: "Edit Favorites"))
        try sut.inspect().find(button: "Done").tap()
        XCTAssertTrue(onCloseCalled)
    }
}
