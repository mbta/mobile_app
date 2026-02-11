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
            nearbyVM: .init(),
        )

        ViewHosting.host(view: sut)

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
            nearbyVM: .init(),
        )

        ViewHosting.host(view: sut)

        sut.inspection.inspect(after: 0.1) { view in
            XCTAssertNotNil(try sut.inspect().find(text: "Alewife"))
            XCTAssertNotNil(try sut.inspect().find(text: "Northbound to"))

            try? sut.inspect().find(ActionButton.self).implicitAnyView().button().tap()

            XCTAssertNotNil(try view.find(text: "Ashmont/Braintree"))
            XCTAssertNotNil(try view.find(text: "Southbound to"))
        }
    }

    @MainActor func testNotifications() {
        let objects = TestData.clone()
        let route = objects.getRoute(id: "Orange")
        let stop = objects.getStop(id: "place-welln")
        var updatedFavorites: [RouteStopDirection: FavoriteSettings?]?

        loadKoinMocks(objects: objects)

        let sut = SaveFavoritePage(
            routeId: route.id,
            stopId: stop.id,
            initialSelectedDirection: 0,
            context: .stopDetails,
            updateFavorites: { updatedFavorites = $0 },
            navCallbacks: .init(onBack: nil, onClose: nil, backButtonPresentation: .floating),
            nearbyVM: .init(),
        )

        let exp1 = sut.inspection.inspect(after: 1) { view in
            XCTAssertNotNil(try view.find(text: "Add Favorite"))
            try view.find(text: "Get disruption notifications").find(ViewType.Toggle.self, relation: .parent).tap()
        }

        let exp2 = sut.inspection.inspect(after: 2) { view in
            try view.find(button: "Save").tap()
            XCTAssertEqual(updatedFavorites, [
                .init(route: route.id, stop: stop.id, direction: 0): .init(notifications: .init(
                    enabled: true,
                    windows: []
                )),
            ])
        }

        ViewHosting.host(view: sut)

        wait(for: [exp1, exp2], timeout: 5)
    }
}
