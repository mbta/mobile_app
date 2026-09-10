//
//  SaveFavoritePageTests.swift
//  iosApp
//
//  Created by esimon on 11/18/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class SaveFavoritePageTests: XCTestCase {
    @MainActor func testDisplaysSelectedDirection() {
        let objects = TestData.clone()
        let route = objects.getRoute(id: "Red")
        let stop = objects.getStop(id: "place-pktrm")

        loadKoinMocks(objects: objects)

        let sut = SaveFavoritePage(
            routeId: route.id,
            stopId: stop.id,
            initialSelectedDirection: 1,
            context: .favorites,
            updateFavorites: { _ in },
            navCallbacks: .init(onBack: nil, onClose: nil, backButtonPresentation: .floating),
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        sut.inspection.inspect(after: 0.1) { view in
            XCTAssertNotNil(try view.find(text: "Alewife"))
            XCTAssertNotNil(try view.find(text: "Northbound to"))
        }
    }

    @MainActor func testTogglesSelectedDirection() {
        let objects = TestData.clone()
        let route = objects.getRoute(id: "Red")
        let stop = objects.getStop(id: "place-pktrm")

        loadKoinMocks(objects: objects)

        let sut = SaveFavoritePage(
            routeId: route.id,
            stopId: stop.id,
            initialSelectedDirection: 1,
            context: .favorites,
            updateFavorites: { _ in },
            navCallbacks: .init(onBack: nil, onClose: nil, backButtonPresentation: .floating),
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        sut.inspection.inspect(after: 1) { view in
            XCTAssertNotNil(try sut.inspect().find(text: "Alewife"))
            XCTAssertNotNil(try sut.inspect().find(text: "Northbound to"))

            try? sut.inspect().find(ActionButton.self).find(ViewType.Button.self).tap()

            XCTAssertNotNil(try view.find(text: "Ashmont/Braintree"))
            XCTAssertNotNil(try view.find(text: "Southbound to"))
        }
    }

    @MainActor func testTogglesSelectedDirectionWithSavedDirection() {
        let objects = TestData.clone()
        let route = objects.getRoute(id: "Red")
        let stop = objects.getStop(id: "place-pktrm")

        let repositories = MockRepositories()
        repositories.useObjects(objects: objects)
        repositories.favorites = MockFavoritesRepository(
            favorites: Favorites(routeStopDirection:
                [RouteStopDirection(route: route.id, stop: stop.id, direction: 1): FavoriteSettings()])
        )

        loadKoinMocks(repositories: repositories)

        let sut = SaveFavoritePage(
            routeId: route.id,
            stopId: stop.id,
            initialSelectedDirection: 1,
            context: .favorites,
            updateFavorites: { _ in },
            navCallbacks: .init(onBack: nil, onClose: nil, backButtonPresentation: .floating),
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        sut.inspection.inspect(after: 1) { view in
            XCTAssertNotNil(try sut.inspect().find(text: "Alewife"))
            XCTAssertNotNil(try sut.inspect().find(text: "Northbound to"))

            try? sut.inspect().find(ActionButton.self).find(ViewType.Button.self).tap()

            XCTAssertNotNil(try view.find(text: "Ashmont/Braintree"))
            XCTAssertNotNil(try view.find(text: "Southbound to"))

            XCTAssertNotNil(try view.find(text: "Remove from Favorites"))

            try? sut.inspect().find(ActionButton.self).find(ViewType.Button.self).tap()

            XCTAssertNotNil(try sut.inspect().find(text: "Alewife"))
            XCTAssertNotNil(try sut.inspect().find(text: "Northbound to"))
        }
    }

    @MainActor func testsLoadsNotificationSettings() {
        let objects = TestData.clone()
        let route = objects.getRoute(id: "Orange")
        let stop = objects.getStop(id: "place-welln")
        var updatedFavorites: [RouteStopDirection: FavoriteSettings?]?

        loadKoinMocks(objects: objects)

        var settingsLoadedCalled = false

        let notificationSettingsVM: MockNotificationSettingsViewModel = .init(initialState: .init(
            settings: FavoriteSettings.Notifications.companion.disabled,
            selectedPreset: nil
        ))
        notificationSettingsVM.onLoadSavedSettings = { _ in settingsLoadedCalled = true }

        let sut = SaveFavoritePage(
            routeId: route.id,
            stopId: stop.id,
            initialSelectedDirection: 0,
            context: .stopDetails,
            updateFavorites: { updatedFavorites = $0 },
            navCallbacks: .init(onBack: nil, onClose: nil, backButtonPresentation: .floating),
            notificationSettingsVM: notificationSettingsVM
        )

        let exp1 = sut.inspection.inspect(after: 1) { view in
            XCTAssertNotNil(try view.find(text: "Add Favorite"))
            XCTAssertTrue(settingsLoadedCalled)
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [exp1], timeout: 5)
    }

    @MainActor func testCallsSavedSettingsOnSave() {
        let objects = TestData.clone()
        let route = objects.getRoute(id: "Orange")
        let stop = objects.getStop(id: "place-welln")

        loadKoinMocks(objects: objects)

        var savedSettings = false

        let notificationSettingsVM: MockNotificationSettingsViewModel = .init(initialState: .init(
            settings: FavoriteSettings.Notifications.companion.disabled,
            selectedPreset: nil
        ))
        notificationSettingsVM.onSavedSettings = { savedSettings = true }

        let sut = SaveFavoritePage(
            routeId: route.id,
            stopId: stop.id,
            initialSelectedDirection: 0,
            context: .stopDetails,
            updateFavorites: { _ in },
            navCallbacks: .init(onBack: nil, onClose: nil, backButtonPresentation: .floating),
            notificationSettingsVM: notificationSettingsVM
        )

        let exp1 = sut.inspection.inspect(after: 1) { view in
            try view.find(button: "Save").tap()
            XCTAssertTrue(savedSettings)
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [exp1], timeout: 5)
    }

    @MainActor func testClearsLoadedSettingsOnCancel() {
        let objects = TestData.clone()
        let route = objects.getRoute(id: "Orange")
        let stop = objects.getStop(id: "place-welln")

        loadKoinMocks(objects: objects)

        var savedSettingsCleared = false

        let notificationSettingsVM: MockNotificationSettingsViewModel = .init(initialState: .init(
            settings: FavoriteSettings.Notifications.companion.disabled,
            selectedPreset: nil
        ))
        notificationSettingsVM.onLoadSavedSettings = { savedSettingsCleared = $0 == nil }

        let sut = SaveFavoritePage(
            routeId: route.id,
            stopId: stop.id,
            initialSelectedDirection: 0,
            context: .stopDetails,
            updateFavorites: { _ in },
            navCallbacks: .init(onBack: nil, onClose: nil, backButtonPresentation: .floating),
            notificationSettingsVM: notificationSettingsVM
        )

        let exp1 = sut.inspection.inspect(after: 1) { view in
            try view.find(button: "Cancel").tap()
            XCTAssertTrue(savedSettingsCleared)
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [exp1], timeout: 5)
    }
}
