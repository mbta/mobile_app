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
        let filter: Binding<StopDetailsFilter?> = .constant(.init(
            routeId: route.id,
            directionId: routePattern.directionId
        ))

        class FakeSchedulesRepository: ISchedulesRepository {
            let callback: (_ stopIds: [String]) -> Void

            init(callback: @escaping (_ stopIds: [String]) -> Void) {
                self.callback = callback
            }

            func __getSchedule(stopIds: [String]) async throws -> ScheduleResponse {
                callback(stopIds)
                return ScheduleResponse(objects: ObjectCollectionBuilder())
            }

            func __getSchedule(stopIds: [String], now _: Instant) async throws -> ScheduleResponse {
                callback(stopIds)
                return ScheduleResponse(objects: ObjectCollectionBuilder())
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
        let filter: Binding<StopDetailsFilter?> = .init(wrappedValue: .init(
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

        let sut = StopDetailsPage(
            schedulesRepository: MockScheduleRepository(),
            predictionsRepository: MockPredictionsRepository(),
            viewportProvider: viewportProvider,
            stop: stop,
            filter: filter,
            nearbyVM: .init(departures: .init(routes: [route1Departures, route2Departures]))
        )

        try sut.inspect().find(button: "All").tap()
        XCTAssertNil(filter.wrappedValue)
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

            func __getSchedule(stopIds _: [String]) async throws -> ScheduleResponse {
                callback?()
                return ScheduleResponse(objects: objects)
            }

            func __getSchedule(stopIds _: [String], now _: Instant) async throws -> ScheduleResponse {
                callback?()
                return ScheduleResponse(objects: objects)
            }
        }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let filter: Binding<StopDetailsFilter?> = .constant(.init(
            routeId: route.id,
            directionId: routePattern.directionId
        ))

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

        let sut = StopDetailsPage(
            schedulesRepository: MockScheduleRepository(),
            predictionsRepository: MockPredictionsRepository(),
            viewportProvider: .init(),
            stop: stop,
            filter: .constant(nil),
            nearbyVM: FakeNearbyVM(backExp)
        )

        try sut.inspect().find(ActionButton.self).button().tap()

        wait(for: [backExp], timeout: 2)
    }

    func testRejoinsPredictionsAfterBackgrounding() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let filter: Binding<StopDetailsFilter?> = .constant(.init(routeId: route.id, directionId: 0))
        let joinExpectation = expectation(description: "joins predictions")
        joinExpectation.expectedFulfillmentCount = 2
        joinExpectation.assertForOverFulfill = true

        let leaveExpectation = expectation(description: "leaves predictions")

        let predictionsRepo = MockPredictionsRepository(onConnect: { joinExpectation.fulfill() },
                                                        onConnectV2: {},
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

    func testJoinsPredictionsV2WhenEnabled() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let filter: Binding<StopDetailsFilter?> = .constant(.init(routeId: route.id, directionId: 0))
        let joinExpectation = expectation(description: "joins predictions")
        joinExpectation.expectedFulfillmentCount = 2
        joinExpectation.assertForOverFulfill = true

        let leaveExpectation = expectation(description: "leaves predictions")

        let predictionsRepo = MockPredictionsRepository(onConnect: {},
                                                        onConnectV2: { joinExpectation.fulfill() },
                                                        onDisconnect: { leaveExpectation.fulfill() },
                                                        connectOutcome: nil,
                                                        connectV2Outcome: nil)
        let sut = StopDetailsPage(
            schedulesRepository: MockScheduleRepository(),
            settingsRepository: MockSettingsRepository(settings: [.init(key: .predictionsV2Channel, isOn: true)]),
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

    func testUpdatesDeparturesOnV2PredictionsChange() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let prediction = objects.prediction { _ in }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let filter: Binding<StopDetailsFilter?> = .constant(.init(routeId: route.id, directionId: 0))

        let nearbyVM: NearbyViewModel = .init()

        let predictionsRepo = MockPredictionsRepository()
        let sut = StopDetailsPage(
            globalRepository: MockGlobalRepository(response: .init(
                objects: objects,
                patternIdsByStop: [stop.id: []]
            )),
            schedulesRepository: MockScheduleRepository(),
            settingsRepository: MockSettingsRepository(settings: [.init(key: .predictionsV2Channel, isOn: true)]),
            predictionsRepository: predictionsRepo,
            viewportProvider: viewportProvider,
            stop: stop,
            filter: filter,
            nearbyVM: nearbyVM
        )

        ViewHosting.host(view: sut)
        XCTAssertNil(nearbyVM.departures)

        try sut.inspect().find(StopDetailsView.self)
            .callOnChange(newValue: PredictionsByStopJoinResponse(objects: objects))
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
        let filter: Binding<StopDetailsFilter?> = .init(wrappedValue: nil)

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
            nearbyVM: .init()
        )

        let exp = sut.inspection.inspect(onReceive: schedulesLoadedPublisher, after: 1) { _ in
            XCTAssertEqual(filter.wrappedValue, .init(routeId: route.id, directionId: routePattern.directionId))
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 2)
    }
}
