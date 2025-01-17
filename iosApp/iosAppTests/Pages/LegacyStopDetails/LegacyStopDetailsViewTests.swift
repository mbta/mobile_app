//
//  LegacyStopDetailsViewTests.swift
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

final class LegacyStopDetailsViewTests: XCTestCase {
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

        let sut = LegacyStopDetailsView(stop: stop,
                                        filter: nil,
                                        setFilter: { _ in },
                                        departures: .init(routes: [
                                            .init(
                                                route: routeDefaultSort1, stop: stop,
                                                patterns: [], elevatorAlerts: []
                                            ),
                                            .init(
                                                route: routeDefaultSort0, stop: stop,
                                                patterns: [], elevatorAlerts: []
                                            ),
                                        ]),
                                        errorBannerVM: .init(),
                                        nearbyVM: .init(),
                                        now: Date.now,
                                        pinnedRoutes: [], togglePinnedRoute: { _ in })

        ViewHosting.host(view: sut)
        let routePills = try sut.inspect().find(StopDetailsFilterPills.self).findAll(RoutePill.self)
        XCTAssertEqual(2, routePills.count)
        XCTAssertNotNil(try routePills[0].find(text: "Should be first"))
        XCTAssertNotNil(try routePills[1].find(text: "Should be second"))
    }

    func testSkipsPillsIfOneRoute() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route { route in
            route.shortName = "57"
        }
        let stop = objects.stop { _ in }

        let sut = LegacyStopDetailsView(stop: stop,
                                        filter: nil,
                                        setFilter: { _ in },
                                        departures: .init(routes: [
                                            .init(route: route, stop: stop, patterns: [], elevatorAlerts: []),
                                        ]),
                                        errorBannerVM: .init(),
                                        nearbyVM: .init(),
                                        now: Date.now,
                                        pinnedRoutes: [], togglePinnedRoute: { _ in })

        ViewHosting.host(view: sut)
        XCTAssertNil(try? sut.inspect().find(StopDetailsFilterPills.self))
        XCTAssertNil(try? sut.inspect().find(button: "All"))
    }

    func testCloseButtonCloses() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let nearbyVM: NearbyViewModel = .init(navigationStack: [.legacyStopDetails(stop, nil)])
        let sut = LegacyStopDetailsView(
            stop: stop,
            filter: nil,
            setFilter: { _ in },
            departures: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            now: Date.now,
            pinnedRoutes: [], togglePinnedRoute: { _ in }
        )

        ViewHosting.host(view: sut)
        try? sut.inspect().find(viewWithAccessibilityLabel: "Close").button().tap()
        XCTAssert(nearbyVM.navigationStack.isEmpty)
    }

    func testBackButtonGoesBack() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let initialNavStack: [SheetNavigationStackEntry] = [
            .legacyStopDetails(stop, nil),
            .tripDetails(tripId: "", vehicleId: "", target: nil, routeId: "", directionId: 0),
            .legacyStopDetails(stop, nil),
        ]
        let nearbyVM: NearbyViewModel = .init(navigationStack: initialNavStack)
        let sut = LegacyStopDetailsView(
            stop: stop,
            filter: nil,
            setFilter: { _ in },
            departures: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            now: Date.now,
            pinnedRoutes: [], togglePinnedRoute: { _ in }
        )

        ViewHosting.host(view: sut)
        try? sut.inspect().find(viewWithAccessibilityLabel: "Back").button().tap()
        XCTAssertEqual(initialNavStack.dropLast(), nearbyVM.navigationStack)
    }

    func testBackButtonHiddenWhenNearbyIsBack() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let nearbyVM: NearbyViewModel = .init(navigationStack: [.legacyStopDetails(stop, nil)])
        let sut = LegacyStopDetailsView(
            stop: stop,
            filter: nil,
            setFilter: { _ in },
            departures: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            now: Date.now,
            pinnedRoutes: [], togglePinnedRoute: { _ in }
        )

        ViewHosting.host(view: sut)
        XCTAssertNil(try? sut.inspect().find(viewWithAccessibilityLabel: "Back"))
    }

    func testDebugModeNotShownByDefault() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in
            stop.id = "FAKE_STOP_ID"
        }

        let nearbyVM: NearbyViewModel = .init(
            navigationStack: [.legacyStopDetails(stop, nil)],
            showDebugMessages: false
        )
        let sut = LegacyStopDetailsView(
            stop: stop,
            filter: nil,
            setFilter: { _ in },
            departures: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            now: Date.now,
            pinnedRoutes: [], togglePinnedRoute: { _ in }
        )

        ViewHosting.host(view: sut)
        XCTAssertThrowsError(try sut.inspect().find(text: "stop id: FAKE_STOP_ID"))
    }

    func testDebugModeShown() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in
            stop.id = "FAKE_STOP_ID"
        }

        let nearbyVM: NearbyViewModel = .init(navigationStack: [.legacyStopDetails(stop, nil)], showDebugMessages: true)
        let sut = LegacyStopDetailsView(
            stop: stop,
            filter: nil,
            setFilter: { _ in },
            departures: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            now: Date.now,
            pinnedRoutes: [], togglePinnedRoute: { _ in }
        )

        ViewHosting.host(view: sut)
        XCTAssertNotNil(try sut.inspect().find(text: "stop id: FAKE_STOP_ID"))
    }
}
