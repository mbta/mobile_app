//
//  SearchResultViewTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 1/30/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class SearchResultViewTests: XCTestCase {
    struct NotUnderTestError: Error {}

    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testPending() throws {
        let sut = SearchResultView(results: nil)

        XCTAssertEqual(try sut.inspect().view(SearchResultView.self).text().string(), "Loading...")
    }

    @MainActor func testResultLoad() throws {
        struct FakeBackend: BackendProtocol {
            let getSearchResultsExpectation: XCTestExpectation
            func getNearby(latitude _: Double, longitude _: Double) async throws -> NearbyResponse {
                throw NotUnderTestError()
            }

            func getSearchResults(query _: String) async throws -> SearchResponse {
                getSearchResultsExpectation.fulfill()
                throw NotUnderTestError()
            }
        }

        let getSearchResultsExpectation = expectation(description: "getSearchResults")

        var sut = SearchView(
            query: "hay",
            backend: BackendDispatcher(backend: FakeBackend(getSearchResultsExpectation: getSearchResultsExpectation))
        )

        let hasAppeared = sut.on(\.didAppear) { _ in }
        ViewHosting.host(view: sut)

        wait(for: [hasAppeared], timeout: 1)
        wait(for: [getSearchResultsExpectation], timeout: 1)
    }

    @MainActor func testNoResults() throws {
        let sut = SearchResultView(
            results: SearchResponse(data: SearchResults(routes: [], stops: []))
        )
        XCTAssertEqual(try sut.inspect().view(SearchResultView.self).text().string(), "No results found")
    }

    @MainActor func testFullResults() throws {
        let sut = SearchResultView(
            results: SearchResponse(
                data: SearchResults(
                    routes: [
                        RouteResult(
                            id: "428",
                            rank: 5,
                            longName: "Oaklandvale - Haymarket Station",
                            shortName: "428",
                            routeType: RouteType.bus
                        ),
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
                                ),
                            ]
                        ),
                    ]
                )
            )
        )

        XCTAssertNoThrow(try sut.inspect().find(text: "Stops"))
        XCTAssertNoThrow(try sut.inspect().find(text: "Haymarket"))
        XCTAssertNoThrow(try sut.inspect().find(text: "Routes"))
        XCTAssertNoThrow(try sut.inspect().find(text: "428 Oaklandvale - Haymarket Station"))
    }

    @MainActor func testOnlyRoutes() throws {
        let sut = SearchResultView(
            results: SearchResponse(
                data: SearchResults(
                    routes: [
                        RouteResult(
                            id: "428",
                            rank: 5,
                            longName: "Oaklandvale - Haymarket Station",
                            shortName: "428",
                            routeType: RouteType.bus
                        ),
                    ],
                    stops: []
                )
            )
        )

        XCTAssertNoThrow(try sut.inspect().find(text: "Routes"))
        XCTAssertNoThrow(try sut.inspect().find(text: "428 Oaklandvale - Haymarket Station"))
        XCTAssertThrowsError(try sut.inspect().find(text: "Stops"))
    }

    @MainActor func testOnlyStops() throws {
        let sut = SearchResultView(
            results: SearchResponse(
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
                                ),
                            ]
                        ),
                    ]
                )
            )
        )

        XCTAssertNoThrow(try sut.inspect().find(text: "Stops"))
        XCTAssertNoThrow(try sut.inspect().find(text: "Haymarket"))
        XCTAssertThrowsError(try sut.inspect().find(text: "Routes"))
    }
}
