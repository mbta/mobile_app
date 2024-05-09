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
        let filter: Binding<StopDetailsFilter?> = .constant(.init(routeId: route.id, directionId: routePattern.directionId))

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

        var sut = StopDetailsPage(
            socket: MockSocket(),
            globalFetcher: .init(backend: IdleBackend()),
            schedulesRepository: FakeSchedulesRepository(callback: callback),
            viewportProvider: viewportProvider,
            stop: stop,
            filter: filter,
            nearbyVM: .init()
        )

        ViewHosting.host(view: sut)
        try sut.inspect().vStack().callOnChange(newValue: nextStop)

        wait(for: [newStopSchedulesFetchedExpectation], timeout: 5)
    }

    func testClearsFilter() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let routePattern = objects.routePattern(route: route) { _ in }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let filter: Binding<StopDetailsFilter?> = .init(wrappedValue: .init(routeId: route.id, directionId: routePattern.directionId))

        let sut = StopDetailsPage(
            socket: MockSocket(),
            globalFetcher: .init(backend: IdleBackend()),
            schedulesRepository: MockScheduleRepository(),
            viewportProvider: viewportProvider,
            stop: stop,
            filter: filter,
            nearbyVM: .init()
        )

        try sut.inspect().find(button: "Clear Filter").tap()
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

        class fakeSchedulesRepository: ISchedulesRepository {
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
        let filter: Binding<StopDetailsFilter?> = .constant(.init(routeId: route.id, directionId: routePattern.directionId))

        var sut = StopDetailsPage(
            socket: MockSocket(),
            globalFetcher: .init(backend: IdleBackend()),
            schedulesRepository: fakeSchedulesRepository(objects: objects, callback: { schedulesLoadedPublisher.send(true) }),
            viewportProvider: viewportProvider,
            stop: stop,
            filter: filter,
            nearbyVM: .init()
        )

        let exp = sut.inspection.inspect(onReceive: schedulesLoadedPublisher, after: 0.2) { view in
            XCTAssertNotNil(try view.find(text: "Scheduled departures"))
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 2)
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

        class FakePredictionsFetcher: PredictionsFetcher {
            let joinExpectation: XCTestExpectation
            let leaveExpectation: XCTestExpectation

            init(joinExpectation: XCTestExpectation, leaveExpectation: XCTestExpectation) {
                self.joinExpectation = joinExpectation
                self.leaveExpectation = leaveExpectation
                super.init(socket: MockSocket())
            }

            override func run(stopIds _: [String]) {
                joinExpectation.fulfill()
            }

            override func leave() {
                leaveExpectation.fulfill()
            }
        }

        let predictionsFetcher = FakePredictionsFetcher(joinExpectation: joinExpectation, leaveExpectation: leaveExpectation)
        let sut = StopDetailsPage(
            socket: MockSocket(),
            globalFetcher: .init(backend: IdleBackend()),
            schedulesRepository: MockScheduleRepository(),
            viewportProvider: viewportProvider,
            stop: stop,
            filter: filter,
            nearbyVM: .init(),
            predictionsFetcher: predictionsFetcher
        )

        ViewHosting.host(view: sut)

        try sut.inspect().vStack().callOnChange(newValue: ScenePhase.background)

        wait(for: [leaveExpectation], timeout: 1)

        try sut.inspect().vStack().callOnChange(newValue: ScenePhase.active)

        wait(for: [joinExpectation], timeout: 1)
    }
}
