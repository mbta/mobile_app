//
//  StopDetailsViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 6/20/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
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

        let sut = StopDetailsView(
            stopId: stop.id,
            stopFilter: nil,
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            departures: .init(routes: [
                .init(route: routeDefaultSort1, stop: stop, patterns: [], elevatorAlerts: []),
                .init(route: routeDefaultSort0, stop: stop, patterns: [], elevatorAlerts: []),
            ]),
            routeCardData: nil,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init(
                globalRepository: MockGlobalRepository(response: .init(objects: objects))
            )
        )

        ViewHosting.host(view: sut)
        let routePills = try sut.inspect().find(StopDetailsFilterPills.self).findAll(RoutePill.self)
        XCTAssertEqual(2, routePills.count)
        XCTAssertNotNil(try routePills[0].find(text: "Should be first"))
        XCTAssertNotNil(try routePills[1].find(text: "Should be second"))
    }

    @MainActor func testFiltersInSameOrderAsRouteCardData() throws {
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

        let nearbyVM = NearbyViewModel()
        nearbyVM.groupByDirection = true

        let sut = StopDetailsView(
            stopId: stop.id,
            stopFilter: nil,
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            departures: nil,
            routeCardData: [
                .init(
                    lineOrRoute: RouteCardDataLineOrRouteRoute(route: routeDefaultSort1),
                    stopData: [],
                    context: .stopDetailsUnfiltered,
                    at: Date.now.toKotlinInstant()
                ),
                .init(
                    lineOrRoute: RouteCardDataLineOrRouteRoute(route: routeDefaultSort0),
                    stopData: [],
                    context: .stopDetailsUnfiltered,
                    at: Date.now.toKotlinInstant()
                ),
            ],
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: .init(
                globalRepository: MockGlobalRepository(response: .init(objects: objects))
            )
        )

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

        let sut = StopDetailsView(
            stopId: stop.id,
            stopFilter: nil,
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            departures: .init(routes: [
                .init(route: route, stop: stop, patterns: [], elevatorAlerts: []),
            ]),
            routeCardData: nil,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init()
        )

        ViewHosting.host(view: sut)
        XCTAssertNil(try? sut.inspect().find(StopDetailsFilterPills.self))
        XCTAssertNil(try? sut.inspect().find(button: "All"))
    }

    func testShowsElevatorAlertsOnUnfiltered() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route { route in
            route.shortName = "Blue"
        }
        let stop = objects.stop { _ in }
        let alert = objects.alert { alert in
            alert.effect = .elevatorClosure
            alert.header = "Alert header"
        }

        let sut = StopDetailsView(
            stopId: stop.id,
            stopFilter: nil,
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            departures: .init(routes: [
                .init(route: route, stop: stop, patterns: [], elevatorAlerts: [alert]),
            ]),
            routeCardData: nil,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init()
        )

        ViewHosting.host(view: sut)
        XCTAssertNil(try? sut.inspect().find(AlertCard.self))
        XCTAssertNil(try? sut.inspect().find(text: alert.header!))
    }

    func testDisplaysVehicleData() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route { route in
            route.shortName = "57"
        }
        let stop = objects.stop { _ in }
        let routePattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: routePattern) { _ in }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.currentStatus = .inTransitTo
        }

        let sut = StopDetailsView(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: .init(tripId: trip.id, vehicleId: vehicle.id, stopSequence: 1, selectionLock: false),
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            departures: .init(routes: [
                .init(route: route, stop: stop, patterns: [], elevatorAlerts: []),
            ]),
            routeCardData: nil,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init()
        )

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()))
        XCTAssertNotNil(try? sut.inspect().find(TripDetailsView.self))
    }

    func testCloseButtonCloses() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let oldEntry: SheetNavigationStackEntry = .stopDetails(stopId: "oldStop", stopFilter: nil, tripFilter: nil)

        let nearbyVM: NearbyViewModel = .init(
            navigationStack: [oldEntry, .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)]
        )
        let sut = StopDetailsView(
            stopId: stop.id,
            stopFilter: nil,
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            departures: nil,
            routeCardData: nil,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: .init()
        )

        ViewHosting.host(view: sut)
        try? sut.inspect().find(viewWithAccessibilityLabel: "Close").button().tap()
        XCTAssertEqual([oldEntry], nearbyVM.navigationStack)
    }

    func testDebugModeNotShownByDefault() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in
            stop.id = "FAKE_STOP_ID"
        }

        let nearbyVM: NearbyViewModel = .init(
            navigationStack: [.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)],
            showDebugMessages: false
        )
        let sut = StopDetailsView(
            stopId: stop.id,
            stopFilter: nil,
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            departures: nil,
            routeCardData: nil,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: .init()
        )

        ViewHosting.host(view: sut)
        XCTAssertThrowsError(try sut.inspect().find(text: "stop id: FAKE_STOP_ID"))
    }

    func testDebugModeShown() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in
            stop.id = "FAKE_STOP_ID"
        }

        let nearbyVM: NearbyViewModel = .init(
            navigationStack: [.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)],
            showDebugMessages: true
        )
        let sut = StopDetailsView(
            stopId: stop.id,
            stopFilter: nil,
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            departures: nil,
            routeCardData: nil,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: .init()
        )

        ViewHosting.host(view: sut)
        try sut.inspect().findAll(ViewType.Text.self).forEach { view in try print(view.string()) }
        XCTAssertNotNil(try sut.inspect().find(text: "stop id: FAKE_STOP_ID"))
    }
}
