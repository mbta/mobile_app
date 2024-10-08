//
//  StopDetailsPageTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 4/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest
@_spi(Experimental) import MapboxMaps

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
        let filter: StopDetailsFilter? = .init(
            routeId: route.id,
            directionId: routePattern.directionId
        )

        class FakeSchedulesRepository: ISchedulesRepository {
            let callback: (_ stopIds: [String]) -> Void

            init(callback: @escaping (_ stopIds: [String]) -> Void) {
                self.callback = callback
            }

            func __getSchedule(stopIds: [String]) async throws -> ApiResult<ScheduleResponse> {
                callback(stopIds)
                return ApiResultOk(data: ScheduleResponse(objects: ObjectCollectionBuilder()))
            }

            func __getSchedule(stopIds: [String], now _: Instant) async throws -> ApiResult<ScheduleResponse> {
                callback(stopIds)
                return ApiResultOk(data: ScheduleResponse(objects: ObjectCollectionBuilder()))
            }
        }

        let newStopSchedulesFetchedExpectation = XCTestExpectation(description: "Fetched stops for next stop")
        func callback(stopIds: [String]) {
            if stopIds == [nextStop.id] {
                newStopSchedulesFetchedExpectation.fulfill()
            }
        }

        let sut = StopDetailsPage(
            schedulesRepository: FakeSchedulesRepository(callback: callback),
            predictionsRepository: MockPredictionsRepository(),
            viewportProvider: viewportProvider,
            stop: stop,
            filter: filter,
            nearbyVM: .init()
        )

        ViewHosting.host(view: sut)
        try sut.inspect().find(StopDetailsView.self).callOnChange(newValue: nextStop)

        wait(for: [newStopSchedulesFetchedExpectation], timeout: 5)
    }

    func testClearsFilter() throws {
        let objects = ObjectCollectionBuilder()
        let route1 = objects.route()
        let route2 = objects.route()
        let stop = objects.stop { _ in }
        let routePattern1 = objects.routePattern(route: route1) { _ in }
        let routePattern2 = objects.routePattern(route: route2) { _ in }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let filter: StopDetailsFilter? = .init(.init(
            routeId: route1.id,
            directionId: routePattern1.directionId
        ))

        let route1Departures = PatternsByStop(
            route: route1,
            stop: stop,
            patterns: [.ByHeadsign(
                route: route1,
                headsign: "",
                line: nil,
                patterns: [routePattern1],
                upcomingTrips: nil,
                alertsHere: nil,
                hasSchedulesToday: true
            )]
        )
        let route2Departures = PatternsByStop(
            route: route2,
            stop: stop,
            patterns: [.ByHeadsign(
                route: route2,
                headsign: "",
                line: nil,
                patterns: [routePattern2],
                upcomingTrips: nil,
                alertsHere: nil,
                hasSchedulesToday: true
            )]
        )

        let nearbyVM: NearbyViewModel = .init(navigationStack: [.stopDetails(stop,
                                                                             .init(
                                                                                 routeId: route1.id,
                                                                                 directionId: routePattern1.directionId
                                                                             ))])

        let sut = StopDetailsPage(
            schedulesRepository: MockScheduleRepository(),
            predictionsRepository: MockPredictionsRepository(),
            viewportProvider: viewportProvider,
            stop: stop,
            filter: filter,
            internalDepartures: .init(routes: [route1Departures, route2Departures]),
            nearbyVM: nearbyVM
        )

        XCTAssertNotNil(nearbyVM.navigationStack.lastStopDetailsFilter)

        try sut.inspect().find(button: "All").tap()
        XCTAssertNil(nearbyVM.navigationStack.lastStopDetailsFilter)
    }

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

        class FakeSchedulesRepository: ISchedulesRepository {
            let objects: ObjectCollectionBuilder
            let callback: (() -> Void)?
            init(objects: ObjectCollectionBuilder, callback: (() -> Void)?) {
                self.objects = objects
                self.callback = callback
            }

            func __getSchedule(stopIds _: [String]) async throws -> ApiResult<ScheduleResponse> {
                callback?()
                return ApiResultOk(data: ScheduleResponse(objects: objects))
            }

            func __getSchedule(stopIds _: [String], now _: Instant) async throws -> ApiResult<ScheduleResponse> {
                callback?()
                return ApiResultOk(data: ScheduleResponse(objects: objects))
            }
        }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let filter: StopDetailsFilter? = .init(
            routeId: route.id,
            directionId: routePattern.directionId
        )

        let sut = StopDetailsPage(
            globalRepository: MockGlobalRepository(response: .init(objects: objects, patternIdsByStop: [:])),
            schedulesRepository: FakeSchedulesRepository(
                objects: objects,
                callback: { schedulesLoadedPublisher.send(true) }
            ),
            predictionsRepository: MockPredictionsRepository(),
            viewportProvider: viewportProvider,
            stop: stop,
            filter: filter,
            nearbyVM: .init()
        )

        let exp = sut.inspection.inspect(onReceive: schedulesLoadedPublisher, after: 1) { view in
            XCTAssertNotNil(try view.find(StopDetailsRoutesView.self))
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 2)
    }

    func testBackButton() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        class FakeNearbyVM: NearbyViewModel {
            let backExp: XCTestExpectation
            init(_ backExp: XCTestExpectation) {
                self.backExp = backExp
                super.init()
            }

            override func goBack() {
                backExp.fulfill()
            }
        }

        let backExp = XCTestExpectation(description: "goBack called")
        let nearbyVM = FakeNearbyVM(backExp)
        nearbyVM.navigationStack = [.stopDetails(stop, nil), .stopDetails(stop, nil), .stopDetails(stop, nil)]

        let sut = StopDetailsPage(
            schedulesRepository: MockScheduleRepository(),
            predictionsRepository: MockPredictionsRepository(),
            viewportProvider: .init(),
            stop: stop,
            filter: nil,
            nearbyVM: nearbyVM
        )

        try sut.inspect().find(viewWithAccessibilityLabel: "Back").button().tap()

        wait(for: [backExp], timeout: 2)
    }

    func testCloseButton() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let nearbyVM = NearbyViewModel(
            navigationStack: [.stopDetails(stop, nil), .stopDetails(stop, nil), .stopDetails(stop, nil)]
        )

        let sut = StopDetailsPage(
            schedulesRepository: MockScheduleRepository(),
            predictionsRepository: MockPredictionsRepository(),
            viewportProvider: .init(),
            stop: stop,
            filter: nil,
            nearbyVM: nearbyVM
        )

        try sut.inspect().find(viewWithAccessibilityLabel: "Close").button().tap()
        XCTAssert(nearbyVM.navigationStack.isEmpty)
    }

    func testRejoinsPredictionsAfterBackgrounding() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let filter: StopDetailsFilter? = .init(routeId: route.id, directionId: 0)
        let joinExpectation = expectation(description: "joins predictions")
        joinExpectation.expectedFulfillmentCount = 2
        joinExpectation.assertForOverFulfill = true

        let leaveExpectation = expectation(description: "leaves predictions")

        let predictionsRepo = MockPredictionsRepository(onConnect: {},
                                                        onConnectV2: { _ in joinExpectation.fulfill() },
                                                        onDisconnect: { leaveExpectation.fulfill() },
                                                        connectOutcome: nil,
                                                        connectV2Outcome: nil)
        let sut = StopDetailsPage(
            schedulesRepository: MockScheduleRepository(),
            predictionsRepository: predictionsRepo,
            viewportProvider: viewportProvider,
            stop: stop,
            filter: filter,
            nearbyVM: .init()
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
        let filter: StopDetailsFilter? = .init(routeId: route.id, directionId: 0)
        let leaveExpectation = expectation(description: "leaves predictions")
        leaveExpectation.expectedFulfillmentCount = 1

        let joinExpectation = expectation(description: "joins predictions")
        joinExpectation.expectedFulfillmentCount = 2
        joinExpectation.assertForOverFulfill = true

        let predictionsRepo = MockPredictionsRepository(onConnect: {},
                                                        onConnectV2: { _ in joinExpectation.fulfill() },
                                                        onDisconnect: { leaveExpectation.fulfill() },
                                                        connectOutcome: nil,
                                                        connectV2Outcome: nil)
        let sut = StopDetailsPage(
            schedulesRepository: MockScheduleRepository(),
            predictionsRepository: predictionsRepo,
            viewportProvider: viewportProvider,
            stop: stop,
            filter: filter,
            nearbyVM: .init()
        )

        ViewHosting.host(view: sut)

        try sut.inspect().find(StopDetailsView.self).callOnChange(newValue: stop)

        wait(for: [leaveExpectation], timeout: 1)
        wait(for: [joinExpectation], timeout: 1)
    }

    func testUpdatesDeparturesOnPredictionsChange() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let prediction = objects.prediction { _ in }
        let pattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip { trip in
            trip.id = prediction.tripId
            trip.stopIds = [stop.id]
        }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let filter: StopDetailsFilter? = .init(routeId: route.id, directionId: 0)

        let nearbyVM: NearbyViewModel = .init(navigationStack: [.stopDetails(stop, nil)])

        let globalDataLoaded = PassthroughSubject<Void, Never>()

        let predictionsRepo = MockPredictionsRepository()
        var sut = StopDetailsPage(
            globalRepository: MockGlobalRepository(response: .init(objects: objects,
                                                                   patternIdsByStop: [stop.id: [pattern.id]]),
                                                   onGet: { globalDataLoaded.send() }),
            schedulesRepository: MockScheduleRepository(),
            predictionsRepository: predictionsRepo,
            viewportProvider: viewportProvider,
            stop: stop,
            filter: filter,
            nearbyVM: nearbyVM
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

    func testAppliesFilterAutomatically() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let routePattern = objects.routePattern(route: route) {
            $0.typicality = .typical
            $0.representativeTrip { _ in }
        }

        let schedulesLoadedPublisher = PassthroughSubject<Bool, Never>()

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let filter: StopDetailsFilter? = nil
        let nearbyVM: NearbyViewModel = .init(navigationStack: [.stopDetails(stop, nil)])

        let sut = StopDetailsPage(
            globalRepository: MockGlobalRepository(response: .init(
                objects: objects,
                patternIdsByStop: [stop.id: [routePattern.id]]
            )),
            schedulesRepository: MockScheduleRepository(
                scheduleResponse: .init(objects: objects),
                callback: { _ in schedulesLoadedPublisher.send(true) }
            ),
            predictionsRepository: MockPredictionsRepository(),
            viewportProvider: viewportProvider,
            stop: stop,
            filter: filter,
            nearbyVM: nearbyVM
        )

        let exp = sut.inspection.inspect(onReceive: schedulesLoadedPublisher, after: 1) { _ in
            XCTAssertEqual(nearbyVM.navigationStack.lastStopDetailsFilter,
                           StopDetailsFilter(routeId: route.id, directionId: routePattern.directionId))
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 2)
    }
}
