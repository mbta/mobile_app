//
//  SearchResultViewTest.swift
//  iosAppTests
//
//  Created by Simon, Emma on 1/30/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import XCTest
import ViewInspector
import SwiftUI
import shared
@testable import iosApp

final class SearchResultViewTests: XCTestCase {
    struct NotUnderTestError : Error {}

    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testPending() throws {
        let sut = SearchResultView(viewModel: .init(query: nil, backend: .init(backend: IdleBackend()), response: nil))

        XCTAssertEqual(try sut.inspect().view(SearchResultView.self).text().string(), "Loading...")
    }
    
    @MainActor func testNoResults() throws {
        struct FakeBackend : BackendProtocol {
            let getSearchResultsExpectation: XCTestExpectation
            func getNearby(latitude: Double, longitude: Double) async throws -> NearbyResponse {
                throw NotUnderTestError()
            }
            func getSearchResults(query: String) async throws -> SearchResponse {
                getSearchResultsExpectation.fulfill()
                throw NotUnderTestError()
            }
        }
        
        let getSearchResultsExpectation = expectation(description: "getSearchResults")

        let sut = SearchResultView(viewModel: .init(
            query: "nothing",
            backend: BackendDispatcher(backend: FakeBackend(getSearchResultsExpectation: getSearchResultsExpectation)),
            response: SearchResponse(data: SearchResults(routes: [], stops: []))
        ))
        
        XCTAssertEqual(try sut.inspect().view(SearchResultView.self).text().string(), "No results found")
        wait(for: [getSearchResultsExpectation], timeout: 1)
    }
    
    @MainActor func testWithResults() throws {
        struct FakeBackend : BackendProtocol {
            let getSearchResultsExpectation: XCTestExpectation
            func getNearby(latitude: Double, longitude: Double) async throws -> NearbyResponse {
                throw NotUnderTestError()
            }
            func getSearchResults(query: String) async throws -> SearchResponse {
                getSearchResultsExpectation.fulfill()
                throw NotUnderTestError()
            }
        }

        let getSearchResultsExpectation = expectation(description: "getSearchResults")

        let sut = SearchResultView(viewModel: .init(
            query: "hay",
            backend: BackendDispatcher(backend: FakeBackend(getSearchResultsExpectation: getSearchResultsExpectation)),
            response: SearchResponse(
                data: SearchResults(
                    routes: [
                        RouteResult(
                            id: "428",
                            rank: 5,
                            longName: "Oaklandvale - Haymarket Station",
                            shortName: "428",
                            routeType: RouteType.bus
                        )
                    ],
                    stops: [
                        StopResult(
                            id: "place-haecl",
                            rank: 2,
                            name: "Haymarket",
                            zone: nil,
                            isStation: true,
                            routes: [
                                StopResultRoute(
                                    type: RouteType.subway,
                                    icon: "orange_line"
                                )
                            ]
                        )
                    ]
                )
            )
        ))

        XCTAssertEqual(try sut.inspect().view(SearchResultView.self).list()[0].section().header().text().string(), "Stops")
        XCTAssertEqual(try sut.inspect().findAll(StopResultView.self)[0].vStack()[0].text().string(), "Haymarket")
        XCTAssertEqual(try sut.inspect().view(SearchResultView.self).list()[1].section().header().text().string(), "Routes")
        XCTAssertEqual(try sut.inspect().findAll(RouteResultView.self)[0].vStack()[0].text().string(), "428 Oaklandvale - Haymarket Station")
        wait(for: [getSearchResultsExpectation], timeout: 1)
    }

    @MainActor func testOnlyRoutes() throws {
        struct FakeBackend : BackendProtocol {
            func getNearby(latitude: Double, longitude: Double) async throws -> NearbyResponse {
                throw NotUnderTestError()
            }
            func getSearchResults(query: String) async throws -> SearchResponse {
                throw NotUnderTestError()
            }
        }

        let sut = SearchResultView(viewModel: .init(
            query: "hay",
            backend: BackendDispatcher(backend: FakeBackend()),
            response: SearchResponse(
                data: SearchResults(
                    routes: [
                        RouteResult(
                            id: "428",
                            rank: 5,
                            longName: "Oaklandvale - Haymarket Station",
                            shortName: "428",
                            routeType: RouteType.bus
                        )
                    ],
                    stops: []
                )
            )
        ))
        
        XCTAssertEqual(try sut.inspect().view(SearchResultView.self).list().section(1).header().text().string(), "Routes")
        XCTAssertEqual(try sut.inspect().findAll(RouteResultView.self)[0].vStack()[0].text().string(), "428 Oaklandvale - Haymarket Station")
        XCTAssertThrowsError(try sut.inspect().view(SearchResultView.self).list().section(0))
    }

    @MainActor func testOnlyStops() throws {
        struct FakeBackend : BackendProtocol {
            func getNearby(latitude: Double, longitude: Double) async throws -> NearbyResponse {
                throw NotUnderTestError()
            }
            func getSearchResults(query: String) async throws -> SearchResponse {
                throw NotUnderTestError()
            }
        }

        let sut = SearchResultView(viewModel: .init(
            query: "hay",
            backend: BackendDispatcher(backend: FakeBackend()),
            response: SearchResponse(
                data: SearchResults(
                    routes: [],
                    stops: [
                        StopResult(
                            id: "place-haecl",
                            rank: 2,
                            name: "Haymarket",
                            zone: nil,
                            isStation: true,
                            routes: [
                                StopResultRoute(
                                    type: RouteType.subway,
                                    icon: "orange_line"
                                )
                            ]
                        )
                    ]
                )
            )
        ))

        XCTAssertEqual(try sut.inspect().view(SearchResultView.self).list().section(0).header().text().string(), "Stops")
        XCTAssertEqual(try sut.inspect().findAll(StopResultView.self)[0].vStack()[0].text().string(), "Haymarket")
        XCTAssertThrowsError(try sut.inspect().view(SearchResultView.self).list().section(1))
    }
}
