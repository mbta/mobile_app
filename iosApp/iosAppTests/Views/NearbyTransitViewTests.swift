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
    struct NotUnderTestError : Error {}

    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testPending() throws {
        let sut = NearbyTransitView(viewModel: .init(location: nil, backend: .init(backend: IdleBackend()), nearby: nil))
            .environmentObject(LocationDataManager())

        XCTAssertEqual(try sut.inspect().view(NearbyTransitView.self).text().string(), "Loading...")
    }

    @MainActor func testLoading() throws {
        struct FakeBackend: BackendProtocol {
            let getNearbyExpectation: XCTestExpectation
            func getNearby(latitude _: Double, longitude _: Double) async throws -> NearbyResponse {
                getNearbyExpectation.fulfill()
                throw NotUnderTestError()
            }
            func getSearchResults(query: String) async throws -> SearchResponse {
                throw NotUnderTestError()
            }
        }

        let getNearbyExpectation = expectation(description: "getNearby")

        let sut = NearbyTransitView(viewModel: .init(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            backend: BackendDispatcher(backend: FakeBackend(getNearbyExpectation: getNearbyExpectation))
        ))
        .environmentObject(LocationDataManager())

        XCTAssertEqual(try sut.inspect().view(NearbyTransitView.self).text().string(), "Loading...")
        wait(for: [getNearbyExpectation], timeout: 1)
    }

    @MainActor func testLoaded() throws {
        struct FakeBackend: BackendProtocol {
            func getNearby(latitude _: Double, longitude _: Double) async throws -> NearbyResponse {
                struct AlreadyLoadedError: Error {}
                throw AlreadyLoadedError()
            }
            func getSearchResults(query: String) async throws -> SearchResponse {
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
        let nearby = NearbyResponse(
            stops: [
                Stop(id: "8552",
                     latitude: 42.289904,
                     longitude: -71.191003,
                     name: "Sawmill Brook Pkwy @ Walsh Rd",
                     parentStation: nil),
                Stop(id: "84791",
                     latitude: 42.289995,
                     longitude: -71.191092,
                     name: "Sawmill Brook Pkwy @ Walsh Rd",
                     parentStation: nil),
            ],
            routePatterns: [
                "52-4-0": RoutePattern(id: "52-4-0",
                                       directionId: 0,
                                       name: "Watertown - Charles River Loop via Meadowbrook Rd",
                                       sortOrder: 505_200_020,
                                       route: route52),
                "52-4-1": RoutePattern(id: "52-4-1",
                                       directionId: 1,
                                       name: "Charles River Loop - Watertown via Meadowbrook Rd",
                                       sortOrder: 505_201_010,
                                       route: route52),
                "52-5-0": RoutePattern(id: "52-5-0",
                                       directionId: 0,
                                       name: "Watertown - Dedham Mall via Meadowbrook Rd",
                                       sortOrder: 505_200_000,
                                       route: route52),
                "52-5-1": RoutePattern(id: "52-5-1",
                                       directionId: 1,
                                       name: "Dedham Mall - Watertown via Meadowbrook Rd",
                                       sortOrder: 505_201_000,
                                       route: route52),
            ],
            patternIdsByStop: [
                "8552": ["52-5-0", "52-4-0"],
                "84791": ["52-5-1", "52-4-1"],
            ]
        )

        let sut = NearbyTransitView(viewModel: .init(
            location: CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78),
            backend: BackendDispatcher(backend: FakeBackend()),
            nearby: nearby
        ))
        .environmentObject(LocationDataManager())

        let routePatterns = try sut.inspect().findAll(NearbyRoutePatternView.self)

        XCTAssertEqual(try routePatterns[0].vStack().text(0).string(), "Route 52 Dedham Mall - Watertown Yard")
        XCTAssertEqual(try routePatterns[0].vStack().text(1).string(), "Pattern 52-5-0 Watertown - Dedham Mall via Meadowbrook Rd")
        XCTAssertEqual(try routePatterns[0].vStack().text(2).string(), "Stop Sawmill Brook Pkwy @ Walsh Rd")

        XCTAssertEqual(try routePatterns[1].vStack().text(0).string(), "Route 52 Dedham Mall - Watertown Yard")
        XCTAssertEqual(try routePatterns[1].vStack().text(1).string(), "Pattern 52-4-0 Watertown - Charles River Loop via Meadowbrook Rd")
        XCTAssertEqual(try routePatterns[1].vStack().text(2).string(), "Stop Sawmill Brook Pkwy @ Walsh Rd")

        XCTAssertEqual(try routePatterns[2].vStack().text(0).string(), "Route 52 Dedham Mall - Watertown Yard")
        XCTAssertEqual(try routePatterns[2].vStack().text(1).string(), "Pattern 52-5-1 Dedham Mall - Watertown via Meadowbrook Rd")
        XCTAssertEqual(try routePatterns[2].vStack().text(2).string(), "Stop Sawmill Brook Pkwy @ Walsh Rd")

        XCTAssertEqual(try routePatterns[3].vStack().text(0).string(), "Route 52 Dedham Mall - Watertown Yard")
        XCTAssertEqual(try routePatterns[3].vStack().text(1).string(), "Pattern 52-4-1 Charles River Loop - Watertown via Meadowbrook Rd")
        XCTAssertEqual(try routePatterns[3].vStack().text(2).string(), "Stop Sawmill Brook Pkwy @ Walsh Rd")
    }
}
