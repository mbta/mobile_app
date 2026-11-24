//
//  StopOverviewPageTests.swift
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

final class StopOverviewPageTests: XCTestCase {
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

        let filters = StopOverviewPageFilters(stopId: stop.id)
        let stopCentricVM = MockStopCentricViewModel(initialState: .init(
            routeData: StopCentricViewModel.RouteDataStopOverview(
                filteredWith: filters,
                routeCards: [.init(
                    lineOrRoute: .route(route),
                    stopData: [.init(
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
                    )],
                    at: .now(),
                )]
            ),
            awaitingPredictionsAfterBackground: false
        ))

        let sut = StopOverviewPage(
            filters: filters,
            navCallbacks: .companion.empty,
            alerts: .init(alerts: [:]),
            errorBannerVM: MockErrorBannerViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            stopCentricVM: stopCentricVM,
            navManager: .init(),
            viewportProvider: viewportProvider
        )

        try await ViewHosting.host(sut.environmentObject(ViewportProvider()).withFixedSettings([:])) {
            XCTAssertNotNil(try sut.inspect().find(UpcomingTripView.self))
        }
    }

    func testCloseButton() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let closeExp = expectation(description: "close button pressed")

        let sut = StopOverviewPage(
            filters: StopOverviewPageFilters(stopId: stop.id),
            navCallbacks: .init(onBack: nil, onClose: { closeExp.fulfill() }, backButtonPresentation: .floating),
            alerts: .init(alerts: [:]),
            errorBannerVM: MockErrorBannerViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            stopCentricVM: MockStopCentricViewModel(),
            navManager: .init(),
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

        let sut = StopOverviewPage(
            filters: .init(stopId: stop.id),
            navCallbacks: .companion.empty,
            alerts: .init(alerts: [:]),
            errorBannerVM: MockErrorBannerViewModel(),
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
    func testAppliesStopFilterAutomatically() {
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

        let navManager = NavigationManager(navigationStack: [.stopOverview(stopId: stop.id)])

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let updatedFilter = RouteStopFilter(
            routeId: route.id,
            directionId: routePattern.directionId,
            autoFilter: true
        )
        let stopCentricVM = MockStopCentricViewModel()

        let sut = StopOverviewPage(
            filters: StopOverviewPageFilters(stopId: stop.id),
            navCallbacks: .companion.empty,
            alerts: .init(alerts: [:]),
            errorBannerVM: MockErrorBannerViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            stopCentricVM: stopCentricVM,
            navManager: navManager,
            viewportProvider: viewportProvider
        )
        ViewHosting.host(view: sut.withFixedSettings([:]))
        defer { ViewHosting.expel() }

        stopCentricVM.filterUpdates.tryEmit(value: .init(
            stopId: stop.id,
            routeStopFilter: updatedFilter,
            tripFilter: nil
        ))

        sut.inspection.inspect(after: 1) { _ in
            XCTAssertEqual(
                navManager.navigationStack.lastRouteStopFilter,
                updatedFilter
            )
        }
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

        let initialFilters = StopOverviewPageFilters(stopId: stop.id)
        let updatedTripFilter = TripDetailsFilter(
            tripId: trip.id, vehicleId: nil, stopSequence: 0, selectionLock: false
        )

        let stopCentricVM = MockStopCentricViewModel()
        stopCentricVM.filterUpdates.tryEmit(value: .init(
            stopId: stop.id, routeStopFilter: routeStopFilter, tripFilter: updatedTripFilter
        ))

        let sut = StopOverviewPage(
            filters: initialFilters,
            navCallbacks: .companion.empty,
            alerts: .init(alerts: [:]),
            errorBannerVM: MockErrorBannerViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            stopCentricVM: stopCentricVM,
            navManager: navManager,
            viewportProvider: viewportProvider
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        defer { ViewHosting.expel() }

        sut.inspection.inspect(after: 1) { _ in
            XCTAssertEqual(
                navManager.navigationStack.lastTripDetailsFilter,
                updatedTripFilter
            )
        }
    }
}
