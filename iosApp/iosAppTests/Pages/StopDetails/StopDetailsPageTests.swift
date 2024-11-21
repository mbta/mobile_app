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

        let sut = StopDetailsPage(
            stopId: stop.id,
            stopFilter: stopFilter,
            tripFilter: nil,
            errorBannerVM: .init(),
            nearbyVM: .init(combinedStopAndTrip: true),
            mapVM: .init(),
            viewportProvider: viewportProvider,
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(scheduleResponse: .init(objects: objects), callback: callback)
        )

        ViewHosting.host(view: sut)
        try sut.inspect().find(StopDetailsView.self).callOnChange(newValue: nextStop.id)

        wait(for: [newStopSchedulesFetchedExpectation], timeout: 5)
    }

    @MainActor
    func testClearsFilter() {
        let objects = ObjectCollectionBuilder()
        let route1 = objects.route()
        let route2 = objects.route()
        let stop = objects.stop { _ in }
        let trip1 = objects.trip { trip in trip.stopIds = [stop.id] }
        let trip2 = objects.trip { trip in trip.stopIds = [stop.id] }
        let routePattern1 = objects.routePattern(route: route1) { pattern in
            pattern.representativeTripId = trip1.id
        }
        objects.routePattern(route: route2) { pattern in
            pattern.representativeTripId = trip2.id
        }

        let schedule1 = objects.schedule { schedule in
            schedule.trip = trip1
            schedule.routeId = route1.id
            schedule.stopId = stop.id
            schedule.departureTime = (Date.now + 10 * 60).toKotlinInstant()
        }

        let schedule2 = objects.schedule { schedule in
            schedule.trip = trip2
            schedule.routeId = route2.id
            schedule.stopId = stop.id
            schedule.departureTime = (Date.now + 10 * 60).toKotlinInstant()
        }

        objects.prediction(schedule: schedule1) { _ in }
        objects.prediction(schedule: schedule2) { _ in }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let stopFilter: StopDetailsFilter? = .init(
            routeId: route1.id,
            directionId: routePattern1.directionId
        )

        let nearbyVM = NearbyViewModel(
            navigationStack: [.stopDetails(
                stopId: stop.id,
                stopFilter: .init(routeId: route1.id, directionId: 0),
                tripFilter: nil
            )],
            combinedStopAndTrip: true
        )
        nearbyVM.alerts = .init(alerts: [:])

        let schedulesLoadedPublisher = PassthroughSubject<Bool, Never>()

        let sut = StopDetailsPage(
            stopId: stop.id,
            stopFilter: stopFilter,
            tripFilter: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            viewportProvider: viewportProvider,
            globalRepository: MockGlobalRepository(response: .init(objects: objects)),
            predictionsRepository: MockPredictionsRepository(
                connectV2Outcome: .init(objects: objects)
            ),
            schedulesRepository: MockScheduleRepository(
                scheduleResponse: .init(objects: objects),
                callback: { _ in schedulesLoadedPublisher.send(true) }
            )
        )

        XCTAssertNotNil(nearbyVM.navigationStack.lastStopDetailsFilter)
        let exp = sut.inspection.inspect(onReceive: schedulesLoadedPublisher) { view in
            try view.find(button: "All").tap()
            XCTAssertNil(nearbyVM.navigationStack.lastStopDetailsFilter)
        }
        ViewHosting.host(view: sut)

        wait(for: [exp], timeout: 5)
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

        let sut = StopDetailsPage(
            stopId: stop.id,
            stopFilter: stopFilter,
            tripFilter: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            viewportProvider: viewportProvider,
            globalRepository: MockGlobalRepository(response: .init(objects: objects, patternIdsByStop: [:])),
            predictionsRepository: MockPredictionsRepository(connectV2Outcome: .companion.empty),
            schedulesRepository: MockScheduleRepository(
                scheduleResponse: .init(objects: objects),
                callback: { _ in schedulesLoadedPublisher.send(true) }
            )
        )

        let exp = sut.inspection.inspect(onReceive: schedulesLoadedPublisher, after: 1) { view in
            XCTAssertNotNil(try view.find(StopDetailsRoutesView.self))
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 30)
    }

    func testBackButton() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        class FakeNearbyVM: NearbyViewModel {
            let backExp: XCTestExpectation
            init(_ backExp: XCTestExpectation) {
                self.backExp = backExp
                super.init(combinedStopAndTrip: true)
            }

            override func goBack() {
                backExp.fulfill()
            }
        }

        let backExp = XCTestExpectation(description: "goBack called")
        let nearbyVM = FakeNearbyVM(backExp)
        nearbyVM.navigationStack = [
            .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
            .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
            .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
        ]

        let sut = StopDetailsPage(
            stopId: stop.id,
            stopFilter: nil,
            tripFilter: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            viewportProvider: .init(),
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository()
        )

        try sut.inspect().find(viewWithAccessibilityLabel: "Back").button().tap()

        wait(for: [backExp], timeout: 2)
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

        let sut = StopDetailsPage(
            stopId: stop.id,
            stopFilter: nil,
            tripFilter: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            viewportProvider: .init(),
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository()
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
        let sut = StopDetailsPage(
            stopId: stop.id,
            stopFilter: stopFilter,
            tripFilter: nil,
            errorBannerVM: .init(),
            nearbyVM: .init(combinedStopAndTrip: true),
            mapVM: .init(),
            viewportProvider: viewportProvider,
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository()
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
        let sut = StopDetailsPage(
            stopId: stop.id,
            stopFilter: stopFilter,
            tripFilter: nil,
            errorBannerVM: .init(),
            nearbyVM: .init(combinedStopAndTrip: true),
            mapVM: .init(),
            viewportProvider: viewportProvider,
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository()
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

        let globalDataLoaded = PassthroughSubject<Void, Never>()

        let globalResponse: GlobalResponse = .init(
            objects: objects,
            patternIdsByStop: [stop.id: [pattern.id]]
        )
        let predictionsRepo = MockPredictionsRepository()
        let sut = StopDetailsPage(
            stopId: stop.id,
            stopFilter: stopFilter,
            tripFilter: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            viewportProvider: viewportProvider,
            globalRepository: MockGlobalRepository(
                response: globalResponse,
                onGet: { globalDataLoaded.send() }
            ),
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository()
        )

        XCTAssertNil(nearbyVM.departures)
        let hasAppeared = sut.inspection.inspect(onReceive: globalDataLoaded, after: 1) { view in
            try view.find(StopDetailsView.self)
                .callOnChange(newValue: PredictionsByStopJoinResponse(objects: objects))
            XCTAssertNotNil(nearbyVM.departures)
            // Keeps internal departures in sync with VM departures
            XCTAssertEqual(try view.actualView().internalDepartures, nearbyVM.departures)
        }
        ViewHosting.host(view: sut)

        wait(for: [hasAppeared], timeout: 5)
    }

    @MainActor
    func testAppliesFilterAutomatically() throws {
        let objects = ObjectCollectionBuilder()

        let route = objects.route()
        let stop = objects.stop { _ in }
        let trip = objects.trip { trip in
            trip.directionId = 0
            trip.stopIds = [stop.id]
        }
        let routePattern = objects.routePattern(route: route) { pattern in
            pattern.representativeTripId = trip.id
        }

        let schedule = objects.schedule { schedule in
            schedule.trip = trip
            schedule.routeId = route.id
            schedule.stopId = stop.id
            schedule.stopSequence = 0
            schedule.departureTime = (Date.now + 10 * 60).toKotlinInstant()
        }
        objects.prediction(schedule: schedule) { prediction in
            prediction.arrivalTime = (Date.now + 10 * 60).toKotlinInstant()
        }
        let vehicle = objects.vehicle { vehicle in
            vehicle.currentStopSequence = 0
            vehicle.currentStatus = .inTransitTo
            vehicle.tripId = trip.id
        }

        let schedulesLoadedPublisher = PassthroughSubject<Void, Never>()

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let nearbyVM: NearbyViewModel = .init(
            navigationStack: [.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)],
            combinedStopAndTrip: true
        )
        nearbyVM.alerts = .init(alerts: [:])

        let sut = StopDetailsPage(
            stopId: stop.id,
            stopFilter: nil,
            tripFilter: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            viewportProvider: viewportProvider,
            globalRepository: MockGlobalRepository(response: .init(objects: objects)),
            predictionsRepository: MockPredictionsRepository(connectV2Outcome: .init(objects: objects)),
            schedulesRepository: MockScheduleRepository(
                scheduleResponse: .init(objects: objects),
                callback: { _ in schedulesLoadedPublisher.send() }
            )
        )

        let stopChangedPublisher = PassthroughSubject<Void, Never>()
        let stopFilterExp = sut.inspection.inspect(onReceive: schedulesLoadedPublisher) { view in
            let stopFilter = nearbyVM.navigationStack.lastStopDetailsFilter
            XCTAssertEqual(stopFilter, StopDetailsFilter(routeId: route.id, directionId: routePattern.directionId))
            try view.find(StopDetailsView.self).callOnChange(newValue: stopFilter)
            stopChangedPublisher.send()
        }

        let vehicleFilterExp = sut.inspection.inspect(onReceive: stopChangedPublisher, after: 2) { _ in
//            XCTAssertEqual(
//                nearbyVM.navigationStack.lastTripDetailsFilter,
//                TripDetailsFilter(tripId: trip.id, vehicleId: vehicle.id, stopSequence: 0, selectionLock: false)
//            )
        }
        ViewHosting.host(view: sut)
        wait(for: [stopFilterExp, vehicleFilterExp], timeout: 30)
    }
}
