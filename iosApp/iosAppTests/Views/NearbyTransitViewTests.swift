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
import SwiftUI
import ViewInspector
import XCTest

final class NearbyTransitViewTests: XCTestCase {
    struct NotUnderTestError: Error {}

    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testPending() throws {
        let sut = NearbyTransitView(location: nil, nearbyFetcher: NearbyFetcher(backend: IdleBackend()), predictionsFetcher: .init(backend: IdleBackend()))
        XCTAssertEqual(try sut.inspect().view(NearbyTransitView.self).vStack()[0].text().string(), "Loading...")
    }

    @MainActor func testLoading() throws {
        class FakeNearbyFetcher: NearbyFetcher {
            let getNearbyExpectation: XCTestExpectation

            init(getNearbyExpectation: XCTestExpectation) {
                self.getNearbyExpectation = getNearbyExpectation
                super.init(backend: IdleBackend())
            }

            override func getNearby(latitude _: Double, longitude _: Double) async throws {
                getNearbyExpectation.fulfill()
                throw NotUnderTestError()
            }
        }

        let getNearbyExpectation = expectation(description: "getNearby")

        var sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            nearbyFetcher: FakeNearbyFetcher(getNearbyExpectation: getNearbyExpectation),
            predictionsFetcher: .init(backend: IdleBackend())
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
            let route52 = Route(id: "52",
                                color: "FFC72C",
                                directionNames: ["Outbound", "Inbound"],
                                directionDestinations: ["Dedham Mall", "Watertown Yard"],
                                longName: "Dedham Mall - Watertown Yard",
                                shortName: "52",
                                sortOrder: 50520,
                                textColor: "000000")
            let stop1 = Stop(id: "8552",
                             latitude: 42.289904,
                             longitude: -71.191003,
                             name: "Sawmill Brook Pkwy @ Walsh Rd",
                             parentStation: nil)
            let stop2 = Stop(id: "84791",
                             latitude: 42.289995,
                             longitude: -71.191092,
                             name: "Sawmill Brook Pkwy @ Walsh Rd - opposite side",
                             parentStation: nil)
            nearbyByRouteAndStop = [NearbyRoute(
                route: route52,
                patternsByStop: [
                    NearbyPatternsByStop(
                        stop: stop1,
                        routePatterns: [
                            RoutePattern(
                                id: "52-4-0",
                                directionId: 0,
                                name: "Watertown - Charles River Loop via Meadowbrook Rd",
                                sortOrder: 505_200_020,
                                route: route52
                            ),
                            RoutePattern(
                                id: "52-5-0",
                                directionId: 0,
                                name: "Watertown - Dedham Mall via Meadowbrook Rd",
                                sortOrder: 505_200_000,
                                route: route52
                            ),
                        ]
                    ),
                    NearbyPatternsByStop(
                        stop: stop2,
                        routePatterns: [
                            RoutePattern(
                                id: "52-4-1",
                                directionId: 1,
                                name: "Charles River Loop - Watertown via Meadowbrook Rd",
                                sortOrder: 505_201_010,
                                route: route52
                            ),
                            RoutePattern(id: "52-5-1",
                                         directionId: 1,
                                         name: "Dedham Mall - Watertown via Meadowbrook Rd",
                                         sortOrder: 505_201_000,
                                         route: route52),
                        ]
                    ),
                ]
            )]
        }

        override func getNearby(latitude _: Double, longitude _: Double) async throws {
            throw NotUnderTestError()
        }
    }

    @MainActor func testRoutePatternsGroupedByRouteAndStop() throws {
        let sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            nearbyFetcher: Route52NearbyFetcher(),
            predictionsFetcher: .init(backend: IdleBackend())
        )

        let routes = try sut.inspect().findAll(NearbyRouteView.self)

        XCTAssertNotNil(try routes[0].find(text: "52 Dedham Mall - Watertown Yard"))
        XCTAssertNotNil(try routes[0].find(text: "Sawmill Brook Pkwy @ Walsh Rd")
            .parent().find(text: "Watertown - Dedham Mall via Meadowbrook Rd"))
        XCTAssertNotNil(try routes[0].find(text: "Sawmill Brook Pkwy @ Walsh Rd")
            .parent().find(text: "Watertown - Charles River Loop via Meadowbrook Rd"))

        XCTAssertNotNil(try routes[0].find(text: "Sawmill Brook Pkwy @ Walsh Rd - opposite side")
            .parent().find(text: "Charles River Loop - Watertown via Meadowbrook Rd"))
        XCTAssertNotNil(try routes[0].find(text: "Sawmill Brook Pkwy @ Walsh Rd - opposite side")
            .parent().find(text: "Dedham Mall - Watertown via Meadowbrook Rd"))
    }

    @MainActor func testWithPredictions() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!

        @MainActor class FakePredictionsFetcher: PredictionsFetcher {
            init() {
                super.init(backend: IdleBackend())
                predictions = [
                    Prediction(
                        id: "prediction-60451421-8552-38",
                        arrivalTime: Instant.companion.parse(isoString: "2024-02-05T15:20:24-05:00"),
                        departureTime: Instant.companion.parse(isoString: "2024-02-05T15:20:24-05:00"),
                        directionId: 0,
                        revenue: true,
                        scheduleRelationship: .scheduled,
                        status: nil,
                        stopSequence: 38,
                        trip: Trip(id: "60451421", routePatternId: "52-5-0", stops: nil)
                    ),
                    Prediction(
                        id: "prediction-60451426-84791-18",
                        arrivalTime: Instant.companion.parse(isoString: "2024-02-05T16:04:59-05:00"),
                        departureTime: Instant.companion.parse(isoString: "2024-02-05T16:04:59-05:00"),
                        directionId: 1,
                        revenue: true,
                        scheduleRelationship: .scheduled,
                        status: nil,
                        stopSequence: 18,
                        trip: Trip(id: "60451426", routePatternId: "52-5-1", stops: nil)
                    ),
                ]
            }
        }

        let sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            nearbyFetcher: Route52NearbyFetcher(),
            predictionsFetcher: FakePredictionsFetcher()
        )

        let stops = try sut.inspect().findAll(NearbyStopView.self)

        XCTAssertNotNil(try stops[0].find(text: "Watertown - Dedham Mall via Meadowbrook Rd")
            .parent().find(text: "3:20\u{202F}PM"))
        XCTAssertNotNil(try stops[0].find(text: "Watertown - Charles River Loop via Meadowbrook Rd")
            .parent().find(text: "No Predictions"))

        XCTAssertNotNil(try stops[1].find(text: "Charles River Loop - Watertown via Meadowbrook Rd")
            .parent().find(text: "No Predictions"))
        XCTAssertNotNil(try stops[1].find(text: "Dedham Mall - Watertown via Meadowbrook Rd")
            .parent().find(text: "4:04\u{202F}PM"))
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
                super.init(backend: IdleBackend())
            }

            override func run(stopIds: [String]) async throws {
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

        nearbyFetcher.nearbyByRouteAndStop = [
            NearbyRoute(
                route: nearbyFetcher.nearbyByRouteAndStop![0].route,
                patternsByStop: [
                    NearbyPatternsByStop(
                        stop: Stop(id: "place-lech", latitude: 90.12, longitude: 34.56, name: "Lechmere", parentStation: nil),
                        routePatterns: []
                    ),
                ]
            ),
        ]

        wait(for: [lechmereExpectation], timeout: 1)
    }

    func testRendersUpdatedPredictions() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!

        let predictionsFetcher = PredictionsFetcher(backend: IdleBackend())
        let sut = NearbyTransitView(location: .init(), nearbyFetcher: Route52NearbyFetcher(), predictionsFetcher: predictionsFetcher)

        func prediction(departureTime: String) -> Prediction {
            Prediction(
                id: "",
                arrivalTime: nil,
                departureTime: Instant.companion.parse(isoString: departureTime),
                directionId: 0,
                revenue: true,
                scheduleRelationship: .scheduled,
                status: nil,
                stopSequence: 1,
                trip: Trip(id: "", routePatternId: "52-5-0", stops: nil)
            )
        }

        predictionsFetcher.predictions = [prediction(departureTime: "2024-02-06T10:58:06-05:00")]

        XCTAssertNotNil(try sut.inspect().find(text: "10:58\u{202F}AM"))

        predictionsFetcher.predictions = [prediction(departureTime: "2024-02-06T11:03:00-05:00")]

        XCTAssertNotNil(try sut.inspect().find(text: "11:03\u{202F}AM"))
    }
}
