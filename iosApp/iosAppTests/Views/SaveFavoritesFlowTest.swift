//
//  SaveFavoritesFlowTest.swift
//  iosAppTests
//
//  Created by Kayla Brady on 6/30/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp

import Combine
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class SaveFavoritesFlowTest: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    let line = RouteCardData.LineOrRoute.line(
        TestData.getLine(id: "line-Green"),
        Set([
            TestData.getRoute(id: "Green-B"),
            TestData.getRoute(id: "Green-C"),
            TestData.getRoute(id: "Green-D"),
            TestData.getRoute(id: "Green-E"),
        ])
    )

    let stop = TestData.getStop(id: "place-boyls")
    let direction0 = Direction(name: "West", destination: "Copley & West", id: 0)
    let direction1 = Direction(name: "East", destination: "Park St & North", id: 1)
    var directions: [Direction] { [direction0, direction1] }

    func testWithoutTappingAnyButtonSavesProposedChanges() throws {
        var updateFavoritesCalledFor: [RouteStopDirection: Bool] = [:]
        var onCloseCalled = false

        let sut = FavoriteConfirmationDialogActions(lineOrRoute: line,
                                                    stop: stop,
                                                    favoritesToSave: [direction0: true],
                                                    updateFavorites: { updateFavoritesCalledFor = $0 },
                                                    onClose: { onCloseCalled = true })

        try sut.inspect().find(button: "Add").tap()

        XCTAssertEqual(updateFavoritesCalledFor, [.init(route: line.id, stop: stop.id, direction: 0): true])
        XCTAssertTrue(onCloseCalled)
    }

    func testCancelDoesntUpdateFavorites() throws {
        var updateFavoritesCalled = false
        var onCloseCalled = false

        let sut = FavoriteConfirmationDialogActions(lineOrRoute: line,
                                                    stop: stop,
                                                    favoritesToSave: [direction0: true],
                                                    updateFavorites: { _ in updateFavoritesCalled = true },
                                                    onClose: { onCloseCalled = true })

        try sut.inspect().find(button: "Cancel").tap()

        XCTAssertTrue(onCloseCalled)
        XCTAssertFalse(updateFavoritesCalled)
    }

    func testAddingOtherDirectionUpdates() throws {
        var updateLocalFavoriteCalledFor: [Direction: Bool] = [:]
        var onCloseCalled = false

        let sut = FavoriteConfirmationDialogContents(lineOrRoute: line,
                                                     stop: stop,
                                                     directions: directions,
                                                     selectedDirection: 0,
                                                     context: SaveFavoritesContext.favorites,
                                                     favoritesToSave: [direction0: true, direction1: false],
                                                     updateLocalFavorite: { updateLocalFavoriteCalledFor = [$0: $1] })

        print(Inspector.print(sut))
        try sut.inspect().findAll(ViewType.Button.self)[1].tap()

        XCTAssertEqual(updateLocalFavoriteCalledFor, [direction1: true])
    }

    func testRemovingOtherDirectionUpdates() throws {
        var updateLocalFavoriteCalledFor: [Direction: Bool] = [:]
        var onCloseCalled = false

        let sut = FavoriteConfirmationDialogContents(lineOrRoute: line,
                                                     stop: stop,
                                                     directions: directions,
                                                     selectedDirection: 0,
                                                     context: SaveFavoritesContext.favorites,
                                                     favoritesToSave: [direction0: true, direction1: true],
                                                     updateLocalFavorite: { updateLocalFavoriteCalledFor = [$0: $1] })

        print(Inspector.print(sut))
        try sut.inspect().findAll(ViewType.Button.self)[1].tap()

        XCTAssertEqual(updateLocalFavoriteCalledFor, [direction1: false])
    }

    func testRemovingProposedFavoriteDisablesAddButton() throws {
        var updateFavoritesCalled = false
        var onCloseCalled = false

        let sut = FavoriteConfirmationDialogActions(lineOrRoute: line,
                                                    stop: stop,
                                                    favoritesToSave: [direction0: false, direction1: false],
                                                    updateFavorites: { _ in updateFavoritesCalled = true },
                                                    onClose: { onCloseCalled = true })

        XCTAssertTrue(try sut.inspect().find(button: "Add").isDisabled())
    }

    @MainActor
    func testFavoritingOnlyDirectionPresentsDialogWhenNonBus() throws {
        var updateFavoritesCalledFor: [RouteStopDirection: Bool] = [:]

        let sut = SaveFavoritesFlow(lineOrRoute: line,
                                    stop: stop,
                                    directions: [direction0],
                                    selectedDirection: 0,
                                    context: SaveFavoritesContext.favorites,
                                    isFavorite: { _ in false },
                                    updateFavorites: { updateFavoritesCalledFor = $0 },
                                    onClose: {})

        let exp = sut.inspection.inspect(after: 0.5) { view in
            XCTAssertNotNil(try view.find(FavoriteConfirmationDialog.self))
        }

        ViewHosting.host(view: sut)

        wait(for: [exp], timeout: 2)
    }

    func testFavoritingOnlyDirectionSkipsDialogWhenBus() throws {
        var updateFavoritesCalledFor: [RouteStopDirection: Bool] = [:]
        let onCloseExp = XCTestExpectation(description: "On close called")

        let route = ObjectCollectionBuilder().route { route in
            route.type = RouteType.bus
        }

        let sut = SaveFavoritesFlow(lineOrRoute: RouteCardData.LineOrRoute.route(route),
                                    stop: stop,
                                    directions: [direction0],
                                    selectedDirection: 0,
                                    context: SaveFavoritesContext.favorites,
                                    isFavorite: { _ in false },
                                    updateFavorites: { updateFavoritesCalledFor = $0 },
                                    onClose: { onCloseExp.fulfill() })

        ViewHosting.host(view: sut)

        wait(for: [onCloseExp], timeout: 2)
        XCTAssertEqual(updateFavoritesCalledFor, [.init(route: route.id, stop: stop.id, direction: 0): true])
    }

    func testUnfavoritingOnlyDirectionUpdatesFavoritesWithoutDialog() throws {
        var updateFavoritesCalledFor: [RouteStopDirection: Bool] = [:]
        let onCloseExp = XCTestExpectation(description: "On close called")

        let route = ObjectCollectionBuilder().route { route in
            route.type = RouteType.bus
        }

        let sut = SaveFavoritesFlow(lineOrRoute: RouteCardData.LineOrRoute.route(route),
                                    stop: stop,
                                    directions: [direction0],
                                    selectedDirection: 0,
                                    context: SaveFavoritesContext.favorites,
                                    isFavorite: { _ in true },
                                    updateFavorites: { updateFavoritesCalledFor = $0 },
                                    onClose: { onCloseExp.fulfill() })

        ViewHosting.host(view: sut)

        wait(for: [onCloseExp], timeout: 2)
        XCTAssertEqual(updateFavoritesCalledFor, [.init(route: route.id, stop: stop.id, direction: 0): false])
    }

    @MainActor
    func testFavoritingWhenOnlyDirectionIsOppositePresentsDialog() throws {
        var updateFavoritesCalledFor: [RouteStopDirection: Bool] = [:]

        let sut = SaveFavoritesFlow(lineOrRoute: line,
                                    stop: stop,
                                    directions: [direction0],
                                    selectedDirection: 1,
                                    context: SaveFavoritesContext.favorites,
                                    isFavorite: { _ in false },
                                    updateFavorites: { updateFavoritesCalledFor = $0 },
                                    onClose: {})

        let exp = sut.inspection.inspect(after: 0.5) { view in
            XCTAssertNotNil(try view.find(FavoriteConfirmationDialog.self))
        }

        ViewHosting.host(view: sut)

        wait(for: [exp], timeout: 2)
    }

    func testFavoritingWhenOnlyDirectionHasDisclaimer() throws {
        var updateFavoritesCalledFor: [RouteStopDirection: Bool] = [:]

        let sut = FavoriteConfirmationDialogContents(lineOrRoute: line,
                                                     stop: stop,
                                                     directions: [direction0],
                                                     selectedDirection: 1,
                                                     context: .favorites,
                                                     favoritesToSave: [direction0: false],
                                                     updateLocalFavorite: { _, _ in })

        try XCTAssertNotNil(sut.inspect().find(text: "Westbound service only"))
    }

    func testFavoritingWhenDropOffOnlyHasDisclaimer() throws {
        var updateFavoritesCalledFor: [RouteStopDirection: Bool] = [:]

        let sut = FavoriteConfirmationDialogContents(lineOrRoute: line,
                                                     stop: stop,
                                                     directions: [],
                                                     selectedDirection: 0,
                                                     context: .favorites,
                                                     favoritesToSave: [direction0: false],
                                                     updateLocalFavorite: { _, _ in })

        try XCTAssertNotNil(sut.inspect().find(text: "This stop is drop-off only"))
    }
}
