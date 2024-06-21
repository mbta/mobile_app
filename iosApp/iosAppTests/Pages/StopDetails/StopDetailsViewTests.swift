//
//  StopDetailsViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 6/20/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class StopDetailsViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testFiltersInSameOrderAsDepartures() throws {
        let objects = ObjectCollectionBuilder()
        let routeDefaultSort0 = objects.route { route in
            route.sortOrder = 0
            route.type = .bus
            route.shortName = "Should be second"
        }
        let routeDefaultSort1 = objects.route { route in
            route.sortOrder = 1
            route.type = .bus
            route.shortName = "Should be first"
        }
        let stop = objects.stop { _ in }

        let sut = StopDetailsView(globalFetcher: .init(backend: IdleBackend()),
                                  stop: stop,
                                  filter: .constant(nil),
                                  nearbyVM: .init(departures: .init(routes: [
                                      .init(route: routeDefaultSort1, stop: stop, patternsByHeadsign: []),
                                      .init(route: routeDefaultSort0, stop: stop, patternsByHeadsign: []),
                                  ])),
                                  pinnedRoutes: [], togglePinnedRoute: { _ in })

        ViewHosting.host(view: sut)
        let routePills = try sut.inspect().find(StopDetailsRoutePills.self).findAll(RoutePill.self)
        XCTAssertEqual(2, routePills.count)
        XCTAssertNotNil(try routePills[0].find(text: "Should be first"))
        XCTAssertNotNil(try routePills[1].find(text: "Should be second"))
    }
}
