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
import shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest

final class StopDetailsPageTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testStopChangeFetchesNewData() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let nextStop = objects.stop { $0.id = "next" }
        let routePattern = objects.routePattern(route: route) { _ in }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let stopFilter: StopDetailsFilter? = .init(
            routeId: route.id,
            directionId: routePattern.directionId
        )

        let newStopSchedulesFetchedExpectation = XCTestExpectation(description: "Fetched stops for next stop")
        func callback(stopIds: [String]) {
            if stopIds == [nextStop.id] {
                newStopSchedulesFetchedExpectation.fulfill()
            }
        }

        let stopDetailsVM = StopDetailsViewModel(
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(scheduleResponse: .init(objects: objects), callback: callback)
        )

        let sut = StopDetailsPage(
            stopId: stop.id,
            stopFilter: stopFilter,
            tripFilter: nil,
            errorBannerVM: .init(),
            nearbyVM: .init(combinedStopAndTrip: true),
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: viewportProvider
        )

        ViewHosting.host(view: sut)
        try sut.inspect().find(StopDetailsView.self).callOnChange(newValue: nextStop.id)

        wait(for: [newStopSchedulesFetchedExpectation], timeout: 5)
    }

    @MainActor
    func testDisplaysSchedules() {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let trip = objects.trip { _ in }
        objects.schedule { schedule in
            schedule.trip = trip
            schedule.routeId = route.id
            schedule.stopId = stop.id
            schedule.departureTime = (Date.now + 10 * 60).toKotlinInstant()
        }
        let routePattern = objects.routePattern(route: route) { _ in }

        let schedulesLoadedPublisher = PassthroughSubject<Bool, Never>()

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let stopFilter: StopDetailsFilter? = .init(
            routeId: route.id,
            directionId: routePattern.directionId
        )

        let nearbyVM = NearbyViewModel(combinedStopAndTrip: true)
        nearbyVM.alerts = .init(alerts: [:])

        let stopDetailsVM = StopDetailsViewModel(
            globalRepository: MockGlobalRepository(response: .init(objects: objects)),
            predictionsRepository: MockPredictionsRepository(connectV2Response: .companion.empty),
            schedulesRepository: MockScheduleRepository(
                scheduleResponse: .init(objects: objects),
                callback: { _ in schedulesLoadedPublisher.send(true) }
            )
        )

        let sut = StopDetailsPage(
            stopId: stop.id,
            stopFilter: stopFilter,
            tripFilter: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: viewportProvider
        )

        let exp = sut.inspection.inspect(onReceive: schedulesLoadedPublisher, after: 1) { view in
            XCTAssertNotNil(try view.find(StopDetailsFilteredView.self))
            XCTAssertNotNil(try view.find(DepartureTile.self))
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 30)
    }

    func testCloseButton() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let nearbyVM = NearbyViewModel(
            navigationStack: [
                .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
                .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
                .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
            ],
            combinedStopAndTrip: true
        )

        let stopDetailsVM = StopDetailsViewModel(
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository()
        )

        let sut = StopDetailsPage(
            stopId: stop.id,
            stopFilter: nil,
            tripFilter: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: .init()
        )

        try sut.inspect().find(viewWithAccessibilityLabel: "Close").button().tap()
        XCTAssert(nearbyVM.navigationStack.isEmpty)
    }

    func testRejoinsPredictionsAfterBackgrounding() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let stopFilter: StopDetailsFilter? = .init(routeId: route.id, directionId: 0)
        let joinExpectation = expectation(description: "joins predictions")
        joinExpectation.expectedFulfillmentCount = 2
        joinExpectation.assertForOverFulfill = true

        let leaveExpectation = expectation(description: "leaves predictions")

        let predictionsRepo = MockPredictionsRepository(
            onConnect: {},
            onConnectV2: { _ in joinExpectation.fulfill() },
            onDisconnect: { leaveExpectation.fulfill() },
            connectOutcome: nil,
            connectV2Outcome: nil
        )

        let stopDetailsVM = StopDetailsViewModel(
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository()
        )

        let sut = StopDetailsPage(
            stopId: stop.id,
            stopFilter: stopFilter,
            tripFilter: nil,
            errorBannerVM: .init(),
            nearbyVM: .init(combinedStopAndTrip: true),
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: viewportProvider
        )

        ViewHosting.host(view: sut)

        try sut.inspect().find(StopDetailsView.self).callOnChange(newValue: ScenePhase.background)

        wait(for: [leaveExpectation], timeout: 1)

        try sut.inspect().find(StopDetailsView.self).callOnChange(newValue: ScenePhase.active)

        wait(for: [joinExpectation], timeout: 1)
    }

    func testLeavesAndJoinsPredictionsOnStopChange() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let stopFilter: StopDetailsFilter? = .init(routeId: route.id, directionId: 0)
        let leaveExpectation = expectation(description: "leaves predictions")
        leaveExpectation.expectedFulfillmentCount = 1

        let joinExpectation = expectation(description: "joins predictions")
        joinExpectation.expectedFulfillmentCount = 2
        joinExpectation.assertForOverFulfill = true

        let predictionsRepo = MockPredictionsRepository(
            onConnect: {},
            onConnectV2: { _ in joinExpectation.fulfill() },
            onDisconnect: { leaveExpectation.fulfill() },
            connectOutcome: nil,
            connectV2Outcome: nil
        )

        let stopDetailsVM = StopDetailsViewModel(
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository()
        )

        let sut = StopDetailsPage(
            stopId: stop.id,
            stopFilter: stopFilter,
            tripFilter: nil,
            errorBannerVM: .init(),
            nearbyVM: .init(combinedStopAndTrip: true),
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: viewportProvider
        )

        ViewHosting.host(view: sut)

        try sut.inspect().find(StopDetailsView.self).callOnChange(newValue: stop.id)

        wait(for: [leaveExpectation], timeout: 1)
        wait(for: [joinExpectation], timeout: 1)
    }

    @MainActor
    func testUpdatesDeparturesOnPredictionsChange() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let prediction = objects.prediction { _ in }
        let pattern = objects.routePattern(route: route) { _ in }
        objects.trip { trip in
            trip.id = prediction.tripId
            trip.stopIds = [stop.id]
        }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let stopFilter: StopDetailsFilter? = .init(routeId: route.id, directionId: 0)

        let nearbyVM: NearbyViewModel = .init(
            navigationStack: [.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)],
            combinedStopAndTrip: true
        )
        nearbyVM.alerts = .init(alerts: [:])

        let stopDetailsVM = StopDetailsViewModel(
            globalRepository: MockGlobalRepository(),
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository()
        )
        stopDetailsVM.global = .init(objects: objects, patternIdsByStop: [stop.id: [pattern.id]])
        stopDetailsVM.predictionsByStop = .init(objects: objects)
        stopDetailsVM.schedulesResponse = .init(objects: objects)

        let sut = StopDetailsPage(
            stopId: stop.id,
            stopFilter: stopFilter,
            tripFilter: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: viewportProvider
        )

        XCTAssertNil(nearbyVM.departures)

        let hasSetDepartures = sut.inspection.inspect { view in
            XCTAssertNotNil(nearbyVM.departures)
            // Keeps internal departures in sync with VM departures
            XCTAssertEqual(try view.actualView().internalDepartures, nearbyVM.departures)
        }
        ViewHosting.host(view: sut)

        wait(for: [hasSetDepartures], timeout: 2)
    }

    @MainActor
    func testAppliesStopFilterAutomatically() throws {
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
            schedule.routeId = route.id
            schedule.stopId = stop.id
            schedule.stopSequence = 0
            schedule.departureTime = (Date.now + 10 * 60).toKotlinInstant()
        }

        let schedulesLoadedPublisher = PassthroughSubject<Void, Never>()

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let nearbyVM: NearbyViewModel = .init(
            navigationStack: [.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)],
            combinedStopAndTrip: true
        )
        nearbyVM.alerts = .init(alerts: [:])

        let stopDetailsVM = StopDetailsViewModel(
            globalRepository: MockGlobalRepository(response: .init(objects: objects)),
            predictionsRepository: MockPredictionsRepository(connectV2Response: .init(objects: objects)),
            schedulesRepository: MockScheduleRepository(
                scheduleResponse: .init(objects: objects),
                callback: { _ in schedulesLoadedPublisher.send() }
            )
        )

        let sut = StopDetailsPage(
            stopId: stop.id,
            stopFilter: nil,
            tripFilter: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: viewportProvider
        )

        let stopFilterExp = sut.inspection.inspect(onReceive: schedulesLoadedPublisher) { _ in
            XCTAssertEqual(
                nearbyVM.navigationStack.lastStopDetailsFilter,
                StopDetailsFilter(routeId: route.id, directionId: routePattern.directionId)
            )
        }

        ViewHosting.host(view: sut)
        wait(for: [stopFilterExp], timeout: 2)
    }

    @MainActor
    func testAppliesTripFilterAutomatically() throws {
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
            schedule.routeId = route.id
            schedule.stopId = stop.id
            schedule.stopSequence = 0
            schedule.departureTime = (Date.now + 10 * 60).toKotlinInstant()
        }
        objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = (Date.now + 10 * 60).toKotlinInstant()
        }

        let schedulesLoadedPublisher = PassthroughSubject<Void, Never>()

        let stopFilter: StopDetailsFilter = .init(routeId: route.id, directionId: 0)
        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let nearbyVM: NearbyViewModel = .init(
            navigationStack: [.stopDetails(stopId: stop.id, stopFilter: stopFilter, tripFilter: nil)],
            combinedStopAndTrip: true
        )
        nearbyVM.alerts = .init(alerts: [:])

        let stopDetailsVM = StopDetailsViewModel(
            globalRepository: MockGlobalRepository(response: .init(objects: objects)),
            predictionsRepository: MockPredictionsRepository(connectV2Response: .init(objects: objects)),
            schedulesRepository: MockScheduleRepository(
                scheduleResponse: .init(objects: objects),
                callback: { _ in schedulesLoadedPublisher.send() }
            )
        )

        let sut = StopDetailsPage(
            stopId: stop.id,
            stopFilter: stopFilter,
            tripFilter: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: viewportProvider
        )

        let vehicleFilterExp = sut.inspection.inspect(onReceive: schedulesLoadedPublisher) { _ in
            XCTAssertEqual(
                nearbyVM.navigationStack.lastTripDetailsFilter,
                TripDetailsFilter(tripId: trip.id, vehicleId: nil, stopSequence: 0, selectionLock: false)
            )
        }
        ViewHosting.host(view: sut)
        wait(for: [vehicleFilterExp], timeout: 2)
    }
}
