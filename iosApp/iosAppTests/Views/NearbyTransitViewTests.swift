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
import ViewInspector
import XCTest

final class NearbyTransitViewTests: XCTestCase {
    struct NotUnderTestError: Error {}

    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testPending() throws {
        let sut = NearbyTransitView(location: nil, backend: .init(backend: IdleBackend()), nearby: nil)

        XCTAssertEqual(try sut.inspect().view(NearbyTransitView.self).vStack()[0].text().string(), "Loading...")
    }

    @MainActor func testLoading() throws {
        struct FakeBackend: BackendProtocol {
            let getNearbyExpectation: XCTestExpectation
            func getNearby(latitude _: Double, longitude _: Double) async throws -> NearbyResponse {
                getNearbyExpectation.fulfill()
                throw NotUnderTestError()
            }

            func getSearchResults(query _: String) async throws -> SearchResponse {
                throw NotUnderTestError()
            }
        }

        let getNearbyExpectation = expectation(description: "getNearby")

        var sut = NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            backend: BackendDispatcher(backend: FakeBackend(getNearbyExpectation: getNearbyExpectation))
        )

        let hasAppeared = sut.on(\.didAppear) { _ in }
        ViewHosting.host(view: sut)

        wait(for: [hasAppeared], timeout: 1)
        XCTAssertEqual(try sut.inspect().view(NearbyTransitView.self).vStack()[0].text().string(), "Loading...")
        wait(for: [getNearbyExpectation], timeout: 1)
    }

    @MainActor func testRoutePatternsGroupedByRouteAndStop() throws {
        struct FakeBackend: BackendProtocol {
            func getNearby(latitude _: Double, longitude _: Double) async throws -> NearbyResponse {
                struct AlreadyLoadedError: Error {}
                throw AlreadyLoadedError()
            }

            func getSearchResults(query _: String) async throws -> SearchResponse {
                throw NotUnderTestError()
            }
        }

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
        let nearby = [NearbyRoute(
            route: route52, patternsByStop: [
                NearbyPatternsByStop(stop: stop1,
                                     routePatterns: [
                                         RoutePattern(id: "52-4-0",
                                                      directionId: 0,
                                                      name: "Watertown - Charles River Loop via Meadowbrook Rd",
                                                      sortOrder: 505_200_020,
                                                      route: route52),
                                         RoutePattern(id: "52-5-0",
                                                      directionId: 0,
                                                      name: "Watertown - Dedham Mall via Meadowbrook Rd",
                                                      sortOrder: 505_200_000,
                                                      route: route52),
                                     ]),
                NearbyPatternsByStop(stop: stop2,
                                     routePatterns: [
                                         RoutePattern(id: "52-4-1",
                                                      directionId: 1,
                                                      name: "Charles River Loop - Watertown via Meadowbrook Rd",
                                                      sortOrder: 505_201_010,
                                                      route: route52),
                                         RoutePattern(id: "52-5-1",
                                                      directionId: 1,
                                                      name: "Dedham Mall - Watertown via Meadowbrook Rd",
                                                      sortOrder: 505_201_000,
                                                      route: route52),
                                     ]),
            ]
        )]

        let sut = NearbyTransitView(location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
                                    backend: BackendDispatcher(backend: FakeBackend()),
                                    nearby: nearby)
            .environmentObject(LocationDataManager())

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
}
