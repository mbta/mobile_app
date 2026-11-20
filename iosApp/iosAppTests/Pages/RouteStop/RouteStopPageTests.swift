//
//  RouteStopPageTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 4/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
@_spi(Experimental) import MapboxMaps
import Shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest

final class RouteStopPageTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testDisplaysSchedules() async throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let trip = objects.trip { _ in }
        let schedule = objects.schedule { schedule in
            schedule.trip = trip
            schedule.routeId = route.id.idText
            schedule.stopId = stop.id
            schedule.departureTime = EasternTimeInstant.now().plus(minutes: 10)
        }
        let routePattern = objects.routePattern(route: route) { _ in }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))

        loadKoinMocks(objects: objects)

        let filters = RouteStopPageFilters(
            stopId: stop.id,
            routeStopFilter: .init(
                routeId: route.id,
                directionId: routePattern.directionId
            ),
            tripFilter: nil
        )
        let stopCentricVM = MockStopCentricViewModel(initialState: .init(
            routeData: StopCentricViewModel.RouteDataRouteStop(
                filteredWith: filters,
                stopData: .init(
                    route: route,
                    stop: stop,
                    data: [.init(
                        lineOrRoute: .route(route),
                        stop: stop,
                        direction: .init(directionId: 0, route: route),
                        routePatterns: [routePattern],
                        stopIds: [stop.id],
                        upcomingTrips: [.init(trip: trip, schedule: schedule)],
                        alertsHere: [],
                        allDataLoaded: true,
                        hasSchedulesToday: true,
                        subwayServiceStartTime: nil,
                        alertsDownstream: [],
                        context: .routeStop
                    )]
                )
            ),
            awaitingPredictionsAfterBackground: false
        ))

        let sut = RouteStopPage(
            filters: filters,
            navCallbacks: .companion.empty,
            alerts: .init(alerts: [:]),
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            stopCentricVM: stopCentricVM,
            navManager: .init(),
            viewportProvider: viewportProvider
        )

        try await ViewHosting.host(sut.environmentObject(ViewportProvider()).withFixedSettings([:])) {
            XCTAssertNotNil(try sut.inspect().find(DepartureTile.self))
        }
    }

    @MainActor func testCloseButton() {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }

        loadKoinMocks(objects: objects)

        let filters = RouteStopPageFilters(
            stopId: stop.id,
            routeStopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil
        )

        let closeExp = expectation(description: "close button pressed")

        let sut = RouteStopPage(
            filters: filters,
            navCallbacks: .init(onBack: nil, onClose: { closeExp.fulfill() }, backButtonPresentation: .floating),
            alerts: .init(alerts: [:]),
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            stopCentricVM: MockStopCentricViewModel(initialState: .init(
                routeData: StopCentricViewModel.RouteDataRouteStop(
                    filteredWith: filters,
                    stopData: .init(
                        route: route,
                        stop: stop,
                        data: []
                    )
                ),
                awaitingPredictionsAfterBackground: false
            )),
            navManager: .init(),
            viewportProvider: .init()
        )

        let exp = sut.inspection.inspect(after: 1) { view in
            try view.find(viewWithAccessibilityLabel: "Close").button().tap()
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        defer { ViewHosting.expel() }

        wait(for: [closeExp, exp], timeout: 2)
    }

    func testRejoinsPredictionsAfterBackgrounding() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let routeStopFilter: RouteStopFilter = .init(routeId: route.id, directionId: 0)
        let joinExpectation = expectation(description: "joins predictions")
        joinExpectation.expectedFulfillmentCount = 2
        let leaveExpectation = expectation(description: "leaves predictions")

        let stopCentricVM = MockStopCentricViewModel()
        stopCentricVM.onSetActive = { active, background in
            if !active.boolValue, background.boolValue {
                leaveExpectation.fulfill()
            } else if active.boolValue, !background.boolValue {
                joinExpectation.fulfill()
            }
        }

        let sut = RouteStopPage(
            filters: RouteStopPageFilters(
                stopId: stop.id,
                routeStopFilter: routeStopFilter,
                tripFilter: nil
            ),
            navCallbacks: .companion.empty,
            alerts: .init(alerts: [:]),
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            stopCentricVM: stopCentricVM,
            navManager: .init(),
            viewportProvider: viewportProvider
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        defer { ViewHosting.expel() }

        try sut.inspect().findAndCallOnChange(newValue: ScenePhase.background)
        wait(for: [leaveExpectation], timeout: 1)

        try sut.inspect().findAndCallOnChange(newValue: ScenePhase.active)
        wait(for: [joinExpectation], timeout: 1)
    }

    @MainActor
    func testAppliesTripFilterAutomatically() {
        let objects = ObjectCollectionBuilder()

        let route = objects.route()
        let stop = objects.stop { _ in }

        let tripId = "trip"
        let routePattern = objects.routePattern(route: route) { pattern in
            pattern.representativeTripId = tripId
        }
        let trip = objects.trip(routePattern: routePattern) { trip in
            trip.id = tripId
            trip.directionId = 0
            trip.stopIds = [stop.id]
            trip.routePatternId = routePattern.id
        }
        let schedule = objects.schedule { schedule in
            schedule.trip = trip
            schedule.routeId = route.id.idText
            schedule.stopId = stop.id
            schedule.stopSequence = 0
            schedule.departureTime = EasternTimeInstant.now().plus(minutes: 10)
        }
        objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = EasternTimeInstant.now().plus(minutes: 10)
        }

        loadKoinMocks(objects: objects)

        let routeStopFilter: RouteStopFilter = .init(routeId: route.id, directionId: 0)
        let navManager = NavigationManager(navigationStack: [.routeStop(
            stopId: stop.id,
            routeStopFilter: routeStopFilter,
            tripFilter: nil
        )])
        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))

        let initialFilters = RouteStopPageFilters(
            stopId: stop.id,
            routeStopFilter: routeStopFilter,
            tripFilter: nil
        )
        let updatedTripFilter = TripDetailsFilter(
            tripId: trip.id, vehicleId: nil, stopSequence: 0, selectionLock: false
        )

        let stopCentricVM = MockStopCentricViewModel()
        stopCentricVM.filterUpdates.tryEmit(value: .init(
            stopId: stop.id, routeStopFilter: routeStopFilter, tripFilter: updatedTripFilter
        ))

        let sut = RouteStopPage(
            filters: initialFilters,
            navCallbacks: .companion.empty,
            alerts: .init(alerts: [:]),
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            stopCentricVM: stopCentricVM,
            navManager: navManager,
            viewportProvider: viewportProvider
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        defer { ViewHosting.expel() }

        let exp = sut.inspection.inspect(after: 1) { _ in
            XCTAssertEqual(
                navManager.navigationStack.lastTripDetailsFilter,
                updatedTripFilter
            )
        }

        wait(for: [exp], timeout: 2)
    }
}
