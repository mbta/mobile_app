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
        let sut = SearchResultView(results: nil, handleStopTap: { _ in })

        XCTAssertEqual(try sut.inspect().view(SearchResultView.self).text().string(), "Loading...")
    }

    @MainActor func testResultLoad() throws {
        class FakeRepo: ISearchResultRepository {
            let getSearchResultsExpectation: XCTestExpectation

            init(getSearchResultsExpectation: XCTestExpectation) {
                self.getSearchResultsExpectation = getSearchResultsExpectation
            }

            func __getSearchResults(query _: String) async throws -> SearchResults? {
                getSearchResultsExpectation.fulfill()
                throw NotUnderTestError()
            }
        }

        let getSearchResultsExpectation = expectation(description: "getSearchResults")

        var sut = SearchView(
            query: "hay",
            nearbyVM: NearbyViewModel(),
            searchVM: SearchViewModel(),
            searchResultsRepository: FakeRepo(getSearchResultsExpectation: getSearchResultsExpectation)
        )

        let hasAppeared = sut.on(\.didAppear) { _ in }
        ViewHosting.host(view: sut)

        wait(for: [hasAppeared], timeout: 1)
        wait(for: [getSearchResultsExpectation], timeout: 1)
    }

    @MainActor func testOverlayDisplayedOnFocus() throws {
        class FakeRepo: ISearchResultRepository {
            let getSearchResultsExpectation: XCTestExpectation

            init(getSearchResultsExpectation: XCTestExpectation) {
                self.getSearchResultsExpectation = getSearchResultsExpectation
            }

            func __getSearchResults(query _: String) async throws -> SearchResults? {
                getSearchResultsExpectation.fulfill()
                return SearchResults(routes: [], stops: [])
            }
        }

        let getSearchResultsExpectation = expectation(description: "getSearchResults")
        getSearchResultsExpectation.assertForOverFulfill = false
        let searchObserver = TextFieldObserver()

        var sut = SearchOverlay(
            searchObserver: searchObserver,
            nearbyVM: NearbyViewModel(),
            searchVM: SearchViewModel(),
            searchResultsRepository: FakeRepo(getSearchResultsExpectation: getSearchResultsExpectation)
        )

        ViewHosting.host(view: sut)

        // On init, only the search field should be displayed
        XCTAssertNotNil(try sut.inspect().find(SearchField.self))
        XCTAssertThrowsError(try sut.inspect().find(SearchResultView.self))
        XCTAssertThrowsError(try sut.inspect().find(SearchField.self).find(button: "Cancel"))
        // On focus, the result view and cancel button should appear,
        // but the clear button should be hidden
        searchObserver.isFocused = true
        XCTAssertNotNil(try sut.inspect().find(SearchResultView.self))
        XCTAssertNotNil(try sut.inspect().find(SearchField.self).find(button: "Cancel"))
        XCTAssertThrowsError(try sut.inspect().find(SearchField.self).find(ActionButton.self))
        XCTAssert(searchObserver.isSearching)
        // Once text is entered, the search repo should be called
        searchObserver.searchText = "test"
        wait(for: [getSearchResultsExpectation], timeout: 1)
        XCTAssertEqual("test", searchObserver.searchText)
        // Even if the focus is then lost, the result view should still be displayed
        searchObserver.isFocused = false
        XCTAssertNotNil(try sut.inspect().find(SearchResultView.self))
        // When the search field is cleared, the field should be refocused
        try sut.inspect().find(SearchField.self).find(ActionButton.self).button().tap()
        XCTAssertThrowsError(try sut.inspect().find(SearchField.self).find(ActionButton.self))
        XCTAssert(searchObserver.isSearching)
        XCTAssertNotNil(try sut.inspect().find(SearchResultView.self))
        // When cancel is tapped, the results should disappear and the search field be unfocused
        try sut.inspect().find(SearchField.self).find(button: "Cancel").tap()
        XCTAssertThrowsError(try sut.inspect().find(SearchResultView.self))
        XCTAssertFalse(searchObserver.isSearching)
    }

    @MainActor func testNoResults() throws {
        let sut = SearchResultView(
            results: SearchResults(routes: [], stops: []),
            handleStopTap: { _ in }
        )
        XCTAssertEqual(try sut.inspect().view(SearchResultView.self).text().string(), "No results found")
    }

    @MainActor func testFullResults() throws {
        let sut = SearchResultView(
            results: SearchResults(
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
                                type: .heavyRail,
                                icon: "orange_line"
                            ),
                        ]
                    ),
                ]
            ),
            handleStopTap: { _ in },
            showRoutes: true
        )

        XCTAssertNoThrow(try sut.inspect().find(text: "Stops"))
        XCTAssertNoThrow(try sut.inspect().find(text: "Haymarket"))
        XCTAssertNoThrow(try sut.inspect().find(text: "Routes"))
        XCTAssertNoThrow(try sut.inspect().find(text: "428 Oaklandvale - Haymarket Station"))
    }

    @MainActor func testOnlyRoutes() throws {
        let sut = SearchResultView(
            results: SearchResults(
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
            ),
            handleStopTap: { _ in },
            showRoutes: true
        )

        XCTAssertNoThrow(try sut.inspect().find(text: "Routes"))
        XCTAssertNoThrow(try sut.inspect().find(text: "428 Oaklandvale - Haymarket Station"))
        XCTAssertThrowsError(try sut.inspect().find(text: "Stops"))
    }

    @MainActor func testRoutesHidden() throws {
        let sut = SearchResultView(
            results: SearchResults(
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
            ),
            handleStopTap: { _ in },
            showRoutes: false
        )

        XCTAssertEqual(try sut.inspect().view(SearchResultView.self).text().string(), "No results found")
    }

    @MainActor func testOnlyStops() throws {
        let sut = SearchResultView(
            results: SearchResults(
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
                                type: .heavyRail,
                                icon: "orange_line"
                            ),
                        ]
                    ),
                ]
            ),
            handleStopTap: { _ in }
        )

        XCTAssertNoThrow(try sut.inspect().find(text: "Stops"))
        XCTAssertNoThrow(try sut.inspect().find(text: "Haymarket"))
        XCTAssertThrowsError(try sut.inspect().find(text: "Routes"))
    }

    @MainActor func testStopTapping() async throws {
        let tapStopExpectation = expectation(description: "stop was tapped")

        let sut = SearchResultView(
            results: SearchResults(
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
                                type: .heavyRail,
                                icon: "orange_line"
                            ),
                        ]
                    ),
                ]
            ),
            handleStopTap: { stopId in
                XCTAssertEqual("place-haecl", stopId)
                tapStopExpectation.fulfill()
            }
        )

        XCTAssertNoThrow(
            try sut.inspect()
                .find(text: "Haymarket")
                .find(StopResultView.self, relation: .parent)
                .callOnTapGesture()
        )
        await fulfillment(of: [tapStopExpectation], timeout: 1)
    }
}
