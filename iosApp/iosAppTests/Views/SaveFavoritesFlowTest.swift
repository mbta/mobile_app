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

    let line = LineOrRoute.line(
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
        var updateFavoritesCalledFor: [RouteStopDirection: FavoriteSettings?] = [:]
        var onCloseCalled = false

        let sut = FavoriteConfirmationDialogActions(
            lineOrRoute: line,
            stop: stop,
            favoritesToSave: [direction0: .init()],
            updateFavorites: { updateFavoritesCalledFor = $0 },
            onClose: { onCloseCalled = true },
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        try sut.inspect().find(button: "Add").tap()

        XCTAssertEqual(updateFavoritesCalledFor, [.init(route: line.id, stop: stop.id, direction: 0): .init()])
        XCTAssertTrue(onCloseCalled)
    }

    func testCancelDoesntUpdateFavorites() throws {
        var updateFavoritesCalled = false
        var onCloseCalled = false

        let sut = FavoriteConfirmationDialogActions(
            lineOrRoute: line,
            stop: stop,
            favoritesToSave: [direction0: .init()],
            updateFavorites: { _ in updateFavoritesCalled = true },
            onClose: { onCloseCalled = true },
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        try sut.inspect().find(button: "Cancel").tap()

        XCTAssertTrue(onCloseCalled)
        XCTAssertFalse(updateFavoritesCalled)
    }

    func testAddingOtherDirectionUpdates() throws {
        var updateLocalFavoriteCalledFor: [Direction: FavoriteSettings?] = [:]

        let sut = FavoriteConfirmationDialogContents(
            lineOrRoute: line,
            stop: stop,
            directions: directions,
            selectedDirection: 0,
            context: EditFavoritesContext.favorites,
            favoritesToSave: [direction0: .init(), direction1: nil],
            updateLocalFavorite: { updateLocalFavoriteCalledFor = [$0: $1] },
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        try sut.inspect().findAll(ViewType.Button.self)[1].tap()

        XCTAssertEqual(updateLocalFavoriteCalledFor, [direction1: .init()])
    }

    func testRemovingOtherDirectionUpdates() throws {
        var updateLocalFavoriteCalledFor: [Direction: FavoriteSettings?] = [:]

        let sut = FavoriteConfirmationDialogContents(
            lineOrRoute: line,
            stop: stop,
            directions: directions,
            selectedDirection: 0,
            context: EditFavoritesContext.favorites,
            favoritesToSave: [direction0: .init(), direction1: .init()],
            updateLocalFavorite: { updateLocalFavoriteCalledFor = [$0: $1] },
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        try sut.inspect().findAll(ViewType.Button.self)[1].tap()

        XCTAssertEqual(updateLocalFavoriteCalledFor, [direction1: nil])
    }

    func testRemovingProposedFavoriteDisablesAddButton() throws {
        var updateFavoritesCalled = false
        var onCloseCalled = false

        let sut = FavoriteConfirmationDialogActions(
            lineOrRoute: line,
            stop: stop,
            favoritesToSave: [direction0: nil, direction1: nil],
            updateFavorites: { _ in updateFavoritesCalled = true },
            onClose: { onCloseCalled = true },
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        XCTAssertTrue(try sut.inspect().find(button: "Add").isDisabled())
    }

    @MainActor
    func testFavoritingOnlyDirectionPresentsDialogWhenNonBus() throws {
        var updateFavoritesCalledFor: [RouteStopDirection: FavoriteSettings?] = [:]

        let sut = SaveFavoritesFlow(
            lineOrRoute: line,
            stop: stop,
            directions: [direction0],
            selectedDirection: 0,
            context: EditFavoritesContext.favorites,
            global: .init(objects: .init()),
            isFavorite: { _ in false },
            updateFavorites: { updateFavoritesCalledFor = $0 },
            onClose: {},
            pushNavEntry: { _ in },
        )

        let exp = sut.inspection.inspect(after: 0.5) { view in
            XCTAssertNotNil(try view.find(FavoriteConfirmationDialog.self))
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [exp], timeout: 2)
    }

    func testFavoritingOnlyDirectionSkipsDialogWhenBus() throws {
        var updateFavoritesCalledFor: [RouteStopDirection: FavoriteSettings?] = [:]
        let onCloseExp = XCTestExpectation(description: "On close called")

        let route = ObjectCollectionBuilder().route { route in
            route.type = RouteType.bus
        }

        let sut = SaveFavoritesFlow(
            lineOrRoute: LineOrRoute.route(route),
            stop: stop,
            directions: [direction0],
            selectedDirection: 0,
            context: EditFavoritesContext.favorites,
            global: .init(objects: .init()),
            isFavorite: { _ in false },
            updateFavorites: { updateFavoritesCalledFor = $0 },
            onClose: { onCloseExp.fulfill() },
            pushNavEntry: { _ in },
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [onCloseExp], timeout: 2)
        XCTAssertEqual(updateFavoritesCalledFor, [.init(route: route.id, stop: stop.id, direction: 0): .init()])
    }

    func testUnfavoritingOnlyDirectionUpdatesFavoritesWithoutDialog() throws {
        var updateFavoritesCalledFor: [RouteStopDirection: FavoriteSettings?] = [:]
        let onCloseExp = XCTestExpectation(description: "On close called")

        let route = ObjectCollectionBuilder().route { route in
            route.type = RouteType.bus
        }

        let sut = SaveFavoritesFlow(
            lineOrRoute: LineOrRoute.route(route),
            stop: stop,
            directions: [direction0],
            selectedDirection: 0,
            context: EditFavoritesContext.favorites,
            global: .init(objects: .init()),
            isFavorite: { _ in true },
            updateFavorites: { updateFavoritesCalledFor = $0 },
            onClose: { onCloseExp.fulfill() },
            pushNavEntry: { _ in },
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [onCloseExp], timeout: 2)
        XCTAssertEqual(updateFavoritesCalledFor, [.init(route: route.id, stop: stop.id, direction: 0): nil])
    }

    @MainActor
    func testFavoritingWhenOnlyDirectionIsOppositePresentsDialog() throws {
        let sut = SaveFavoritesFlow(
            lineOrRoute: line,
            stop: stop,
            directions: [direction0],
            selectedDirection: 1,
            context: EditFavoritesContext.favorites,
            global: .init(objects: .init()),
            isFavorite: { _ in false },
            updateFavorites: { _ in },
            onClose: {},
            pushNavEntry: { _ in }
        )

        let exp = sut.inspection.inspect(after: 0.5) { view in
            XCTAssertNotNil(try view.find(FavoriteConfirmationDialog.self))
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [exp], timeout: 2)
    }

    func testFavoritingWhenOnlyDirectionHasDisclaimer() throws {
        let sut = FavoriteConfirmationDialogContents(
            lineOrRoute: line,
            stop: stop,
            directions: [direction0],
            selectedDirection: 1,
            context: .favorites,
            favoritesToSave: [direction0: nil],
            updateLocalFavorite: { _, _ in }
        )

        try XCTAssertNotNil(sut.inspect().find(text: "Westbound service only"))
    }

    func testFavoritingWhenDropOffOnlyHasDisclaimer() throws {
        let sut = FavoriteConfirmationDialogContents(
            lineOrRoute: line,
            stop: stop,
            directions: [],
            selectedDirection: 0,
            context: .favorites,
            favoritesToSave: [direction0: nil],
            updateLocalFavorite: { _, _ in }
        )

        try XCTAssertNotNil(sut.inspect().find(text: "This stop is drop-off only"))
    }

    func testFavoritingDisplaysToast() throws {
        var updateFavoritesCalledFor: [RouteStopDirection: FavoriteSettings?] = [:]
        let onToastExp = XCTestExpectation(description: "Toast displayed with expected text")

        let route = ObjectCollectionBuilder().route { route in
            route.type = RouteType.bus
        }

        let toastVM = MockToastViewModel()
        toastVM.onShowToast = { toast in
            XCTAssertEqual("Added to Favorites", toast.message)
            onToastExp.fulfill()
        }

        let sut = SaveFavoritesFlow(
            lineOrRoute: LineOrRoute.route(route),
            stop: stop,
            directions: [direction0],
            selectedDirection: 0,
            context: EditFavoritesContext.favorites,
            global: .init(objects: .init()),
            isFavorite: { _ in false },
            updateFavorites: { updateFavoritesCalledFor = $0 },
            onClose: {},
            pushNavEntry: { _ in },
            toastVM: toastVM,
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [onToastExp], timeout: 2)
        XCTAssertEqual(updateFavoritesCalledFor, [.init(route: route.id, stop: stop.id, direction: 0): .init()])
    }

    func testOpensSavePageWhenNotificationsFlagIsOnForSingleDirection() throws {
        let onCloseExp = XCTestExpectation(description: "On close called")
        let onPushNavExp = XCTestExpectation(description: "Navigation pushed")
        var pushedNav: SheetNavigationStackEntry?

        let route = ObjectCollectionBuilder().route { route in
            route.type = RouteType.bus
        }

        let sut = SaveFavoritesFlow(
            lineOrRoute: LineOrRoute.route(route),
            stop: stop,
            directions: [direction0],
            selectedDirection: 0,
            context: EditFavoritesContext.favorites,
            global: .init(objects: .init()),
            isFavorite: { _ in false },
            updateFavorites: { _ in XCTFail("Favorites should not be updated automatically") },
            onClose: { onCloseExp.fulfill() },
            pushNavEntry: { entry in
                pushedNav = entry
                onPushNavExp.fulfill()
            },
        )

        ViewHosting.host(view: sut.withFixedSettings([.notifications: true]))

        wait(for: [onCloseExp, onPushNavExp], timeout: 2)
        XCTAssertEqual(
            pushedNav,
            .saveFavorite(routeId: route.id, stopId: stop.id, selectedDirection: 0, context: .favorites)
        )
    }

    @MainActor
    func testOpensSavePageWhenNotificationsFlagIsOnForMultiDirection() throws {
        let onCloseExp = XCTestExpectation(description: "On close called")
        let onPushNavExp = XCTestExpectation(description: "Navigation pushed")
        var pushedNav: SheetNavigationStackEntry?

        let route = ObjectCollectionBuilder().route { route in
            route.type = RouteType.bus
        }

        let sut = SaveFavoritesFlow(
            lineOrRoute: line,
            stop: stop,
            directions: [direction0, direction1],
            selectedDirection: 1,
            context: EditFavoritesContext.stopDetails,
            global: .init(objects: .init()),
            isFavorite: { _ in false },
            updateFavorites: { _ in XCTFail("Favorites should not be updated automatically") },
            onClose: { onCloseExp.fulfill() },
            pushNavEntry: { entry in
                pushedNav = entry
                onPushNavExp.fulfill()
            },
        )

        ViewHosting.host(view: sut.withFixedSettings([.notifications: true]))

        wait(for: [onCloseExp, onPushNavExp], timeout: 2)
        XCTAssertEqual(
            pushedNav,
            .saveFavorite(routeId: line.id, stopId: stop.id, selectedDirection: 1, context: .stopDetails)
        )
    }
}
