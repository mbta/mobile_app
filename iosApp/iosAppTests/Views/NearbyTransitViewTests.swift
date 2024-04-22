//
//  NearbyTransitViewTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-01-22.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import CoreLocation
@testable import iosApp
import shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest
@_spi(Experimental) import MapboxMaps

final class NearbyTransitViewTests: XCTestCase {
    struct NotUnderTestError: Error {}

    override func setUp() {
        executionTimeAllowance = 60
    }

    func testPending() throws {
        let sut = NearbyTransitView(
            location: ViewportProvider.defaultCenter,
            globalFetcher: .init(backend: IdleBackend()),
            nearbyFetcher: NearbyFetcher(backend: IdleBackend()),
            schedulesRepository: MockScheduleRepository(),
            predictionsFetcher: .init(socket: MockSocket()),
            alertsFetcher: .init(socket: MockSocket())
        )
        XCTAssertEqual(try sut.inspect().view(NearbyTransitView.self).vStack()[0].text().string(), "Loading...")
    }

    func testLoading() throws {
        class FakeGlobalFetcher: GlobalFetcher {
            init() {
                super.init(backend: IdleBackend())
                response = GlobalResponse(patternIdsByStop: [:], routes: [:], routePatterns: [:], stops: [:], trips: [:])
            }
        }

        class FakeNearbyFetcher: NearbyFetcher {
            let getNearbyExpectation: XCTestExpectation

            init(getNearbyExpectation: XCTestExpectation) {
                self.getNearbyExpectation = getNearbyExpectation
                super.init(backend: IdleBackend())
            }

            override func getNearby(global _: GlobalResponse, location _: CLLocationCoordinate2D) async {
                getNearbyExpectation.fulfill()
            }
        }

        let getNearbyExpectation = expectation(description: "getNearby")

        var sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            globalFetcher: FakeGlobalFetcher(),
            nearbyFetcher: FakeNearbyFetcher(getNearbyExpectation: getNearbyExpectation),
            schedulesRepository: MockScheduleRepository(),
            predictionsFetcher: .init(socket: MockSocket()), alertsFetcher: .init(socket: MockSocket())
        )

        let hasAppeared = sut.on(\NearbyTransitView.didAppear) { _ in }
        ViewHosting.host(view: sut)

        wait(for: [hasAppeared], timeout: 5)
        XCTAssertEqual(try sut.inspect().view(NearbyTransitView.self).vStack()[0].text().string(), "Loading...")
        wait(for: [getNearbyExpectation], timeout: 1)
    }

    class Route52NearbyFetcher: NearbyFetcher {
        init() {
            super.init(backend: IdleBackend())

            let objects = ObjectCollectionBuilder()
            let route52 = objects.route { route in
                route.id = "52"
                route.type = .bus
                route.shortName = "52"
            }
            let stop1 = objects.stop { stop in
                stop.id = "8552"
                stop.name = "Sawmill Brook Pkwy @ Walsh Rd"
            }
            let stop2 = objects.stop { stop in
                stop.id = "84791"
                stop.name = "Sawmill Brook Pkwy @ Walsh Rd - opposite side"
            }
            // In reality, 52-4-0 and 52-4-1 have typicality: .deviation,
            // but these tests are from before we started hiding deviations with no predictions,
            // and it's easier to just fudge the data than to rewrite the tests.
            let rp40 = objects.routePattern(route: route52) { pattern in
                pattern.id = "52-4-0"
                pattern.sortOrder = 505_200_020
                pattern.typicality = .typical
                pattern.representativeTrip { trip in
                    trip.headsign = "Charles River Loop"
                }
            }
            let rp50 = objects.routePattern(route: route52) { pattern in
                pattern.id = "52-5-0"
                pattern.sortOrder = 505_200_000
                pattern.typicality = .typical
                pattern.representativeTrip { trip in
                    trip.headsign = "Dedham Mall"
                }
            }
            let rp41 = objects.routePattern(route: route52) { pattern in
                pattern.id = "52-4-1"
                pattern.sortOrder = 505_201_010
                pattern.typicality = .typical
                pattern.representativeTrip { trip in
                    trip.headsign = "Watertown Yard"
                }
            }
            let rp51 = objects.routePattern(route: route52) { pattern in
                pattern.id = "52-5-1"
                pattern.sortOrder = 505_201_000
                pattern.typicality = .typical
                pattern.representativeTrip { trip in
                    trip.headsign = "Watertown Yard"
                }
            }
            nearbyByRouteAndStop = NearbyStaticData.companion.build { builder in
                builder.route(route: route52) { builder in
                    builder.stop(stop: stop1) { builder in
                        builder.headsign(headsign: "Charles River Loop", patterns: [rp40])
                        builder.headsign(headsign: "Dedham Mall", patterns: [rp50])
                    }
                    builder.stop(stop: stop2) { builder in
                        builder.headsign(headsign: "Watertown Yard", patterns: [rp41, rp51])
                    }
                }
            }
            loadedLocation = CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)
        }

        override func getNearby(global _: GlobalResponse, location _: CLLocationCoordinate2D) async {}
    }

    func testRoutePatternsGroupedByRouteAndStop() throws {
        var sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            globalFetcher: .init(backend: IdleBackend()),
            nearbyFetcher: Route52NearbyFetcher(),
            schedulesRepository: MockScheduleRepository(),
            predictionsFetcher: .init(socket: MockSocket()),
            alertsFetcher: .init(socket: MockSocket())
        )
        let exp = sut.on(\.didAppear) { view in
            let routes = view.findAll(NearbyRouteView.self)
            XCTAssert(!routes.isEmpty)
            guard let route = routes.first else { return }

            XCTAssertNotNil(try route.find(text: "52"))
            XCTAssertNotNil(try route.find(text: "Sawmill Brook Pkwy @ Walsh Rd")
                .find(NearbyStopView.self, relation: .parent).find(text: "Charles River Loop"))
            XCTAssertNotNil(try route.find(text: "Sawmill Brook Pkwy @ Walsh Rd")
                .find(NearbyStopView.self, relation: .parent).find(text: "Dedham Mall"))

            XCTAssertNotNil(try route.find(text: "Sawmill Brook Pkwy @ Walsh Rd - opposite side")
                .find(NearbyStopView.self, relation: .parent).find(text: "Watertown Yard"))
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 1)
    }

    @MainActor func testWithSchedules() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!

        let objects = ObjectCollectionBuilder()

        // schedule, no prediction
        let time1 = Date.now.addingTimeInterval(45 * 60).toKotlinInstant()
        let trip1 = objects.trip { $0.headsign = "Dedham Mall" }
        objects.schedule { schedule in
            schedule.departureTime = time1
            schedule.routeId = "52"
            schedule.stopId = "8552"
            schedule.tripId = trip1.id
        }

        // schedule & prediction
        let notTime2 = Date.now.addingTimeInterval(9 * 60).toKotlinInstant()
        let time2 = Date.now.addingTimeInterval(10 * 60).toKotlinInstant()
        let trip2 = objects.trip { $0.headsign = "Charles River Loop" }
        let sched2 = objects.schedule { schedule in
            schedule.departureTime = notTime2
            schedule.routeId = "52"
            schedule.stopId = "8552"
            schedule.tripId = trip2.id
            schedule.stopSequence = 13
        }
        objects.prediction(schedule: sched2) { prediction in
            prediction.departureTime = time2
        }

        // schedule & cancellation
        let notTime3 = Date.now.addingTimeInterval(15 * 60).toKotlinInstant()
        let trip3 = objects.trip { $0.headsign = "Watertown Yard" }
        let sched3 = objects.schedule { schedule in
            schedule.departureTime = notTime3
            schedule.routeId = "52"
            schedule.stopId = "84791"
            schedule.tripId = trip3.id
            schedule.stopSequence = 13
        }
        objects.prediction(schedule: sched3) { prediction in
            prediction.departureTime = nil
            prediction.scheduleRelationship = .cancelled
        }

        class FakePredictionsFetcher: PredictionsFetcher {
            init(_ objects: ObjectCollectionBuilder) {
                super.init(socket: MockSocket())
                predictions = .init(objects: objects)
            }
        }

        var sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            globalFetcher: .init(backend: IdleBackend()),
            nearbyFetcher: Route52NearbyFetcher(),
            schedulesRepository: IdleScheduleRepository(),
            scheduleResponse: .init(objects: objects),
            predictionsFetcher: FakePredictionsFetcher(objects), alertsFetcher: .init(socket: MockSocket())
        )

        let exp = sut.on(\.didAppear) { view in
            let patterns = try view.findAll(ViewType.NavigationLink.self, where: { _ in true })
                .map { try $0.labelView().view(HeadsignRowView.self) }

            XCTAssertEqual(try patterns[0].actualView().headsign, "Dedham Mall")
            XCTAssertEqual(try patterns[0].find(UpcomingTripView.self).actualView().prediction, .some(UpcomingTrip.FormatSchedule(scheduleTime: time1)))
            XCTAssertEqual(try patterns[0].find(ViewType.Image.self).actualImage().name(), "clock")

            XCTAssertEqual(try patterns[1].actualView().headsign, "Charles River Loop")
            XCTAssertEqual(try patterns[1].find(UpcomingTripView.self).actualView().prediction, .some(UpcomingTrip.FormatMinutes(minutes: 10)))

            XCTAssertEqual(try patterns[2].actualView().headsign, "Watertown Yard")
            XCTAssertEqual(try patterns[2].find(UpcomingTripView.self).actualView().prediction, .none)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 1)
    }

    @MainActor func testWithPredictions() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!

        class FakePredictionsFetcher: PredictionsFetcher {
            init(distantInstant: Instant? = nil) {
                super.init(socket: MockSocket())
                let objects = ObjectCollectionBuilder()
                let route = objects.route()
                let rp1 = objects.routePattern(route: route) { routePattern in
                    routePattern.representativeTrip { representativeTrip in
                        representativeTrip.headsign = "Dedham Mall"
                    }
                }
                let rp2 = objects.routePattern(route: route) { routePattern in
                    routePattern.representativeTrip { representativeTrip in
                        representativeTrip.headsign = "Watertown Yard"
                    }
                }
                objects.prediction { prediction in
                    prediction.arrivalTime = Date.now.addingTimeInterval(10 * 60).toKotlinInstant()
                    prediction.departureTime = Date.now.addingTimeInterval(12 * 60).toKotlinInstant()
                    prediction.routeId = "52"
                    prediction.stopId = "8552"
                    prediction.tripId = objects.trip(routePattern: rp1).id
                }
                objects.prediction { prediction in
                    prediction.arrivalTime = Date.now.addingTimeInterval(11 * 60).toKotlinInstant()
                    prediction.departureTime = Date.now.addingTimeInterval(15 * 60).toKotlinInstant()
                    prediction.status = "Overridden"
                    prediction.routeId = "52"
                    prediction.stopId = "8552"
                    prediction.tripId = objects.trip(routePattern: rp1).id
                }
                objects.prediction { prediction in
                    prediction.arrivalTime = Date.now.addingTimeInterval(1 * 60 + 1).toKotlinInstant()
                    prediction.departureTime = Date.now.addingTimeInterval(2 * 60).toKotlinInstant()
                    prediction.routeId = "52"
                    prediction.stopId = "84791"
                    prediction.tripId = objects.trip(routePattern: rp2).id
                }
                objects.prediction { prediction in
                    prediction.departureTime = distantInstant
                    prediction.routeId = "52"
                    prediction.stopId = "84791"
                    prediction.tripId = objects.trip(routePattern: rp2).id
                }
                predictions = .init(objects: objects)
            }
        }

        let distantInstant = Date.now.addingTimeInterval(TimeInterval(DISTANT_FUTURE_CUTOFF)).addingTimeInterval(5 * 60).toKotlinInstant()
        let testFormatter = DateFormatter()
        testFormatter.timeStyle = .short
        var sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            globalFetcher: .init(backend: IdleBackend()),
            nearbyFetcher: Route52NearbyFetcher(),
            schedulesRepository: MockScheduleRepository(),
            predictionsFetcher: FakePredictionsFetcher(distantInstant: distantInstant),
            alertsFetcher: .init(socket: MockSocket())
        )

        let exp = sut.on(\.didAppear) { view in
            let stops = view.findAll(NearbyStopView.self)

            XCTAssertNotNil(try stops[0].find(text: "Charles River Loop")
                .parent().find(text: "No Predictions"))

            XCTAssertNotNil(try stops[0].find(text: "Dedham Mall")
                .parent().find(text: "10 min"))
            XCTAssertNotNil(try stops[0].find(text: "Dedham Mall")
                .parent().find(text: "Overridden"))

            XCTAssertNotNil(try stops[1].find(text: "Watertown Yard")
                .parent().find(text: "1 min"))

            let expectedState = UpcomingTripView.State.some(UpcomingTrip.FormatDistantFuture(predictionTime: distantInstant))
            XCTAssert(try !stops[1].find(text: "Watertown Yard").parent()
                .findAll(UpcomingTripView.self, where: { sut in
                    try debugPrint(sut.actualView())
                    return try sut.actualView().prediction == expectedState
                }).isEmpty)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 1)
    }

    func testRefetchesPredictionsOnNewStops() throws {
        let sawmillAtWalshExpectation = expectation(description: "joins predictions for Sawmill @ Walsh")
        let lechmereExpectation = expectation(description: "joins predictions for Lechmere")

        class FakePredictionsFetcher: PredictionsFetcher {
            let sawmillAtWalshExpectation: XCTestExpectation
            let lechmereExpectation: XCTestExpectation

            init(sawmillAtWalshExpectation: XCTestExpectation, lechmereExpectation: XCTestExpectation) {
                self.sawmillAtWalshExpectation = sawmillAtWalshExpectation
                self.lechmereExpectation = lechmereExpectation
                super.init(socket: MockSocket())
            }

            override func run(stopIds: [String]) {
                if stopIds.sorted() == ["84791", "8552"] {
                    sawmillAtWalshExpectation.fulfill()
                } else if stopIds == ["place-lech"] {
                    lechmereExpectation.fulfill()
                } else {
                    XCTFail("unexpected stop IDs \(stopIds)")
                }
            }
        }

        let nearbyFetcher = Route52NearbyFetcher()
        let predictionsFetcher = FakePredictionsFetcher(sawmillAtWalshExpectation: sawmillAtWalshExpectation, lechmereExpectation: lechmereExpectation)
        let sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            globalFetcher: .init(backend: IdleBackend()),
            nearbyFetcher: nearbyFetcher,
            schedulesRepository: MockScheduleRepository(),
            predictionsFetcher: predictionsFetcher, alertsFetcher: .init(socket: MockSocket())
        )

        ViewHosting.host(view: sut)

        wait(for: [sawmillAtWalshExpectation], timeout: 1)

        nearbyFetcher.nearbyByRouteAndStop = NearbyStaticData.companion.build { builder in
            builder.route(route: nearbyFetcher.nearbyByRouteAndStop!.data[0].route) { builder in
                let lechmere = Stop(id: "place-lech", latitude: 90.12, longitude: 34.56, name: "Lechmere", locationType: .station, parentStationId: nil, childStopIds: [])
                builder.stop(stop: lechmere) { _ in
                }
            }
        }

        wait(for: [lechmereExpectation], timeout: 1)
    }

    func testRendersUpdatedPredictions() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!

        let predictionsFetcher = PredictionsFetcher(socket: MockSocket())
        var sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            globalFetcher: .init(backend: IdleBackend()),
            nearbyFetcher: Route52NearbyFetcher(),
            schedulesRepository: MockScheduleRepository(),
            predictionsFetcher: predictionsFetcher,
            alertsFetcher: .init(socket: MockSocket())
        )

        func prediction(minutesAway: Double) -> PredictionsStreamDataResponse {
            let objects = ObjectCollectionBuilder()
            let trip = objects.trip { trip in
                trip.headsign = "Dedham Mall"
            }
            objects.prediction { prediction in
                prediction.departureTime = Date.now.addingTimeInterval(minutesAway * 60).toKotlinInstant()
                prediction.routeId = "52"
                prediction.stopId = "8552"
                prediction.tripId = trip.id
            }
            return PredictionsStreamDataResponse(objects: objects)
        }

        let exp = sut.on(\.didAppear) { view in
            predictionsFetcher.predictions = prediction(minutesAway: 2)
            try view.vStack().callOnChange(newValue: predictionsFetcher.predictions)
            XCTAssertNotNil(try view.find(text: "2 min"))
            predictionsFetcher.predictions = prediction(minutesAway: 3)
            try view.vStack().callOnChange(newValue: predictionsFetcher.predictions)
            XCTAssertNotNil(try view.find(text: "3 min"))
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 1)
    }

    func testLeavesChannelWhenBackgrounded() throws {
        let joinExpectation = expectation(description: "joins predictions")
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

        let nearbyFetcher = Route52NearbyFetcher()
        let predictionsFetcher = FakePredictionsFetcher(joinExpectation: joinExpectation, leaveExpectation: leaveExpectation)
        let sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            globalFetcher: .init(backend: IdleBackend()),
            nearbyFetcher: nearbyFetcher,
            schedulesRepository: MockScheduleRepository(),
            predictionsFetcher: predictionsFetcher, alertsFetcher: .init(socket: MockSocket())
        )

        ViewHosting.host(view: sut)

        wait(for: [joinExpectation], timeout: 1)
        try sut.inspect().vStack().callOnChange(newValue: ScenePhase.background)

        wait(for: [leaveExpectation], timeout: 1)
    }

    func testLeavesChannelWhenInactive() throws {
        let joinExpectation = expectation(description: "joins predictions")
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

        let nearbyFetcher = Route52NearbyFetcher()
        let predictionsFetcher = FakePredictionsFetcher(joinExpectation: joinExpectation, leaveExpectation: leaveExpectation)
        let sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            globalFetcher: .init(backend: IdleBackend()),
            nearbyFetcher: nearbyFetcher,
            schedulesRepository: MockScheduleRepository(),
            predictionsFetcher: predictionsFetcher, alertsFetcher: .init(socket: MockSocket())
        )

        ViewHosting.host(view: sut)

        wait(for: [joinExpectation], timeout: 1)
        try sut.inspect().vStack().callOnChange(newValue: ScenePhase.background)

        wait(for: [leaveExpectation], timeout: 1)
    }

    func testRejoinsChannelWhenReactivated() throws {
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

        let nearbyFetcher = Route52NearbyFetcher()
        let predictionsFetcher = FakePredictionsFetcher(joinExpectation: joinExpectation, leaveExpectation: leaveExpectation)
        let sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            globalFetcher: .init(backend: IdleBackend()),
            nearbyFetcher: nearbyFetcher,
            schedulesRepository: MockScheduleRepository(),
            predictionsFetcher: predictionsFetcher, alertsFetcher: .init(socket: MockSocket())
        )

        ViewHosting.host(view: sut)

        try sut.inspect().vStack().callOnChange(newValue: ScenePhase.background)

        wait(for: [leaveExpectation], timeout: 1)

        try sut.inspect().vStack().callOnChange(newValue: ScenePhase.active)

        wait(for: [joinExpectation], timeout: 1)
    }

    func testScrollToTopWhenNearbyChanges() throws {
        let nearbyFetcher = Route52NearbyFetcher()
        var sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            globalFetcher: .init(backend: IdleBackend()),
            nearbyFetcher: Route52NearbyFetcher(),
            schedulesRepository: MockScheduleRepository(),
            predictionsFetcher: .init(socket: MockSocket()),
            alertsFetcher: .init(socket: MockSocket())
        )
        let exp = sut.on(\.didAppear) { view in
            XCTAssertNil(try view.actualView().scrollPosition)
            try view.vStack().callOnChange(newValue: nearbyFetcher.nearbyByRouteAndStop)
            XCTAssertNotNil(try view.actualView().scrollPosition)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 1)
    }

    func testNearbyErrorMessage() throws {
        class FakeNearbyFetcher: NearbyFetcher {
            init() {
                super.init(backend: IdleBackend())
                errorText = Text("Failed to load nearby transit, test error")
            }
        }

        let sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            globalFetcher: .init(backend: IdleBackend()),
            nearbyFetcher: FakeNearbyFetcher(),
            schedulesRepository: MockScheduleRepository(),
            predictionsFetcher: .init(socket: MockSocket()), alertsFetcher: .init(socket: MockSocket())
        )

        XCTAssertNotNil(try sut.inspect().view(NearbyTransitView.self).find(text: "Failed to load nearby transit, test error"))
    }

    func testPredictionErrorMessage() throws {
        class FakeNearbyFetcher: NearbyFetcher {
            init() {
                super.init(backend: IdleBackend())
                loadedLocation = CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)
                nearbyByRouteAndStop = NearbyStaticData(data: [])
            }
        }
        class FakePredictionsFetcher: PredictionsFetcher {
            init() {
                super.init(socket: MockSocket())
                errorText = Text("Failed to load predictions, test error")
            }
        }

        var sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            globalFetcher: .init(backend: IdleBackend()),
            nearbyFetcher: FakeNearbyFetcher(),
            schedulesRepository: MockScheduleRepository(),
            predictionsFetcher: FakePredictionsFetcher(), alertsFetcher: .init(socket: MockSocket())
        )

        let exp = sut.on(\.didAppear) { view in
            XCTAssertNotNil(try view.find(text: "Failed to load predictions, test error"))
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 1)
    }

    @MainActor func testReloadsWhenLocationChanges() throws {
        class FakeNearbyFetcher: NearbyFetcher {
            var getNearbyExpectation: XCTestExpectation

            init(getNearbyExpectation: XCTestExpectation) {
                self.getNearbyExpectation = getNearbyExpectation
                super.init(backend: IdleBackend())
            }

            override func getNearby(global _: GlobalResponse, location: CLLocationCoordinate2D) async {
                loadedLocation = location
                getNearbyExpectation.fulfill()
            }
        }

        let getNearbyExpectation = expectation(description: "getNearby")
        getNearbyExpectation.expectedFulfillmentCount = 2

        let nearbyFetcher = FakeNearbyFetcher(getNearbyExpectation: getNearbyExpectation)
        let currentLocation = CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)

        let globalFetcher = GlobalFetcher(backend: IdleBackend())
        globalFetcher.response = .init(patternIdsByStop: [:], routes: [:], routePatterns: [:], stops: [:], trips: [:])

        var sut = NearbyTransitPageView(
            currentLocation: currentLocation,
            globalFetcher: globalFetcher,
            nearbyFetcher: nearbyFetcher,
            schedulesRepository: MockScheduleRepository(),
            predictionsFetcher: .init(socket: MockSocket()),
            viewportProvider: .init(viewport: .followPuck(zoom: ViewportProvider.defaultZoom)),
            alertsFetcher: .init(socket: MockSocket())
        )

        let newLocation = CLLocationCoordinate2D(latitude: 0.0, longitude: 0.0)

        let appearancePublisher = PassthroughSubject<Bool, Never>()
        let hasAppeared = sut.inspection.inspect(after: 0.2) { view in
            XCTAssertEqual(try view.actualView().nearbyFetcher.loadedLocation, currentLocation)
            try view.actualView().locationProvider.updateCurrentLocation(newLocation)
            appearancePublisher.send(true)
        }

        let hasChangedLocation = sut.inspection.inspect(onReceive: appearancePublisher, after: 0.2) { view in
            XCTAssertEqual(try view.actualView().nearbyFetcher.loadedLocation, newLocation)
        }

        ViewHosting.host(view: sut)
        wait(for: [hasAppeared, getNearbyExpectation, hasChangedLocation], timeout: 3)
    }

    func testLocationProviderResolvesProperly() {
        let cameraLocation = CLLocationCoordinate2D(latitude: 1.0, longitude: 1.0)
        let currentLocation = CLLocationCoordinate2D(latitude: 2.0, longitude: 2.0)

        let nilCurrentProvider: NearbyTransitLocationProvider = .init(currentLocation: nil, cameraLocation: cameraLocation, isFollowing: true)
        XCTAssertEqual(nilCurrentProvider.location, cameraLocation)

        let cameraProvider: NearbyTransitLocationProvider = .init(currentLocation: currentLocation, cameraLocation: cameraLocation, isFollowing: false)
        XCTAssertEqual(cameraProvider.location, cameraLocation)

        let currentProvider: NearbyTransitLocationProvider = .init(currentLocation: currentLocation, cameraLocation: cameraLocation, isFollowing: true)
        XCTAssertEqual(currentProvider.location, currentLocation)
    }

    func testNoService() throws {
        let predictionsFetcher = PredictionsFetcher(socket: MockSocket())
        predictionsFetcher.predictions = .init(predictions: [:], trips: [:], vehicles: [:])

        let alertsFetcher = AlertsFetcher(socket: MockSocket())
        let objects = ObjectCollectionBuilder()
        objects.alert { alert in
            alert.activePeriod(start: Date.now.addingTimeInterval(-1).toKotlinInstant(), end: nil)
            alert.effect = .suspension
            alert.informedEntity(activities: [.board], directionId: nil, facility: nil, route: "52", routeType: .bus, stop: "8552", trip: nil)
        }
        alertsFetcher.alerts = AlertsStreamDataResponse(objects: objects)

        var sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            globalFetcher: .init(backend: IdleBackend()),
            nearbyFetcher: Route52NearbyFetcher(),
            schedulesRepository: MockScheduleRepository(),
            predictionsFetcher: predictionsFetcher,
            alertsFetcher: alertsFetcher
        )

        let exp = sut.on(\.didAppear) { view in
            XCTAssertNotNil(try view.find(text: "Suspension"))
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 1)
    }

    func testStopPageLink() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let pattern = objects.routePattern(route: route) { pattern in
            pattern.directionId = 1
        }
        let stop = objects.stop { $0.name = "This Stop" }
        let sut = NearbyStopView(patternsAtStop: PatternsByStop(
            route: route, stop: stop,
            patternsByHeadsign: [PatternsByHeadsign(route: route, headsign: "Place", patterns: [pattern], upcomingTrips: nil, alertsHere: nil)]
        ), now: Date.now.toKotlinInstant())

        XCTAssertEqual(
            try sut.inspect().find(navigationLink: "Place").value(SheetNavigationStackEntry.self),
            .stopDetails(stop, .init(routeId: route.id, directionId: 1))
        )
    }
}
