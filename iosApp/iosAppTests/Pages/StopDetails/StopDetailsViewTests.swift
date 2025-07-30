//
//  StopDetailsViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 6/20/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp

import Combine
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class StopDetailsViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
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

        let sut = StopDetailsView(
            stopId: stop.id,
            stopFilter: nil,
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            routeCardData: [
                .init(
                    lineOrRoute: .route(routeDefaultSort1),
                    stopData: [],
                    at: EasternTimeInstant.now()
                ),
                .init(
                    lineOrRoute: .route(routeDefaultSort0),
                    stopData: [],
                    at: EasternTimeInstant.now()
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

        ViewHosting.host(view: sut.withFixedSettings([:]))
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
            routeCardData: [.init(
                lineOrRoute: .route(route),
                stopData: [.init(
                    route: route,
                    stop: stop,
                    data: [],
                    globalData: .init(objects: objects)
                )],
                at: EasternTimeInstant.now()
            )],
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init()
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
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
            routeCardData: [.init(
                lineOrRoute: .route(route),
                stopData: [.init(
                    route: route, stop: stop,
                    data: [.init(
                        lineOrRoute: .route(route),
                        stop: stop,
                        directionId: 0,
                        routePatterns: [],
                        stopIds: [],
                        upcomingTrips: [],
                        alertsHere: [alert],
                        allDataLoaded: true,
                        hasSchedulesToday: true,
                        alertsDownstream: [],
                        context: .stopDetailsUnfiltered
                    )],
                    globalData: .init(objects: objects)
                )],
                at: EasternTimeInstant.now()
            )],
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init()
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        XCTAssertNil(try? sut.inspect().find(AlertCard.self))
        XCTAssertNil(try? sut.inspect().find(text: alert.header!))
    }

    func testDisplaysVehicleData() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let route = objects.route { route in
            route.shortName = "57"
        }
        let stop = objects.stop { _ in }
        let routePattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: routePattern) { _ in }
        let prediction = objects.prediction { prediction in
            prediction.departureTime = now.plus(seconds: 100)
        }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.currentStatus = .inTransitTo
        }

        let leaf = RouteCardData.Leaf(
            lineOrRoute: .route(route),
            stop: stop,
            directionId: 0,
            routePatterns: [routePattern],
            stopIds: Set([stop.id]),
            upcomingTrips: [.init(trip: trip, prediction: prediction)],
            alertsHere: [], allDataLoaded: true,
            hasSchedulesToday: true, alertsDownstream: [],
            context: .stopDetailsFiltered
        )
        let stopData = RouteCardData.RouteStopData(
            lineOrRoute: .route(route),
            stop: stop,
            directions: [
                .init(directionId: 0, route: route),
                .init(directionId: 1, route: route),
            ],
            data: [leaf]
        )
        let routeData = RouteCardData(
            lineOrRoute: .route(route),
            stopData: [stopData],
            at: now
        )

        let sut = StopDetailsView(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: .init(tripId: trip.id, vehicleId: vehicle.id, stopSequence: 1, selectionLock: false),
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            routeCardData: [routeData],
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init()
        )

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([:]))
        XCTAssertNotNil(try? sut.inspect().find(TripDetailsView.self))
    }

    @MainActor func testCloseButtonCloses() throws {
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
            routeCardData: nil,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: .init()
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        try sut.inspect().find(viewWithAccessibilityLabel: "Close").button().tap()
        XCTAssertEqual([oldEntry], nearbyVM.navigationStack)
    }

    func testDebugModeNotShownByDefault() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in
            stop.id = "FAKE_STOP_ID"
        }

        let nearbyVM: NearbyViewModel = .init(
            navigationStack: [.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)]
        )
        let sut = StopDetailsView(
            stopId: stop.id,
            stopFilter: nil,
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            routeCardData: nil,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: .init()
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        XCTAssertThrowsError(try sut.inspect().find(text: "stop id: FAKE_STOP_ID"))
    }

    func testDebugModeShown() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in
            stop.id = "FAKE_STOP_ID"
        }

        let nearbyVM: NearbyViewModel = .init(
            navigationStack: [.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)]
        )
        let sut = StopDetailsView(
            stopId: stop.id,
            stopFilter: nil,
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            routeCardData: nil,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: .init()
        )

        ViewHosting.host(view: sut.withFixedSettings([.devDebugMode: true]))
        try sut.inspect().findAll(ViewType.Text.self).forEach { view in try print(view.string()) }
        XCTAssertNotNil(try sut.inspect().find(text: "stop id: FAKE_STOP_ID"))
    }
}
