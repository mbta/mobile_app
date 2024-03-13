//
//  NearbyTransitViewTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-01-22.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
@testable import iosApp
import shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest

final class NearbyTransitViewTests: XCTestCase {
    struct NotUnderTestError: Error {}

    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testPending() throws {
        let sut = NearbyTransitView(location: nil, nearbyFetcher: NearbyFetcher(backend: IdleBackend()),
                                    predictionsFetcher: .init(socket: MockSocket()))
        XCTAssertEqual(try sut.inspect().view(NearbyTransitView.self).vStack()[0].text().string(), "Loading...")
    }

    @MainActor func testLoading() throws {
        class FakeNearbyFetcher: NearbyFetcher {
            let getNearbyExpectation: XCTestExpectation

            init(getNearbyExpectation: XCTestExpectation) {
                self.getNearbyExpectation = getNearbyExpectation
                super.init(backend: IdleBackend())
            }

            override func getNearby(latitude _: Double, longitude _: Double) async {
                getNearbyExpectation.fulfill()
            }
        }

        let getNearbyExpectation = expectation(description: "getNearby")

        var sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            nearbyFetcher: FakeNearbyFetcher(getNearbyExpectation: getNearbyExpectation),
            predictionsFetcher: .init(socket: MockSocket())
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
        }

        override func getNearby(latitude _: Double, longitude _: Double) async {}
    }

    @MainActor func testRoutePatternsGroupedByRouteAndStop() throws {
        let sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            nearbyFetcher: Route52NearbyFetcher(),
            predictionsFetcher: .init(socket: MockSocket())
        )

        let routes = try sut.inspect().findAll(NearbyRouteView.self)

        XCTAssertNotNil(try routes[0].find(text: "52"))
        XCTAssertNotNil(try routes[0].find(text: "Sawmill Brook Pkwy @ Walsh Rd")
            .parent().find(text: "Charles River Loop"))
        XCTAssertNotNil(try routes[0].find(text: "Sawmill Brook Pkwy @ Walsh Rd")
            .parent().find(text: "Dedham Mall"))

        XCTAssertNotNil(try routes[0].find(text: "Sawmill Brook Pkwy @ Walsh Rd - opposite side")
            .parent().find(text: "Watertown Yard"))
    }

    @MainActor func testWithPredictions() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!

        @MainActor class FakePredictionsFetcher: PredictionsFetcher {
            init(distantInstant: Instant? = nil) {
                super.init(socket: MockSocket())
                let objects = ObjectCollectionBuilder()
                let trip1a = objects.trip { trip in
                    trip.headsign = "Dedham Mall"
                }
                let trip1b = objects.trip { trip in
                    trip.headsign = "Dedham Mall"
                }
                let trip2a = objects.trip { trip in
                    trip.headsign = "Watertown Yard"
                }
                let trip2b = objects.trip { trip in
                    trip.headsign = "Watertown Yard"
                }
                objects.prediction { prediction in
                    prediction.arrivalTime = Date.now.addingTimeInterval(10 * 60).toKotlinInstant()
                    prediction.departureTime = Date.now.addingTimeInterval(12 * 60).toKotlinInstant()
                    prediction.stopId = "8552"
                    prediction.tripId = trip1a.id
                }
                objects.prediction { prediction in
                    prediction.arrivalTime = Date.now.addingTimeInterval(11 * 60).toKotlinInstant()
                    prediction.departureTime = Date.now.addingTimeInterval(15 * 60).toKotlinInstant()
                    prediction.status = "Overridden"
                    prediction.stopId = "8552"
                    prediction.tripId = trip1b.id
                }
                objects.prediction { prediction in
                    prediction.arrivalTime = Date.now.addingTimeInterval(1 * 60 + 1).toKotlinInstant()
                    prediction.departureTime = Date.now.addingTimeInterval(2 * 60).toKotlinInstant()
                    prediction.stopId = "84791"
                    prediction.tripId = trip2a.id
                }
                objects.prediction { prediction in
                    prediction.departureTime = distantInstant
                    prediction.stopId = "84791"
                    prediction.tripId = trip2b.id
                }
                predictions = .init(objects: objects)
            }
        }

        let distantInstant = Date.now.addingTimeInterval(TimeInterval(DISTANT_FUTURE_CUTOFF)).addingTimeInterval(5 * 60).toKotlinInstant()
        let testFormatter = DateFormatter()
        testFormatter.timeStyle = .short
        let sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            nearbyFetcher: Route52NearbyFetcher(),
            predictionsFetcher: FakePredictionsFetcher(distantInstant: distantInstant)
        )

        let stops = try sut.inspect().findAll(NearbyStopView.self)

        XCTAssertNotNil(try stops[0].find(text: "Charles River Loop")
            .parent().find(text: "No Predictions"))

        XCTAssertNotNil(try stops[0].find(text: "Dedham Mall")
            .parent().find(text: "10 min"))
        XCTAssertNotNil(try stops[0].find(text: "Dedham Mall")
            .parent().find(text: "Overridden"))

        XCTAssertNotNil(try stops[1].find(text: "Watertown Yard")
            .parent().find(text: "1 min"))

        let expectedState = PredictionView.State.some(UpcomingTrip.FormatDistantFuture(predictionTime: distantInstant))
        XCTAssert(try !stops[1].find(text: "Watertown Yard").parent()
            .findAll(PredictionView.self, where: { sut in
                try debugPrint(sut.actualView())
                return try sut.actualView().prediction == expectedState
            }).isEmpty)
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
            location: .init(latitude: 12.34, longitude: -56.78),
            nearbyFetcher: nearbyFetcher, predictionsFetcher: predictionsFetcher
        )

        ViewHosting.host(view: sut)

        wait(for: [sawmillAtWalshExpectation], timeout: 1)

        nearbyFetcher.nearbyByRouteAndStop = NearbyStaticData.companion.build { builder in
            builder.route(route: nearbyFetcher.nearbyByRouteAndStop!.data[0].route) { builder in
                let lechmere = Stop(id: "place-lech", latitude: 90.12, longitude: 34.56, name: "Lechmere", parentStationId: nil)
                builder.stop(stop: lechmere) { _ in
                }
            }
        }

        wait(for: [lechmereExpectation], timeout: 1)
    }

    func testRendersUpdatedPredictions() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!

        let predictionsFetcher = PredictionsFetcher(socket: MockSocket())
        let sut = NearbyTransitView(location: .init(), nearbyFetcher: Route52NearbyFetcher(), predictionsFetcher: predictionsFetcher)

        func prediction(minutesAway: Double) -> PredictionsStreamDataResponse {
            let objects = ObjectCollectionBuilder()
            let trip = objects.trip { trip in
                trip.headsign = "Dedham Mall"
            }
            objects.prediction { prediction in
                prediction.departureTime = Date.now.addingTimeInterval(minutesAway * 60).toKotlinInstant()
                prediction.stopId = "8552"
                prediction.tripId = trip.id
            }
            return PredictionsStreamDataResponse(objects: objects)
        }

        predictionsFetcher.predictions = prediction(minutesAway: 2)

        XCTAssertNotNil(try sut.inspect().find(text: "2 min"))

        predictionsFetcher.predictions = prediction(minutesAway: 3)

        XCTAssertNotNil(try sut.inspect().find(text: "3 min"))
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
            location: .init(latitude: 12.34, longitude: -56.78),
            nearbyFetcher: nearbyFetcher, predictionsFetcher: predictionsFetcher
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
            location: .init(latitude: 12.34, longitude: -56.78),
            nearbyFetcher: nearbyFetcher, predictionsFetcher: predictionsFetcher
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
            location: .init(latitude: 12.34, longitude: -56.78),
            nearbyFetcher: nearbyFetcher, predictionsFetcher: predictionsFetcher
        )

        ViewHosting.host(view: sut)

        try sut.inspect().vStack().callOnChange(newValue: ScenePhase.background)

        wait(for: [leaveExpectation], timeout: 1)

        try sut.inspect().vStack().callOnChange(newValue: ScenePhase.active)

        wait(for: [joinExpectation], timeout: 1)
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
            nearbyFetcher: FakeNearbyFetcher(),
            predictionsFetcher: .init(socket: MockSocket())
        )

        XCTAssertNotNil(try sut.inspect().view(NearbyTransitView.self).find(text: "Failed to load nearby transit, test error"))
    }

    func testPredictionErrorMessage() throws {
        class FakeNearbyFetcher: NearbyFetcher {
            init() {
                super.init(backend: IdleBackend())
                nearbyByRouteAndStop = NearbyStaticData(data: [])
            }
        }
        class FakePredictionsFetcher: PredictionsFetcher {
            init() {
                super.init(socket: MockSocket())
                errorText = Text("Failed to load predictions, test error")
            }
        }

        let sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            nearbyFetcher: FakeNearbyFetcher(),
            predictionsFetcher: FakePredictionsFetcher()
        )

        XCTAssertNotNil(try sut.inspect().view(NearbyTransitView.self).find(text: "Failed to load predictions, test error"))
    }
}
