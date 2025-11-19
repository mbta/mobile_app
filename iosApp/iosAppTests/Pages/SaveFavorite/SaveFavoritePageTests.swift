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
}
