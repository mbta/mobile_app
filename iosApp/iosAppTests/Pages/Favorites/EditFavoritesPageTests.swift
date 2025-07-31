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
            staticRouteCardData: [],
            loadedLocation: nil,
        ))

        var onCloseCalled = false
        let sut = EditFavoritesPage(viewModel: favoritesVM, onClose: { onCloseCalled = true }, errorBannerVM: .init())

        XCTAssertNotNil(try sut.inspect().find(text: "Edit Favorites"))
        try sut.inspect().find(button: "Done").tap()
        XCTAssertTrue(onCloseCalled)
    }

    @MainActor func testDeleteFavorite() throws {
        let objects = TestData.clone()
        let globalData = GlobalResponse(objects: objects)

        let route15: Route = objects.getRoute(id: "15")
        let stop15 = objects.getStop(id: "17863")

        let route67: Route = objects.getRoute(id: "67")
        let stop67 = objects.getStop(id: "141")

        let updateFavoritesExp = XCTestExpectation(description: "Update favorites called for route 15 only")

        let favoritesVM = MockFavoritesViewModel(initialState: .init(
            awaitingPredictionsAfterBackground: false,
            favorites: [],
            routeCardData: [],
            staticRouteCardData: [
                .init(lineOrRoute: .route(route15),
                      stopData: [.init(route: route15,
                                       stop: stop15,
                                       data: [.init(lineOrRoute: .route(route15),
                                                    stop: stop15,
                                                    directionId: 0,
                                                    routePatterns: [],
                                                    stopIds: [],
                                                    upcomingTrips: [],
                                                    alertsHere: [],
                                                    allDataLoaded: true,
                                                    hasSchedulesToday: true,
                                                    alertsDownstream: [],
                                                    context: .favorites)],
                                       globalData: globalData)],
                      at: EasternTimeInstant.now()),
                .init(lineOrRoute: .route(route67),
                      stopData: [.init(route: route67,
                                       stop: stop67,
                                       data: [.init(lineOrRoute: .route(route67),
                                                    stop: stop67,
                                                    directionId: 0,
                                                    routePatterns: [],
                                                    stopIds: [],
                                                    upcomingTrips: [],
                                                    alertsHere: [],
                                                    allDataLoaded: true,
                                                    hasSchedulesToday: true,
                                                    alertsDownstream: [],
                                                    context: .favorites)],
                                       globalData: globalData)],
                      at: EasternTimeInstant.now()),
            ],
            loadedLocation: nil,
        ))

        favoritesVM.onUpdateFavorites = { newFavorites in
            if newFavorites == [RouteStopDirection(route: route15.id, stop: stop15.id, direction: 0): false] {
                updateFavoritesExp.fulfill()
            }
        }

        let sut = EditFavoritesPage(viewModel: favoritesVM, onClose: {}, errorBannerVM: .init())

        let exp = sut.inspection.inspect(after: 0.2) { view in

            try view.findAll(DeleteButton.self)[0].find(ViewType.Button.self).tap()
        }
        ViewHosting.host(view: sut.withFixedSettings([.enhancedFavorites: true]))
        wait(for: [exp, updateFavoritesExp], timeout: 2)
    }
}
