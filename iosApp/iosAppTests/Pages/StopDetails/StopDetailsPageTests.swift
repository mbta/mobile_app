//
//  StopDetailsPageTests.swift
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

final class StopDetailsPageTests: XCTestCase {
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

        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(alerts: [:])

        let filters = StopDetailsPageFilters(
            stopId: stop.id,
            stopFilter: .init(
                routeId: route.id,
                directionId: routePattern.directionId
            ),
            tripFilter: nil
        )
        let stopDetailsVM = MockStopDetailsViewModel(initialState: .init(
            routeData: StopDetailsViewModel.RouteDataFiltered(
                filteredWith: filters,
                stopData: .init(
                    route: route,
                    stop: stop,
                    data: [.init(
                        lineOrRoute: .route(route),
                        stop: stop,
                        directionId: 0,
                        routePatterns: [routePattern],
                        stopIds: [stop.id],
                        upcomingTrips: [.init(trip: trip, schedule: schedule)],
                        alertsHere: [],
                        allDataLoaded: true,
                        hasSchedulesToday: true,
                        alertsDownstream: [],
                        context: .stopDetailsFiltered
                    )],
                    globalData: .init(objects: objects)
                )
            ),
            alertSummaries: [:],
            awaitingPredictionsAfterBackground: false
        ))

        let sut = StopDetailsPage(
            filters: filters,
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            nearbyVM: nearbyVM,
            mapVM: MockMapViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: viewportProvider
        )

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([:]))

        try await Task.sleep(for: .seconds(1))
        XCTAssertNotNil(try sut.inspect().find(StopDetailsFilteredView.self))
        XCTAssertNotNil(try sut.inspect().find(DepartureTile.self))
    }

    func testCloseButton() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let nearbyVM = NearbyViewModel()

        let closeExp = expectation(description: "close button pressed")

        let sut = StopDetailsPage(
            filters: .init(
                stopId: stop.id,
                stopFilter: nil,
                tripFilter: nil
            ),
            navCallbacks: .init(onBack: nil, onClose: { closeExp.fulfill() }, backButtonPresentation: .floating),
            errorBannerVM: MockErrorBannerViewModel(),
            nearbyVM: nearbyVM,
            mapVM: MockMapViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            stopDetailsVM: MockStopDetailsViewModel(),
            viewportProvider: .init()
        ).withFixedSettings([:])

        try sut.inspect().find(viewWithAccessibilityLabel: "Close").button().tap()
        wait(for: [closeExp], timeout: 1)
    }

    func testRejoinsPredictionsAfterBackgrounding() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let stopFilter: StopDetailsFilter? = .init(routeId: route.id, directionId: 0)
        let joinExpectation = expectation(description: "joins predictions")
        joinExpectation.expectedFulfillmentCount = 2
        let leaveExpectation = expectation(description: "leaves predictions")

        let stopDetailsVM = MockStopDetailsViewModel()
        stopDetailsVM.onSetActive = { active, background in
            if !active.boolValue, background.boolValue {
                leaveExpectation.fulfill()
            } else if active.boolValue, !background.boolValue {
                joinExpectation.fulfill()
            }
        }

        let sut = StopDetailsPage(
            filters: .init(
                stopId: stop.id,
                stopFilter: stopFilter,
                tripFilter: nil
            ),
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            nearbyVM: .init(),
            mapVM: MockMapViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: viewportProvider
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        try sut.inspect().findAndCallOnChange(newValue: ScenePhase.background)
        wait(for: [leaveExpectation], timeout: 1)

        try sut.inspect().findAndCallOnChange(newValue: ScenePhase.active)
        wait(for: [joinExpectation], timeout: 1)
    }

    @MainActor
    func testAppliesStopFilterAutomatically() async throws {
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
        objects.schedule { schedule in
            schedule.trip = trip
            schedule.routeId = route.id.idText
            schedule.stopId = stop.id
            schedule.stopSequence = 0
            schedule.departureTime = EasternTimeInstant.now().plus(minutes: 10)
        }

        loadKoinMocks(objects: objects)

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let nearbyVM: NearbyViewModel = .init(
            navigationStack: [.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)]
        )
        nearbyVM.alerts = .init(alerts: [:])

        let updatedFilter = StopDetailsFilter(
            routeId: route.id,
            directionId: routePattern.directionId,
            autoFilter: true
        )
        let stopDetailsVM = MockStopDetailsViewModel()
        stopDetailsVM.filterUpdates.tryEmit(value: .init(
            stopId: stop.id,
            stopFilter: updatedFilter,
            tripFilter: nil
        ))

        let sut = StopDetailsPage(
            filters: .init(
                stopId: stop.id,
                stopFilter: nil,
                tripFilter: nil
            ),
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            nearbyVM: nearbyVM,
            mapVM: MockMapViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: viewportProvider
        )
        ViewHosting.host(view: sut.withFixedSettings([:]))

        try await Task.sleep(for: .seconds(1))
        XCTAssertEqual(
            nearbyVM.navigationStack.lastStopDetailsFilter,
            updatedFilter
        )
    }

    @MainActor
    func testAppliesTripFilterAutomatically() async throws {
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

        let stopFilter: StopDetailsFilter = .init(routeId: route.id, directionId: 0)
        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let nearbyVM: NearbyViewModel = .init(
            navigationStack: [.stopDetails(stopId: stop.id, stopFilter: stopFilter, tripFilter: nil)]
        )
        nearbyVM.alerts = .init(alerts: [:])

        let initialFilters = StopDetailsPageFilters(
            stopId: stop.id,
            stopFilter: stopFilter,
            tripFilter: nil
        )
        let updatedTripFilter = TripDetailsFilter(
            tripId: trip.id, vehicleId: nil, stopSequence: 0, selectionLock: false
        )

        let stopDetailsVM = MockStopDetailsViewModel()
        stopDetailsVM.filterUpdates.tryEmit(value: .init(
            stopId: stop.id, stopFilter: stopFilter, tripFilter: updatedTripFilter
        ))

        let sut = StopDetailsPage(
            filters: initialFilters,
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            nearbyVM: nearbyVM,
            mapVM: MockMapViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: viewportProvider
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        try await Task.sleep(for: .seconds(1))

        XCTAssertEqual(
            nearbyVM.navigationStack.lastTripDetailsFilter,
            updatedTripFilter
        )
    }
}
